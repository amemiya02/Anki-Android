# 48: Finish the single-activity structure and remove leftover XML

**What to build:** Moving between any two screens in the Fork feels the same, and the code that drew the old interface is gone.

**Blocked by:** 40 (Note Types and fields); 41 (Card Template editor and its previewer); 42 (Sync setup around a self-hosted server); 43 (First run for a Newcomer); 44 (Home-screen widgets with Glance); 45 (Notifications); 46 (Harmonisation of the Anki Pages); 47 (Remaining small surfaces)

**Status:** ready-for-agent

- [ ] Every rebuilt screen is reached through Compose navigation in one activity.
- [ ] The system back gesture and screen transitions behave the same everywhere.
- [ ] XML layouts and theme pieces that no surface uses are deleted; what the old study screen needs stays.
- [ ] Opening the app from a link, a shared file, a widget or a notification still lands in the right place.
- [ ] Screen-logic tests and Engine tests still pass unchanged.
