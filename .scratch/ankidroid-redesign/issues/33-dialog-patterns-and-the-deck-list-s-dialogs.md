# 33: Dialog patterns and the deck list's dialogs

**What to build:** Every dialog the owner meets from the deck list shares one look, built from a small set of dialog patterns that the rest of the app will reuse.

**Blocked by:** 32 (Mockups for settings and dialogs)

**Status:** ready-for-agent

- [x] The seven dialog patterns exist as reusable pieces.
- [ ] Every dialog reachable from the deck list uses them: create, rename and delete Deck, the backup prompt, sync prompts and errors, export, and the rest.
- [ ] A destructive choice is always recognisable in the same way.
- [ ] Each dialog does what it did before.
- [ ] It matches the agreed Mockup.
- [ ] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [ ] Screen-logic tests and Engine tests still pass unchanged.


## Implementation

- `common/android/src/main/java/com/ichi2/compose/ui/dialogs` holds the seven reusable patterns and the shared window, title, mark and actions. Compose callers can present a dialog directly. Fragment routes can use `createPaperDialog` with their existing argument and result handling.
- Deck list entry points use these pieces for deck creation, renaming, deletion confirmation, backup and recovery, sync, export, maintenance, custom study, deck descriptions and the initial import picker. Import processing and its remaining option dialogs are shared with other screens and are tracked in ticket 35.
- Rendered fixtures cover the seven patterns in `DialogPatternsScreenshotTest` and deck creation through the activity in `DeckPickerDialogsScreenshotTest`.
