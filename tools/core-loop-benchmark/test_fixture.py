# SPDX-License-Identifier: GPL-3.0-or-later

"""Exercise the generated import package through its public command."""

import json
import sqlite3
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path


class FixtureTest(unittest.TestCase):
    def test_generates_a_repeatable_media_free_collection_with_distinct_card_faces(self):
        with tempfile.TemporaryDirectory() as directory:
            first = Path(directory) / "first.apkg"
            second = Path(directory) / "second.apkg"
            for output in (first, second):
                subprocess.run(
                    [sys.executable, str(Path(__file__).with_name("make_fixture.py")), str(output)],
                    check=True,
                )
            self.assertEqual(first.read_bytes(), second.read_bytes())
            with zipfile.ZipFile(first) as package:
                self.assertEqual(json.loads(package.read("media")), {})
                database = package.extract("collection.anki2", directory)
            with sqlite3.connect(database) as collection:
                self.assertEqual(collection.execute("select count(*) from cards").fetchone(), (100_000,))
                self.assertEqual(collection.execute("select sfld from notes order by id limit 1").fetchone(), ("Browser fixture 00000",))
                models = json.loads(collection.execute("select models from col").fetchone()[0])
                template = next(iter(models.values()))["tmpls"][0]
                self.assertIn("#00ee99", template["qfmt"])
                self.assertIn("#ee0099", template["afmt"])


if __name__ == "__main__":
    unittest.main()
