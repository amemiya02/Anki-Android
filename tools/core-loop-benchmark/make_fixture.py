# SPDX-License-Identifier: GPL-3.0-or-later

"""Generate the disposable 100,000-Card release benchmark fixture."""

import argparse
import itertools
import sqlite3
import tempfile
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

import genanki


CARD_COUNT = 100_000
TIMESTAMP = 1_700_000_000


def face(field, color):
    return f'<div aria-hidden="true" style="width:64px;height:64px;margin:16px auto;background:{color}"></div>{{{{{field}}}}}'


def generate(output):
    model = genanki.Model(
        1700001001,
        "Performance Basic",
        fields=[{"name": "Front"}, {"name": "Back"}],
        templates=[{
            "name": "Card 1",
            "qfmt": face("Front", "#00ee99"),
            "afmt": face("Back", "#ee0099"),
        }],
    )
    deck = genanki.Deck(1700001002, "Browser fixture")
    for number in range(CARD_COUNT):
        deck.add_note(genanki.Note(
            model=model,
            fields=[f"Browser fixture {number:05}", f"Answer {number:05}"],
            due=number + 1,
        ))

    with tempfile.TemporaryDirectory() as directory:
        database = Path(directory) / "collection.anki2"
        with sqlite3.connect(database) as collection:
            genanki.Package(deck).write_to_db(collection.cursor(), TIMESTAMP, itertools.count(TIMESTAMP * 1000))
        collection.close()
        with ZipFile(output, "x") as package:
            package.writestr(ZipInfo(database.name), database.read_bytes(), compress_type=ZIP_DEFLATED)
            package.writestr(ZipInfo("media"), b"{}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    generate(parser.parse_args().output)
