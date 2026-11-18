# 37: Settings: studying and controls

**What to build:** Every setting that shapes studying is in the redesigned settings.

**Blocked by:** 36 (Settings: home, search and appearance)

**Status:** implemented, device gesture verification in progress

- [ ] Every study, gesture, key binding, controller and toolbar setting that exists today is present and works.
- [x] The drag-and-drop toolbar customisation works.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.

The Studying route includes all 16 previous settings. Controls retains the three general controls, 52 study-screen actions, and 14 Previewer actions. The toolbar uses the existing stored action lists. Focused tests pass for cross-list dragging, moving an action without dragging, search by a saved binding, assigning keys to card sides, Start of next day, and dependent switches.

All 48 rendered cases passed their accessibility checks and produced 96 captures. The 45 focused cases passed. The unchanged Engine passed 143 cases with seven existing skips. On the Android 15 emulator, F8 and the positive X extremity of a virtual USB gamepad were each bound to Previewer Next, read back, and removed. The gesture binding device check remains open.
