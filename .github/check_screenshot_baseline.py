# SPDX-License-Identifier: GPL-3.0-or-later

from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def check_results(directory):
    executed = failed = 0
    for report in directory.rglob("TEST-*.xml"):
        for case in ET.parse(report).iter("testcase"):
            if case.find("skipped") is not None:
                continue
            executed += 1
            failure = case.find("failure")
            if failure is None:
                failure = case.find("error")
            if failure is None:
                continue
            details = " ".join((failure.get("type", ""), failure.get("message", ""), failure.text or ""))
            if case.get("classname", "").startswith(("Gradle Test Executor", "Gradle Test Run")) or any(
                error in details for error in ("TestSuiteExecutionException", "OutOfMemoryError")
            ):
                raise ValueError(f"Historical screenshot suite could not execute {case.get('name')} ({report.name})")
            failed += 1
    if executed == 0:
        raise ValueError("No historical screenshot tests executed")
    return executed, failed


if __name__ == "__main__":
    try:
        executed, failed = check_results(Path(sys.argv[1]))
    except (ET.ParseError, OSError, ValueError) as error:
        sys.exit(f"Cannot use the historical screenshot baseline. {error}")
    print(f"Historical screenshot suite executed {executed} tests and reported {failed} failures.")
