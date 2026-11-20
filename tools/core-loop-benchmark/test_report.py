"""Check the frame report against an Android framestats example."""

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


class ReportTest(unittest.TestCase):
    def test_reports_only_new_valid_frames_and_uses_androids_deadline(self):
        with tempfile.TemporaryDirectory() as directory:
            capture = Path(directory) / "capture.txt"
            capture.write_text("""---PROFILEDATA---
Flags,FrameTimelineVsyncId,IntendedVsync,FrameDeadline,FrameCompleted,
0,100,50000000,66000000,64000000,
0,101,66000000,81000000,82000000,
0,102,82000000,98000000,97000000,
0,103,98000000,114000000,144000000,
1,104,114000000,130000000,140000000,
0,105,130000000,146000000,9223372036854775807,
---PROFILEDATA---
""")
            result = subprocess.check_output([
                sys.executable, str(Path(__file__).with_name("run.py")),
                "frames", str(capture), "--after", "50000000", "--refresh-rate", "60",
            ])
            self.assertEqual(json.loads(result), {
                "frames": 3, "missed_deadlines": 2,
                "overrun_refresh_intervals": {"0": 1, "1": 1, "2": 1},
                "render_ms": {"p50": 16.0, "p95": 46.0, "p99": 46.0},
            })


if __name__ == "__main__":
    unittest.main()
