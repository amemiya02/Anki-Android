#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""
Adds a Material Symbols icon to the app as a vector drawable.

Every interface icon is a Material Symbol in the style named by the `icons` entry of
docs/redesign/quiet-paper.values.json (Outlined, weight 300, FILL 0, GRAD 0, optical size 24).
This script downloads the symbol's SVG from Google's official repository
(google/material-design-icons, Apache-2.0) into a new temporary directory, writes
`<module>/src/main/res/drawable/symbol_<name>.xml` and records the symbol in
tools/material-symbols/symbols.lock, which `InterfaceIconsAreMaterialSymbolsTest` checks the
drawable against. Google's Android export of the same symbol says whether it carries direction;
if so the drawable mirrors in a right-to-left language.

    python3 -I tools/material-symbols/symbol.py common/android undo
    python3 -I tools/material-symbols/symbol.py AnkiDroid close --variant again --tint '?attr/foundationAnswerAgain'

The drawable is tinted `?attr/colorControlNormal` unless `--tint` names another theme attribute
(or a Flag's colour). A view or menu can still tint it with `app:tint` / `app:iconTint`.
Run it from the repository root. It needs only the Python standard library.
"""

import argparse
import hashlib
import json
import re
import tempfile
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LOCK = ROOT / "tools" / "material-symbols" / "symbols.lock"
VALUES = ROOT / "docs" / "redesign" / "quiet-paper.values.json"
REPOSITORY = "google/material-design-icons"
# The commit of google/material-design-icons the symbols come from; update the lock's header with it
COMMIT = "10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7"
RAW = f"https://raw.githubusercontent.com/{REPOSITORY}/{COMMIT}/symbols"
SVG_NS = "{http://www.w3.org/2000/svg}"
ANDROID = "http://schemas.android.com/apk/res/android"
STYLE_DIRS = {"Material Symbols Outlined": "materialsymbolsoutlined"}


def style():
    return json.loads(VALUES.read_text())["icons"]


def file_stem(name, icons):
    """`undo_wght300_24px`: Google's file name for a symbol in the Foundation's style"""
    axes = ""
    if icons["weight"] != 400:
        axes += f"wght{icons['weight']}"
    if icons["grade"] != 0:
        axes += f"grad{'N' if icons['grade'] < 0 else ''}{abs(icons['grade'])}"
    if icons["fill"] != 0:
        axes += "fill1"
    return name + (f"_{axes}" if axes else "") + f"_{icons['opticalSize']}px"


def download(url, into):
    target = into / url.rsplit("/", 1)[1]
    with urllib.request.urlopen(url) as response:
        target.write_bytes(response.read())
    return target


def path_data(svg_file):
    root = ET.parse(svg_file).getroot()
    if root.get("viewBox") != "0 -960 960 960":
        raise SystemExit(f"{svg_file.name}: unexpected viewBox {root.get('viewBox')}")
    paths = [p.get("d") for p in root.iter(f"{SVG_NS}path")]
    if not paths or any(re.search(r"[aA]", d) for d in paths):
        raise SystemExit(f"{svg_file.name}: no path, or an arc the converter does not handle")
    if len(list(root.iter())) != len(paths) + 1:
        raise SystemExit(f"{svg_file.name}: holds more than paths")
    return paths


def mirrors(android_file):
    """Whether Google's own Android export of the symbol mirrors in right-to-left languages"""
    return ET.parse(android_file).getroot().get(f"{{{ANDROID}}}autoMirrored") == "true"


def drawable(name, paths, mirrored, tint, icons):
    size = icons["size"]
    mirror = '\n    android:autoMirrored="true"' if mirrored else ""
    body = "\n".join(
        f'        <path\n            android:fillColor="@android:color/white"\n            android:pathData="{d}" />' for d in paths
    )
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!--
  {icons['family']}: {name}
  From {REPOSITORY} (Apache-2.0), drawn by tools/material-symbols/symbol.py. Do not edit by hand.
-->
<vector xmlns:android="{ANDROID}"
    android:width="{size}dp"
    android:height="{size}dp"
    android:viewportWidth="960"
    android:viewportHeight="960"{mirror}
    android:tint="{tint}">
    <group android:translateY="960">
{body}
    </group>
</vector>
"""


def read_lock():
    entries = {}
    if LOCK.exists():
        for line in LOCK.read_text().splitlines():
            if line and not line.startswith("#"):
                symbol, direction, digest = line.split()
                entries[symbol] = (direction, digest)
    return entries


def write_lock(entries, icons):
    header = [
        "# The Material Symbols drawn in the app, written by tools/material-symbols/symbol.py.",
        "# <symbol> <mirrored|fixed> <sha256 of its path data, one line per path>",
        f'# style family="{icons["family"]}" weight={icons["weight"]} fill={icons["fill"]} '
        f'grade={icons["grade"]} opticalSize={icons["opticalSize"]} source={REPOSITORY}@{COMMIT}',
    ]
    lines = [f"{s} {d} {h}" for s, (d, h) in sorted(entries.items())]
    LOCK.write_text("\n".join(header + lines) + "\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("module", help="the module that uses the icon, e.g. AnkiDroid or common/android")
    parser.add_argument("symbols", nargs="+", help="Material Symbols names, e.g. undo arrow_back")
    parser.add_argument("--variant", help="suffix for a second drawable of a symbol with its own tint: symbol_<name>_<variant>")
    parser.add_argument("--tint", default="?attr/colorControlNormal", help="a theme attribute, or @color/flag_reviewer_<colour>")
    args = parser.parse_args()

    icons = style()
    family_dir = STYLE_DIRS[icons["family"]]
    res = ROOT / args.module / "src" / "main" / "res" / "drawable"
    if not res.parent.is_dir():
        raise SystemExit(f"no module {args.module}")
    res.mkdir(exist_ok=True)
    entries = read_lock()
    # Downloads go to a new, empty directory outside the repository
    with tempfile.TemporaryDirectory(prefix="material-symbols-") as tmp:
        into = Path(tmp)
        for name in args.symbols:
            stem = file_stem(name, icons)
            svg = download(f"{RAW}/web/{name}/{family_dir}/{stem}.svg", into)
            android = download(f"{RAW}/android/{name}/{family_dir}/{stem}.xml", into)
            paths = path_data(svg)
            mirrored = mirrors(android)
            digest = hashlib.sha256("\n".join(paths).encode()).hexdigest()
            entries[name] = ("mirrored" if mirrored else "fixed", digest)
            file = res / (f"symbol_{name}" + (f"_{args.variant}" if args.variant else "") + ".xml")
            file.write_text(drawable(name, paths, mirrored, args.tint, icons))
            print(f"{file.relative_to(ROOT)}{' (mirrors)' if mirrored else ''}")
    write_lock(entries, icons)


if __name__ == "__main__":
    main()
