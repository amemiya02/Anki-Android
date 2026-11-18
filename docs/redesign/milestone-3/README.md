# Milestone 3 device evidence

The 2026-11-18 walk and package exchange ran on a disposable Pixel 9 emulator with Android 15 AOSP. Both apps were installed side by side. Personal phone verification and owner sign-off are pending at the owner's request. [Milestone issue](../../../.scratch/ankidroid-redesign/issues/31-milestone-3-sign-off-finding-and-adding-on-a-device.md)

| App | Package and version | APK SHA-256 |
| --- | --- | --- |
| Fork (`c63f115e3c`) | `com.ichi2.anki.redesign.debug` · `2.26.0alpha14-debug` · `22600114` | `5cb56c254a71894eccc0df0e4922c08f1b109714350667d66d23976e18f0e2c4` |
| Official App | `com.ichi2.anki` · `2.25.3` · `322503300` | `68de13faf1819008946850aa7c7873f8d60f057ffa96dc2bb5b1ad1bade4df80` |

The Official App was the same published APK verified for Milestone 2. [Release and signing certificate](../milestone-2/README.md)

## Finding and adding

| Locale | Light | Dark | Black |
| --- | --- | --- | --- |
| English (`en-US`) | [Light walk](en-light.png) | [Dark walk](en-dark.png) | [Black walk](en-black.png) |
| Simplified Chinese (`zh-CN`) | [Light walk](zh-light.png) | [Dark walk](zh-dark.png) | [Black walk](zh-black.png) |

1. Start with a disposable collection containing Anatomy and English Decks, and make a PNG available to Android's photo picker. In each locale and theme, add a Basic Note to Anatomy with a unique Front, `Forest` on the Back, the PNG attached to the Back, and the tag `M3walk`.
2. Open Browse and search by that Front. Open the single result, append `Edited` to the Back, and show the answer in the note editor's preview. Check that the answer shows both the image and the appended text. Return to the editor and tap Save.
3. Select the Card in Browse, open Change deck and choose English. Check the new Deck name on its row. The five captures in each sheet show Add, the search result, the edited answer, the Deck picker and the moved Card.
4. Search for `tag:M3walk`, select the two Cards named `M3enblack` and `M3endark`, and move both to Anatomy. Check both rows show Anatomy and the four unselected Cards still show English. Select those same two Cards and move them back to English. [Two-Card move](bulk-move.png)

## Rendered baselines

The browser search and sort matrix and the browser selection matrix each produced 120 captures. The note editor field and save matrix ran 312 cases, and the formatting, attachments and drawing matrix passed 168 cases. The previewer matrix produced 192 captures. Dark and Black card browser preview accessibility checks are still open. The Milestone 1 decision keeps generated Roborazzi baselines under `AnkiDroid/build/outputs/roborazzi/`. The pending milestone gate is to record and verify the complete Finding and Adding baseline set. [Milestone 1 decision](../../../.scratch/ankidroid-redesign/issues/13-milestone-1-sign-off-the-foundation-on-a-device.md)

## Round trip with the Official App

The Fork exported its 42-Card collection after the walkthroughs. The Official App imported it, then graded two English Cards with Good and Easy and exported English and the whole collection. The Fork imported both Official packages, then exported the reviewed English Deck back to the Official App. Both tested Deck exports therefore contain review history. The initial Fork Deck smoke import also matched with 13 Cards, 13 Notes and zero reviews.

| Direction | Export | Cards | Notes | Review entries | Media files |
| --- | --- | ---: | ---: | ---: | ---: |
| Fork → Official App | Reviewed English Deck | 13 | 13 | 2 | 1 |
| Fork → Official App | Collection | 42 | 42 | 9 | 1 |
| Official App → Fork | Reviewed English Deck | 13 | 13 | 2 | 1 |
| Official App → Fork | Collection | 42 | 42 | 11 | 1 |

All four imports ran through the apps' production importers. Each comparison returned zero missing or extra Cards, Notes or review entries. The received image `m3-field.png` was 10,666 bytes in all four comparisons and matched the package SHA-256 `c9c2e1d2dea49eb47970f48cd6635e5925c05787ea03c5c01054b7a22398411c`. [Export and import captures](round-trip.png)

### Repeat the comparison

1. Export the collection as `.colpkg` with media. Export a Deck with reviewed Cards as `.apkg` with scheduling information, Deck presets and media enabled. Leave **Support older Anki versions** off for both formats.
2. Import the Deck into a disposable recipient without that Deck or its Notes. Open the package with the receiving app and enable **Import any learning progress** and **Import any deck presets**. Keep a copy of the recipient database and media after the merge. Import the collection on that same disposable recipient using **Replace**, then copy its database and media again. Repeat in the other direction with both apps.
3. To compare the stored data directly, stop the recipient and copy its `collection.anki2`, SQLite sidecars if present, and `collection.media/` together from the test device. The source database is the decompressed `collection.anki21b` member of the ZIP package.

   ```sh
   unzip -p "$package" collection.anki21b | zstd -dc > source.sqlite
   unzip -p "$package" media | zstd -dc | protoc --decode_raw
   ```

4. Compare the following SQLite rows as multisets. For a Deck import, select recipient Cards in the exported Deck and its descendants, then restrict recipient Notes and review entries to that selection. For a collection replacement, compare the full tables.

   | Data | Fields compared |
   | --- | --- |
   | Cards and scheduling | `cards.id`, `nid`, Deck and original Deck names, `ord`, `type`, `queue`, `due`, `ivl`, `factor`, `reps`, `lapses`, `left`, `odue`, `flags`, `data` |
   | Notes | `notes.guid`, `tags`, `flds` |
   | Review history | `revlog.id`, `cid`, `ease`, `ivl`, `lastIvl`, `factor`, `time`, `type` |

5. Read the package's decoded media manifest in order. Decompress each numbered ZIP member with `zstd -dc` and compare the byte count and SHA-256 with the named file in the recipient media directory.

| Source package used | SHA-256 |
| --- | --- |
| Fork reviewed Deck | `6fd9ff60c3f38ee89433999e616fb715e178820d957eb7c80cbf1001eabf2114` |
| Fork collection | `9330710d7b35b2052dec6cafb269486823dfcdef83d3a595f4e35d372d8e30e8` |
| Official App reviewed Deck | `4462623964c6ad93417304e04aeda136e1f1f7102863c542cba4ed9a141e8da1` |
| Official App collection | `1f4260ddd55a8e51b6ed904553c952d3fd1380f76b32062ec54a5f50270d0efa` |
