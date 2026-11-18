# 22: JavaScript API in the study screen: Card actions, speech and the rest

**What to build:** Every remaining part of the AnkiDroid JavaScript API works in the study screen, so that no Card needs the old study screen any more.

**Blocked by:** 21 (JavaScript API in the study screen: reading the Card and answering)

**Status:** done

- [x] A Card Template can perform every Card action the API offers: mark, flag, bury, suspend, reset progress, set a due date, search, and the rest.
- [x] A Card Template can use the API's speech, speech recognition, messages and screen controls.
- [x] Every call the old study screen supports is supported, checked against a list of the whole API.
- [x] The "not yet supported" warning can no longer appear, and it is removed.
- [x] The tests that cover the API on the old study screen pass against the study screen.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Comments

- The study screen and old study screen share the versioned API handler. Its 68 endpoints, including initialization, match the public script. Card actions use the study screen's queue and undo handling. The tag editor keeps the Note it opened for, and asynchronous search and speech callbacks stay with their Card Content.
- Searching the collection for returned fields and tagging a different Note still require the existing dangerous JavaScript API opt-in. Speech recognition on the study screen requires both template audio recording consent and Android microphone permission.
- Fullscreen calls control the Card Frame and system bars for the current study session. The navigation call opens the app destinations from a native dialog. Options are reachable from the Card even with the toolbar hidden.
- Pixel 9 Android 16 WebView verification covered all four bury and suspend actions, due dates, reset, every flag, marking, tags, search and its callback, reload, fullscreen, scrollbars, menus, toasts, text-to-speech and the speech recognition permission error. Seventeen cases passed in the class run. The two revised fullscreen and reload cases passed in the focused run. The mark and flag test failed on the ticket 21 build and passed with the shared handler. The focused consent check denied recording with Android microphone permission granted and template recording disabled. Live transcription remains unverified.
- The existing reviewer and study screen logic suite passed 231 tests with 2 skipped. The Engine suite passed 143 tests with 7 skipped.
