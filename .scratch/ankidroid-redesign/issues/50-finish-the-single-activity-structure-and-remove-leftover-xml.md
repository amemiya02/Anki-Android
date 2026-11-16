# 50: Finish the single-activity structure and remove leftover XML

**What to build:** Moving between any two screens in the Fork feels the same, and the code that drew the old interface is gone.

**Blocked by:** 42 (Note Types and fields); 43 (Card Template editor and its previewer); 44 (Sync setup around a self-hosted server); 45 (First run for a Newcomer); 46 (Home-screen widgets with Glance); 47 (Notifications); 48 (Harmonisation of the Anki Pages); 49 (Remaining small surfaces)

**Status:** ready-for-agent

- [ ] Every rebuilt screen is reached through Compose navigation in one activity.
- [ ] The system back gesture and screen transitions behave the same everywhere.
- [ ] The XML layouts and theme pieces of the old interface are deleted; no surface uses them.
- [ ] Opening the app from a link, a shared file, a widget or a notification still lands in the right place.
- [ ] Screen-logic tests and Engine tests still pass unchanged.
