# 33: Dialog patterns and the deck list's dialogs

**What to build:** Every dialog the owner meets from the deck list shares one look, built from a small set of dialog patterns that the rest of the app will reuse.

**Blocked by:** 32 (Mockups for settings and dialogs)

**Status:** done

- [x] The seven dialog patterns exist as reusable pieces.
- [x] Every dialog reachable from the deck list uses them: create, rename and delete Deck, the backup prompt, sync prompts and errors, export, and the rest.
- [x] A destructive choice is always recognisable in the same way.
- [x] Each dialog does what it did before.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.


## Implementation

- `common/android/src/main/java/com/ichi2/compose/ui/dialogs` holds the seven reusable patterns and the shared window, title, mark and actions. Compose callers can present a dialog directly. Fragment routes can use `createPaperDialog` with their existing argument and result handling.
- Deck list entry points use these pieces for deck creation, renaming, deletion confirmation, backup and recovery, sync, export, maintenance, custom study, deck descriptions and the initial import picker. Import processing and its remaining option dialogs are shared with other screens and are tracked in ticket 35.
- Rendered fixtures cover the seven patterns in `DialogPatternsScreenshotTest` and deck creation through the activity in `DeckPickerDialogsScreenshotTest`.

## Verification

- The pre-change rendered test failed in all eight Light cases across English, Simplified Chinese, Japanese, Arabic and both font sizes. The Create action was missing from the Compose accessibility tree.
- The Engine suite completed with 143 passes and 7 skips.
- The Mockup contrast check passed every Foundation pairing in Light, Dark and Black.
- The rendered matrix passed all 192 pattern/progress cases across three themes, four locales and two font scales, including all 24 destructive-choice cases after correcting safe-action focus. Deck creation also passed all 24 configurations. All 67 common action tests passed.
