# 37: Remove the old dialog and settings machinery

**What to build:** Nothing in the app can show an old-style dialog or settings screen, because the code that drew them is gone.

**Blocked by:** 32 (Dialogs: study screen and card browser); 33 (Dialogs: note editor, importing, errors and the rest); 35 (Settings: study screens and controls); 36 (Settings: everything else)

**Status:** ready-for-agent

- [ ] The old dialog and settings building blocks are deleted.
- [ ] The app builds and every test passes without them.
- [ ] No screenshot test still captures an old-style dialog or settings screen.
