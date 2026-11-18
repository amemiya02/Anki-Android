# 18: Deck list: adding, Deck actions and sync

**What to build:** From the deck list the owner can add a Note, create a Deck or a filtered Deck, get shared Decks, act on any Deck, and start a sync, with nothing that exists today missing.

**Blocked by:** 17 (Deck list: Decks, counts and starting a session)

**Status:** done

- [x] A prominent button adds a Note; creating a Deck, creating a filtered Deck and getting shared Decks are reachable beside it.
- [x] Every Deck action available today is reachable from a Deck: rename, options, custom study, export, delete, create sub-Deck, and the rest.
- [x] Sync is in the top bar, shows when a sync is needed or running, and leads to sync setup when no server address is set.
- [x] Every action in the deck list's overflow menu today is still reachable.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.
- [x] Exporting a Deck or the whole collection offers the same choices as today: scheduling, media, Deck options, and a package older versions can read.
- [x] The export screen says whether the package will replace or merge with what is in the other app.

## Comments

Most of what this ticket asks for already existed around the deck list rebuilt in ticket 17; this ticket brings it to the Mockup and the Quality Bar and fills the two gaps (the export text, and naming Deck actions for screen readers). How each criterion was checked:

- **Add button, with create Deck, filtered Deck and shared Decks beside it.** The add button is the Mockup's + alone (16dp corners, cinnabar fill); a tap opens Get shared decks, Create filtered deck and Create deck above it and turns it into "Add" (a Note), as before. `DeckPickerTest` (the FAB tests, unchanged and passing); `add_menu` pictures in `DeckPickerScreenshotTest`.
- **Every Deck action.** Unchanged: a long press (or right click) opens the Deck's menu with rename, options, custom study, export, delete, create sub-Deck, unbury, rebuild/empty, shortcut, browse, description, add, reminders. `DeckPickerTest`'s `ContextMenu ...` tests. New: screen readers announce the long press as "Deck actions" (`DeckListScreenTest`).
- **Sync in the top bar.** Sync icon beside the overflow, with the progress line while syncing and a badge when changes wait (dot) or a one-way sync is needed (!). Without a server address it leads to sync setup (`SyncSetupTest`, ticket 03). Deliberately different: without a server the icon is now plain, as in the Mockup, rather than carrying the "!" badge, since no sync is needed (`DeckPickerTest` `sync icon is plain while no sync server is set`). Owner decision delegated (2026-11-17).
- **Overflow menu.** Unchanged (`res/menu/deck_picker.xml`): undo, checks, backup, restore, Note Types, import, export collection.
- **Matches the Mockup.** `decks`, `empty`, `long_names` pictures now include the add button and the plain sync icon, compared with `decks-en.png` / `decks-empty-*.png`. The open add menu is not drawn in the Mockup; it keeps the old look (Material tonal buttons on the progress-track colour) until ticket 33.
- **Quality Bar.** `DeckPickerScreenshotTest` (`@CheckAccessibility`, zh-cn/ja/ar, 200%): 96 pictures pass with `-Ptheme=light,dark,black`, the add button shown in all of them. The add button's Dark/Black contrast report (tickets 15 and 17) is fixed: the closed button held its "Add" label unseen, so the check measured its corners as text; the label now exists only while the menu is open. The dimmed list behind the open menu is cleared from accessibility (Compose semantics), and the menu's labels wrap at 200%. `long_names` scrolls to the end of the list, where the Decks clear the add button (at 200% they run under it at rest, as any list under a floating button does).
- **Screen-logic and Engine tests.** `DeckPickerViewModelTest` unchanged; the Engine untouched.
- **Export choices.** Unchanged: collection (media, older versions), deck package (scheduling, deck presets, media, older versions), notes and Cards as text. `ExportDialogFragmentTest`.
- **Replace or merge.** Under the collection package's choices: importing it replaces everything in the other app and its next AnkiWeb sync is a one-way upload; under the deck package's: it merges and syncs normally. English, Chinese, Japanese. `ExportDialogFragmentTest`; `ExportDialogFragmentScreenshotTest` pictures. The dialog's own look is ticket 33's.

On a device (Milestone 2 sign-off, ticket 24): the add button's shadow and menu animation, the sync progress line during a real sync, and the instrumented tests (which compile again: the stale bottom bar setting is gone from the instrumented `DeckPickerTest`).
