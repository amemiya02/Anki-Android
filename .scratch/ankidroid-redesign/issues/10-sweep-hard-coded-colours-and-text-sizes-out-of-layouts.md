# 10: Sweep hard-coded colours and text sizes out of layouts

**What to build:** No screen that has not been rebuilt yet shows a leftover colour or text size from the old look. This is a mechanical sweep, done in batches so the app stays green between them.

**Blocked by:** 07 (Foundation: colours, shape and the three themes); 08 (Foundation: bundled typefaces and the type scale)

**Status:** ready-for-agent

- [ ] No layout sets a colour directly; each uses a Foundation colour role.
- [ ] No layout sets a fixed text size; each uses a Foundation type style.
- [ ] A check fails the build if a layout hard-codes a colour or a text size again.
- [ ] Every screen with an existing screenshot test is recaptured, and no picture shows the old blue or an unreadable pairing in any of the three themes.
- [ ] Screen-logic tests and Engine tests still pass unchanged.
