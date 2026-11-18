# 35: Dialogs: note editor, importing, errors and the rest

**What to build:** No dialog anywhere in the app still shows the old look.

**Blocked by:** 33 (Dialog patterns and the deck list's dialogs)

**Status:** done

- [x] Every remaining dialog uses the shared patterns: note editor, Note Types, import and export, permissions, media check, database errors, and the rest.
- [x] Each dialog does what it did before.
- [x] A search of the app finds no dialog built the old way.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Implementation and verification

- Editor confirmations, Note Type and Deck selection, formatting, and Card Template field selection use the shared title, choice, input, and action patterns. Export has one path for collection, Deck, and Browse selections. Import retains package routing, collection replacement, and the Anki Page importer.
- Media Check, Manage Space, shared-deck, synchronization, import-error, and permission prompts use the shared patterns. Optional notification access still opens Android's permission prompt or the app's notification settings according to OS version. The existing database and backup recovery routes already use the Paper patterns from ticket 33.
- The red run of `RemainingDialogsTest` reached the actual discard, import, and export dialogs. All three assertions failed because the previous windows exposed no Compose semantics. The passing cases were recorded in the first focused converted run.
- The focused rerun reached 157 tests and 156 passed. The failure showed that the maintenance result was opened and then popped by the recovery confirmation. After moving dismissal before the check or repair, both recovery tests passed, including the result after checking the database from Study Options.
- The rendered capture suite covers editor discard, collection replacement, and named Deck export in the theme, language, and font-size matrix. All 72 cases passed the accessibility checks. The Engine rerun passed 143 tests with seven existing skips. The owner deferred phone acceptance.
- The reminder form and time entry, Instant Note Editor, and About dialog now use the Paper components. Ticket 39 removed the XML-only test fallbacks from the Deck and Custom Study dialogs. A production-source audit found that the only Material alert primitive is the shared Compose window used by the dialog components. The toolbar layout dialog now announces its title as a heading. Its heading regression failed on the previous window, and all five toolbar layout tests passed with the shared window.
