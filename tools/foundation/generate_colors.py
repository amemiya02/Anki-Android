#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Generates Android Foundation palettes from its Material 3 roles and the
pinned iOS Theme definitions, and checks text pairs for WCAG AA contrast.

Run from the repository root:  python tools/foundation/generate_colors.py
"""
from copy import deepcopy
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
VALUES = ROOT / "docs/redesign/material-3.values.json"
BRAND = ROOT / "docs/brand/themes.json"
OUTPUT = ROOT / "common/android/src/main/res/values/foundation_colors.xml"
THEME_OUTPUT = OUTPUT.with_name("hayaki_themes.xml")
THEMES = ("light", "dark", "black")
# Stand-ins for how Card Content is drawn today; the Redesign does not draw them
CARD_SHEET = {"cardSheet", "onCardSheet", "cardSheetRule"}


def snake(name: str) -> str:
    return re.sub(r"([A-Z])", lambda m: "_" + m.group(1).lower(), name)


def luminance(hex_colour: str) -> float:
    h = hex_colour.lstrip("#")
    channels = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    linear = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
    return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]


def ratio(a: str, b: str) -> float:
    high, low = sorted((luminance(a), luminance(b)), reverse=True)
    return (high + 0.05) / (low + 0.05)


def tint(colour: str, over: str, alpha: float) -> str:
    f = [int(colour.lstrip("#")[i:i + 2], 16) for i in (0, 2, 4)]
    b = [int(over.lstrip("#")[i:i + 2], 16) for i in (0, 2, 4)]
    # Match Swift's default rounding for positive sRGB channel values.
    return "#" + "".join("%02X" % int(alpha * x + (1 - alpha) * y + 0.5) for x, y in zip(f, b))


# Android keeps Material 3's tonal surface hierarchy. Its neutral surfaces
# receive the same accent tint opacity as iOS screen backgrounds.
SURFACES = {
    "light": {
        "surface": "#FAFAFA", "surfaceVariant": "#E4E4E4",
        "surfaceContainerLowest": "#FFFFFF", "surfaceContainerLow": "#F8F8F8",
        "surfaceContainer": "#F3F3F3", "surfaceContainerHigh": "#EBEBEB",
        "surfaceContainerHighest": "#E4E4E4", "surfaceDim": "#DFDFDF",
        "surfaceBright": "#FFFFFF",
    },
    "dark": {
        "surface": "#121212", "surfaceVariant": "#444444",
        "surfaceContainerLowest": "#0D0D0D", "surfaceContainerLow": "#1A1A1A",
        "surfaceContainer": "#1F1F1F", "surfaceContainerHigh": "#282828",
        "surfaceContainerHighest": "#323232", "surfaceDim": "#121212",
        "surfaceBright": "#373737",
    },
    "black": {
        "surface": "#000000", "surfaceVariant": "#444444",
        "surfaceContainerLowest": "#000000", "surfaceContainerLow": "#0F0F0F",
        "surfaceContainer": "#141414", "surfaceContainerHigh": "#1B1B1B",
        "surfaceContainerHighest": "#262626", "surfaceDim": "#000000",
        "surfaceBright": "#292929",
    },
}


def with_brand_palette(values: dict, palette: dict, opacity: dict) -> dict:
    result = deepcopy(values)
    fixed = result["materialFixed"]
    for mode in THEMES:
        side = "light" if mode == "light" else "dark"
        accent = palette[side]
        on_accent = "#FFFFFF" if mode == "light" else "#000000"
        on_background = "#1D1D1D" if mode == "light" else "#E6E6E6"
        muted = "#484848" if mode == "light" else "#CACACA"
        outline = {"light": "#CACACA", "dark": "#494949", "black": "#3C3C3C"}[mode]
        surface = SURFACES[mode]
        tonal_base = "#FFFFFF" if mode == "light" else surface["surfaceContainer"]
        primary_container = tint(accent, tonal_base, 0.18 if mode == "light" else 0.22)
        secondary_container = tint(accent, tonal_base, 0.11 if mode == "light" else 0.15)
        if mode == "black":
            canvas = "#000000"
        else:
            fallback = tint(accent, "#FFFFFF" if mode == "light" else "#000000", opacity[mode])
            canvas = palette.get(side + "Canvas", fallback)
        surfaces = {role: tint(accent, neutral, opacity[mode]) for role, neutral in surface.items()}
        result["colour"][mode].update(
            background=surfaces["surface"], onBackground=on_background,
            onBackgroundMuted=muted, outline=outline, primary=accent, onPrimary=on_accent,
            emphasis=accent, progressTrack=secondary_container, progressFill=accent, studyCanvas=canvas,
        )
        result["material"][mode].update(
            primary=accent, onPrimary=on_accent, primaryContainer=primary_container,
            onPrimaryContainer=on_background, secondary=accent, onSecondary=on_accent,
            secondaryContainer=secondary_container, onSecondaryContainer=on_background,
            tertiary=accent, onTertiary=on_accent, tertiaryContainer=primary_container,
            onTertiaryContainer=on_background, onSurface=on_background, onSurfaceVariant=muted,
            outline="#777777" if mode == "light" else "#949494", outlineVariant=outline,
            inverseSurface="#333333" if mode == "light" else "#F5F5F5",
            inverseOnSurface="#F5F5F5" if mode == "light" else "#333333",
            inversePrimary=palette["dark" if mode == "light" else "light"], **surfaces,
        )
    for role, amount in (("primary", 0.18), ("secondary", 0.11), ("tertiary", 0.18)):
        fixed[role + "Fixed"] = tint(palette["light"], "#FFFFFF", amount)
        fixed[role + "FixedDim"] = tint(palette["light"], "#FFFFFF", amount + 0.13)
        fixed["on" + role.capitalize() + "Fixed"] = "#1D1D1D"
        fixed["on" + role.capitalize() + "FixedVariant"] = "#333333"
    return result


def theme_resources(palettes: dict, default: str) -> str:
    parents = {"light": "Theme_Light", "dark": "Theme_Dark", "black": "Theme_Dark.Black"}
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<!-- SPDX-License-Identifier: GPL-3.0-or-later -->",
        "<!-- Generated by tools/foundation/generate_colors.py from docs/brand/themes.json. -->",
        "<resources>",
    ]
    base = palettes[default]
    for theme, values in palettes.items():
        if theme == default:
            continue
        for mode, parent in parents.items():
            lines.append(f'    <style name="HayakiTheme.{theme.capitalize()}.{mode.capitalize()}" parent="{parent}">')
            for role, value in values["colour"][mode].items():
                if base["colour"][mode][role] != value:
                    lines.append(f'        <item name="foundation{role[0].upper() + role[1:]}">{value}</item>')
            for bucket in (values["material"][mode], values["materialFixed"]):
                reference = base["material"][mode] if bucket is values["material"][mode] else base["materialFixed"]
                for role, value in bucket.items():
                    if reference[role] != value:
                        # The Java Material attributes name the inverse roles in reverse order.
                        name = {
                            "inversePrimary": "colorPrimaryInverse",
                            "inverseSurface": "colorSurfaceInverse",
                            "inverseOnSurface": "colorOnSurfaceInverse",
                        }.get(role, "color" + role[0].upper() + role[1:])
                        lines.append(f'        <item name="{name}">{value}</item>')
            lines.append("    </style>")
    lines.append("</resources>")
    return "\n".join(lines) + "\n"


def contrast_failures(values: dict) -> list:
    failures = []
    rules = values["contrast"]
    for theme in THEMES:
        roles, scheme = values["colour"][theme], values["material"][theme]
        pairs = [(fg, bg, 4.5) for fg, bg in rules["text"]] + [(fg, bg, 3.0) for fg, bg in rules["marks"]]
        for fg, bg, need in pairs:
            if ratio(roles[fg], roles[bg]) < need:
                failures.append(f"{theme}: {fg} on {bg} = {ratio(roles[fg], roles[bg]):.2f}")
        # counts sit on a 10% tint of their own colour over the Deck container
        for count in ("countNew", "countLearning", "countDue"):
            pill = tint(roles[count], scheme["surfaceContainerLow"], 0.10)
            if ratio(roles[count], pill) < 4.5:
                failures.append(f"{theme}: {count} on its pill = {ratio(roles[count], pill):.2f}")
        # answer names on the answer buttons
        for answer in ("answerAgain", "answerHard", "answerGood", "answerEasy"):
            if ratio(roles[answer], scheme["surfaceContainerHigh"]) < 4.5:
                failures.append(f"{theme}: {answer} on surfaceContainerHigh")
        for fg, bg in (("onSurfaceVariant", "surfaceContainerHigh"), ("onSurfaceVariant", "surfaceContainerLow"),
                       ("onPrimaryContainer", "primaryContainer"), ("onSecondaryContainer", "secondaryContainer"),
                       ("onSurface", "surfaceContainerLow"), ("primary", "surfaceContainerLow")):
            if ratio(scheme[fg], scheme[bg]) < 4.5:
                failures.append(f"{theme}: {fg} on {bg} = {ratio(scheme[fg], scheme[bg]):.2f}")
    return failures


def resources(values: dict) -> str:
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<!-- SPDX-License-Identifier: GPL-3.0-or-later -->",
        "<!--",
        "  Generated by tools/foundation/generate_colors.py from the Android Material 3 roles",
        "  and the iOS brand palette in docs/brand/themes.json.",
        "-->",
        '<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="UnusedResources">',
    ]
    for theme in THEMES:
        lines.append(f"    <!-- {theme.capitalize()}: Foundation roles -->")
        for role, colour in values["colour"][theme].items():
            if role not in CARD_SHEET:
                lines.append(f'    <color name="foundation_{theme}_{snake(role)}">{colour}</color>')
        lines.append(f"    <!-- {theme.capitalize()}: Material 3 scheme -->")
        for role, colour in values["material"][theme].items():
            lines.append(f'    <color name="scheme_{theme}_{snake(role)}">{colour}</color>')
    lines.append("    <!-- Fixed roles: the same in every theme -->")
    for role, colour in values["materialFixed"].items():
        lines.append(f'    <color name="scheme_{snake(role)}">{colour}</color>')
    lines.append("</resources>")
    return "\n".join(lines) + "\n"


def main() -> int:
    values = json.loads(VALUES.read_text(encoding="utf-8"))
    brand = json.loads(BRAND.read_text(encoding="utf-8"))
    palettes = {
        theme: with_brand_palette(values, palette, brand["tintOpacity"])
        for theme, palette in brand["palettes"].items()
    }
    failures = [f"{theme}: {failure}" for theme, palette in palettes.items() for failure in contrast_failures(palette)]
    if failures:
        print("Contrast below WCAG AA:", *failures, sep="\n  ")
        return 1
    default = brand["default"]
    default_palette = palettes[default]
    if values != default_palette:
        print(f"Update {VALUES.relative_to(ROOT)} to match the generated default palette.")
        return 1
    OUTPUT.write_text(resources(default_palette), encoding="utf-8", newline="\n")
    THEME_OUTPUT.write_text(theme_resources(palettes, default), encoding="utf-8", newline="\n")
    print(f"wrote {OUTPUT.relative_to(ROOT)} and {THEME_OUTPUT.relative_to(ROOT)}; every pair meets WCAG AA")
    return 0


if __name__ == "__main__":
    sys.exit(main())
