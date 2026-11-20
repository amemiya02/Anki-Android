#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Verify and identify a Hayaki APK set and an unpacked iOS release app."""

import argparse
import hashlib
import json
import plistlib
import re
import subprocess
from pathlib import Path


def digest(path):
    sha = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            sha.update(chunk)
    return sha.hexdigest()


def android_packages(directory):
    metadata = json.loads((directory / "output-metadata.json").read_text())
    if metadata["variantName"] != "playRelease":
        raise ValueError(f"{directory}: expected the playRelease variant")
    result = {}
    for element in metadata["elements"]:
        filters = element["filters"]
        abi = next((item["value"] for item in filters if item["filterType"] == "ABI"), "universal")
        if abi in result:
            raise ValueError(f"{directory}: duplicate ABI {abi}")
        result[abi] = directory / element["outputFile"]
    if not result:
        raise ValueError(f"{directory}: no APKs")
    return result


def package_info(path, tools):
    metadata = subprocess.check_output([tools / "aapt2", "dump", "badging", path], text=True)
    fields = dict(re.findall(r"(\w+)='([^']+)'", metadata.splitlines()[0]))
    verify = subprocess.check_output([tools / "apksigner", "verify", "--print-certs", path], text=True)
    certs = sorted(re.findall(r"^Signer #\d+ certificate SHA-256 digest: (\w+)$", verify, re.MULTILINE))
    if not certs:
        raise ValueError(f"{path}: missing verified signer")
    return fields["name"], fields["versionName"], int(fields["versionCode"]), certs


def check_android(args):
    previous = android_packages(args.previous)
    current = android_packages(args.candidate)
    if set(previous) != set(current):
        raise ValueError(f"ABI set changed: {sorted(previous)} -> {sorted(current)}")
    records = {}
    for abi in sorted(current):
        old_app, old_version, old_code, old_certs = package_info(previous[abi], args.build_tools)
        app, version, code, certs = package_info(current[abi], args.build_tools)
        if app != old_app or app != "com.ichi2.anki.redesign" or certs != old_certs:
            raise ValueError(f"{abi}: application ID or signing certificate changed")
        if version != args.version or code <= old_code:
            raise ValueError(f"{abi}: expected {args.version} and a code above {old_code}, found {version} ({code})")
        records[abi] = dict(
            applicationId=app, previousVersion=old_version, previousCode=old_code,
            version=version, code=code, signerSHA256=certs, apkSHA256=digest(current[abi]),
        )
    return records


def check_ios(args):
    records = {}
    bundles = [args.ios_app, *sorted((args.ios_app / "PlugIns").glob("*.appex"))]
    for bundle in bundles:
        with (bundle / "Info.plist").open("rb") as source:
            info = plistlib.load(source)
        version, build = info["CFBundleShortVersionString"], info["CFBundleVersion"]
        if (version, build) != (args.version, args.ios_build):
            raise ValueError(f"{bundle}: expected {args.version} ({args.ios_build}), found {version} ({build})")
        records[info["CFBundleIdentifier"]] = dict(version=version, build=build)
    base = "io.github.amemiya02.hayaki"
    if set(records) != {base, base + ".share", base + ".widgets"}:
        raise ValueError(f"Unexpected iOS app or extension identifiers: {sorted(records)}")
    return records


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--previous", required=True, type=Path, help="previous signed play/release output directory")
    parser.add_argument("--candidate", required=True, type=Path, help="new signed play/release output directory")
    parser.add_argument("--build-tools", required=True, type=Path, help="Android SDK build-tools version directory")
    parser.add_argument("--ios-app", required=True, type=Path, help="Hayaki.app from the matching build")
    parser.add_argument("--version", required=True, help="shared public version")
    parser.add_argument("--ios-build", required=True, help="expected iOS build number")
    options = parser.parse_args()
    print(json.dumps(dict(android=check_android(options), ios=check_ios(options)), indent=2))
