# SPDX-License-Identifier: GPL-3.0-or-later

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from check_screenshot_baseline import check_results


class HistoricalScreenshotResultsTest(unittest.TestCase):
    def setUp(self):
        directory = TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.reports = Path(directory.name)

    def write_report(self, cases):
        (self.reports / "TEST-historical.xml").write_text(
            f"<testsuite>{cases}</testsuite>", encoding="utf-8"
        )

    def test_historical_assertions_remain_in_the_count(self):
        self.write_report(
            '<testcase name="captured" />'
            '<testcase name="captured then failed"><failure type="java.lang.AssertionError" /></testcase>'
            '<testcase name="skipped"><skipped /></testcase>'
        )
        self.assertEqual(check_results(self.reports), (2, 1))

    def test_a_successful_capture_cannot_hide_a_worker_exit(self):
        self.write_report(
            '<testcase name="captured" />'
            '<testcase classname="Gradle Test Executor 4" name="failed to execute tests">'
            '<failure type="org.gradle.api.internal.tasks.testing.TestSuiteExecutionException" />'
            '</testcase>'
        )
        with self.assertRaisesRegex(ValueError, "could not execute"):
            check_results(self.reports)

    def test_an_oom_is_a_recording_failure(self):
        self.write_report(
            '<testcase classname="ScreenshotTest" name="started">'
            '<failure type="java.lang.RuntimeException">Caused by: java.lang.OutOfMemoryError: Java heap space</failure>'
            '</testcase>'
        )
        with self.assertRaisesRegex(ValueError, "could not execute"):
            check_results(self.reports)

    def test_a_missing_report_is_a_recording_failure(self):
        with self.assertRaisesRegex(ValueError, "No historical screenshot tests executed"):
            check_results(self.reports)

    def test_a_fully_skipped_suite_is_a_recording_failure(self):
        self.write_report('<testcase name="skipped"><skipped /></testcase>')
        with self.assertRaisesRegex(ValueError, "No historical screenshot tests executed"):
            check_results(self.reports)
