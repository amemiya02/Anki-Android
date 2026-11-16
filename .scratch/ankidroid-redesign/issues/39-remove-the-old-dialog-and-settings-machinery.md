# 39: Remove the old dialog and settings machinery

**What to build:** Nothing in the app can show an old-style dialog or settings screen, because the code that drew them is gone.

**Blocked by:** 34 (Dialogs: study screen and card browser); 35 (Dialogs: note editor, importing, errors and the rest); 37 (Settings: studying and controls); 38 (Settings: everything else)

**Status:** ready-for-agent

- [ ] The old dialog and settings building blocks are deleted.
- [ ] The app builds and every test passes without them.
- [ ] No screenshot test still captures an old-style dialog or settings screen.
