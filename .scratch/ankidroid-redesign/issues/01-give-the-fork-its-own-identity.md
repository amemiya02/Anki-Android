# 01: Give the Fork its own identity

**What to build:** The Fork installs beside the Official App as a separate app with its own data, under the working name "AnkiDroid Redesign", for every build type. Opening one never opens or disturbs the other.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] A debug build and a release build both install beside the Official App without replacing it.
- [ ] The launcher shows the working name "AnkiDroid Redesign" and an icon that cannot be mistaken for the Official App's.
- [ ] The Fork's public API authority and permission no longer collide with the Official App's.
- [ ] Sharing text into the Fork, its widgets and its file handling all resolve to the Fork, not the Official App.
- [ ] Screen-logic tests and Engine tests still pass unchanged.
