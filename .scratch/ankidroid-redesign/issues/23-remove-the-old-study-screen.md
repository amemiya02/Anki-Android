# 23: Remove the old study screen

**What to build:** There is one study screen. The old study screen, the setting that chose between the two, and the settings only the old one used are gone, with nothing lost that the owner chose to keep.

**Blocked by:** 20 (Study screen: tools around the Card); 22 (JavaScript API in the study screen: Card actions, speech and the rest)

**Status:** ready-for-agent

- [ ] Every feature only the old study screen had is either working in the study screen or was marked "drop" in the Core Loop Mockup ticket.
- [ ] Nothing in the app can open the old study screen: not a setting, a link, a widget, a notification or another app.
- [ ] The setting that chose between the two study screens is gone, and someone who had the old one selected lands in the study screen.
- [ ] Settings only the old study screen used are removed; settings both used still work.
- [ ] The old study screen's code, layouts and tests are deleted, and tests of behaviour both screens shared now run against the study screen.
- [ ] The app builds and every remaining test passes.
- [ ] Screen-logic tests and Engine tests still pass unchanged.
