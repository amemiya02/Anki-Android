# 40: Milestone 4 sign-off: settings and dialogs on a device

**What to build:** The owner changes settings and meets dialogs across the Fork and agrees the Milestone is done.

**Blocked by:** 39 (Remove the old dialog and settings machinery)

**Status:** ready-for-agent

- [x] On the emulator, a setting is found by search and changed, the theme is switched through all three, and one dialog of each pattern is triggered.
- [x] The walk is done in English and Simplified Chinese in Light, Dark and Black.
- [ ] Baseline pictures are regenerated under `AnkiDroid/build/` and verified, following the decision in ticket 13.
- [ ] The owner has signed off the Milestone.
- [x] Round trip with the Official App: a collection and a Deck exported from the Fork import into the current release of the Official App with Cards, scheduling, review history and media intact, and the same works in the other direction.

## Comments

On 2026-11-18 the owner asked development to continue across milestones while personal phone sign-off stays pending. Emulator checks and the package round trip still apply.

On the Android 15 Pixel 9 emulator, both apps imported the reviewed English Deck and the whole collection. All four database and media comparisons matched, including 4 and 6 Deck review entries and 13 and 15 collection review entries. [Round-trip evidence](../../../docs/redesign/milestone-4/README.md#round-trip-with-the-official-app)

The emulator walk changed and restored Fetch media on sync through Settings search, switched all three themes and reached all seven dialog patterns in both languages. A disposable 100,000-Card collection made the real database-check progress visible. [Settings and dialog gallery](../../../docs/redesign/milestone-4/README.md#settings-and-dialogs)

The screenshot matrix recorded baselines for 2,136 cases in 21 suites, including the corrected localized Add profile captures. The full comparison is pending.
