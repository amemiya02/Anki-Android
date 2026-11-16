# 02: Switch off crash reports and usage statistics

**What to build:** The Fork never sends a crash report to Upstream's server and never sends usage statistics anywhere. The settings that controlled them are gone, and nothing asks the owner to opt in.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] A forced crash in a debug build sends nothing to Upstream's crash-report server.
- [ ] No usage-statistics event is sent on launch or during a study session.
- [ ] The opt-in prompt no longer appears, and the related settings entries are removed.
- [ ] The app still shows its own error dialog when something fails.
- [ ] Screen-logic tests and Engine tests still pass unchanged.
