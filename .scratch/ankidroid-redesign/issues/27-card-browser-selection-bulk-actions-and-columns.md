# 27: Card browser: selection, bulk actions and columns

**What to build:** The owner can select many Cards and act on them together, and choose what the card browser shows.

**Blocked by:** 26 (Card browser: list, search and sort)

**Status:** implemented, rendered verification in progress

- [x] Cards can be selected one by one, as a range, and all at once.
- [ ] Every bulk action available today works on a selection: change Deck, flag, mark, suspend, bury, reschedule, delete, tags, and the rest.
- [x] Columns can be chosen and are remembered.
- [x] A selected Card can be previewed.
- [x] Saved searches and the browser's options are reachable.
- [x] While selecting, change Deck, flag, tags and suspend sit in a bottom action bar and every other bulk action is under More.
- [x] The Columns page sets the shown columns and their order for each mode; the first is the row's title (or the table, if the owner picks B).
- [x] It matches the agreed Mockup.
- [ ] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [ ] Screen-logic tests and Engine tests still pass unchanged.

## Verification

The first rendered test failed against its base at `f314126be5` because the Change deck action was absent from the selection bar. The same test drives the new bar through its accessibility labels.

The ARM64 debug APK with SHA-256 `e1767d03af6cdd3503a4432128a4f6b4c76c87f16eb7f5b507a7093fea9666c0` opened seven Anatomy results on the Pixel 9 emulator. A range selected four Cards, a tap reduced the selection to three and Select all took it to seven. More exposed the remaining operations. Preview opened at the selected Card and kept the result context.

Columns saved separate orders for Cards and Notes. Moving Card Type and Note Type to the top made the Browse rows open with “Card 1” and “Basic” respectively. Returning to Columns showed the saved order and the original orders were restored after the check. The page saves each change immediately. The drag handle also opens Move up and Move down for keyboard and accessibility navigation.

Engine verification completed with 143 passing and seven skipped tests. Device captures and the red test log are in `/tmp/anki-redesign-qa/`.
