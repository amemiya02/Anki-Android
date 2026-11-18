# 50: Finish the single-activity structure and remove leftover XML

**What to build:** Moving between any two screens in the Fork feels the same, and the code that drew the old interface is gone.

**Blocked by:** 42 (Note Types and fields); 43 (Card Template editor and its previewer); 44 (Sync setup around a self-hosted server); 45 (First run for a Newcomer); 46 (Home-screen widgets with Glance); 47 (Notifications); 48 (Harmonisation of the Anki Pages); 49 (Remaining small surfaces)

**Status:** done

- [x] Every rebuilt screen is reached through Compose navigation in one activity.
- [x] The system back gesture and screen transitions behave the same everywhere.
- [x] The XML layouts and theme pieces of the old interface are deleted; no surface uses them.
- [x] Opening the app from a link, a shared file, a widget or a notification still lands in the right place.
- [x] Screen-logic tests and Engine tests still pass unchanged.

In-app destinations run through the Compose navigation host in DeckPicker. Native text fields, card WebViews and the tablet panes retain their Android behavior and return results through the same host. The app removed 50 obsolete layout resources and their unused theme styles. Four Android 12 widget drawables still use the platform's RemoteViews layout resources.

## Verification

On Android, Back gestures returned from Settings through More to Decks. Browse reopened after Android restarted the app process, and Back from the split tablet editor restored Study. Browser links, shared text and CSV files, Add Note and due-count widgets, and posted reminder taps reached their intended screens.

Android's launcher opened Browse with 13 cards and Study with a real question. Each shortcut and its Back action left a single DeckPicker activity. Regression tests replaying previously pinned Browse and Study intents failed before the compatibility fix and passed afterwards.

The 25 focused navigation, restoration, insets, drawing and widget checks passed. The launcher and external-intent checks passed all 17 cases. The final compatibility run passed both previously pinned shortcut tests and all six Study launch tests. The Engine suite passed 143 tests with seven existing skips. App test fixtures now follow the hosted navigation and exercise the original screen behavior.

The wider device walkthrough is tracked in [Milestone 5 sign-off](51-milestone-5-sign-off-the-redesign-is-done.md).
