# 07: Foundation: colours, shape and the three themes

**What to build:** The whole app takes on the chosen Direction's colours and shapes at once, in Light, Dark or Black, including screens that have not been rebuilt yet. The Plain and E-ink themes are gone.

**Blocked by:** 04 (Extend the rendered-screen test harness to languages and font sizes); 06 (Complete the chosen Direction: Dark, Black, name and launcher icon)

**Status:** ready-for-agent

- [ ] Colour roles, spacing, corner shapes and elevation are defined once and read by both Compose surfaces and the remaining XML screens.
- [ ] Theme settings offer Light, Dark and Black, and the app follows the system's light or dark setting by default.
- [ ] The Plain and E-ink themes and their settings entries are removed; someone who had one selected lands on Light.
- [ ] Wallpaper-derived colour is not used anywhere.
- [ ] The rendered-screen harness runs Light, Dark and Black only.
- [ ] The deck list, the newer study screen and one dialog are captured in all three themes as proof.
- [ ] Screen-logic tests and Engine tests still pass unchanged.
