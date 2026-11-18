# 23: Remove the old study screen

**What to build:** There is one study screen. The old study screen, the setting that chose between the two, and the settings only the old one used are gone, with nothing lost that the owner chose to keep.

**Blocked by:** 20 (Study screen: tools around the Card); 22 (JavaScript API in the study screen: Card actions, speech and the rest)

**Status:** ready-for-agent

- [x] Every feature only the old study screen had is either working in the study screen or was marked "drop" in the Core Loop Mockup ticket.
- [x] Nothing in the app can open the old study screen: not a setting, a link, a widget, a notification or another app.
- [x] The setting that chose between the two study screens is gone, and someone who had the old one selected lands in the study screen.
- [x] Settings only the old study screen used are removed; settings both used still work.
- [x] The old study screen's code, layouts and tests are deleted, and tests of behaviour both screens shared now run against the study screen.
- [ ] The app builds and every remaining test passes.
- [ ] Screen-logic tests and Engine tests still pass unchanged.


## Implementation and verification

- The owner’s [32 decisions](../../../docs/redesign/core-loop/old-study-screen-features.md) are carried by tickets 19–22. The study bar shows the Deck and estimate. Study settings keep the shared audio, time, accessibility and binding preferences. The compatible JavaScript ETA response still reads a saved ETA preference and keeps its historical zero default.
- DeckPicker and the launcher shortcut open the study screen. Deck shortcuts, widgets and reminders go through the existing IntentHandler storage checks. The exported former activity name remains as an alias into those checks for installed shortcuts and external calls. It opens the current Deck when no valid Deck ID is supplied. Android’s CardViewerActivity remains internal, including both preview surfaces.
- The regression tests for an explicit old-screen choice failed on the previous build for the normal and external review launch paths. Both resolved to the former activity. The updated tests also cover saved shortcuts, explicit Deck IDs, deleted Decks, the republished launcher shortcut and session completion.
- The JavaScript API test exercises the same Card Template reads as the two-screen comparison from ticket 22. It verifies expected JSON values and the string return types for intervals and Deck name through the study WebView. The typing, shared audio settings, and missing-media checks also exercise that screen.
- The ARM64 debug app and instrumentation APKs built. On the Pixel 9 emulator, a cold launch through the former Android component opened CardViewerActivity. Eleven focused Android cases passed across two runs, including the Card Template API on both sides, audio controls, missing-media feedback during HTTP and transport failures, HTML typing with both autofocus settings, and numpad input.
- Focused JVM and Engine verification is pending. The full suite belongs to the combined Core Loop verification.
