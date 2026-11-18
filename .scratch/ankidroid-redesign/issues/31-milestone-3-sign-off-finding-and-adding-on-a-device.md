# 31: Milestone 3 sign-off: finding and adding on a device

**What to build:** The owner adds, finds, edits and previews Cards in the Fork and agrees the Milestone is done.

**Blocked by:** 27 (Card browser: selection, bulk actions and columns); 29 (Note editor: formatting, tags and attachments); 30 (Previewer)

**Status:** ready-for-agent

- [x] On the emulator, a Note is added with an image and a tag, found in the card browser, edited, previewed and bulk-moved to another Deck.
- [x] The walk is done in English and Simplified Chinese and captured in all three themes.
- [ ] Baseline pictures are regenerated under `AnkiDroid/build/` and verified, following the decision in ticket 13.
- [ ] The owner has repeated the walk on their own phone and signed off the Milestone.
- [x] Round trip with the Official App: a collection and a Deck exported from the Fork import into the current release of the Official App with Cards, scheduling, review history and media intact, and the same works in the other direction.

## Comments

On 2026-11-18 the owner asked development to continue across milestones while personal phone sign-off stays pending. Emulator checks and the package round trip still apply.

On the Android 15 Pixel 9 emulator, six walks added an image and tag, found the saved Card, edited and previewed the answer, and moved the Card to English. A separate two-Card selection moved both Cards to Anatomy and back to English. [Device evidence](../../../docs/redesign/milestone-3/README.md)

Both production importers preserved the Cards, scheduling, review history and media. The two collection comparisons matched 42 Cards and Notes, with nine and 11 review entries. Each reviewed Deck comparison matched 13 Cards and Notes and two review entries. [Package comparison](../../../docs/redesign/milestone-3/README.md#round-trip-with-the-official-app)

The previewer passed accessibility checks in all 96 rendered cases. The complete milestone baseline record and verification run remains open. The owner deferred the personal phone walk and sign-off.
