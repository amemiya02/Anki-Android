# 37: Settings: studying and controls

**What to build:** Every setting that shapes studying is in the redesigned settings.

**Blocked by:** 36 (Settings: home, search and appearance)

**Status:** ready-for-agent

- [ ] Every study, gesture, key binding, controller and toolbar setting that exists today is present and works.
- [x] The drag-and-drop toolbar customisation works.
- [ ] It matches the agreed Mockup.
- [ ] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [ ] Screen-logic tests and Engine tests still pass unchanged.

The Studying route includes all 16 previous settings. Controls retains the three general controls, 52 study-screen actions, and 14 Previewer actions. The toolbar uses the existing stored action lists. Focused tests pass for cross-list dragging, moving an action without dragging, search by a saved binding, assigning keys to card sides, Start of next day, and dependent switches.

The complete rendered Quality Bar and Engine runs are pending. The gesture surface and joystick capture still need a device check.
