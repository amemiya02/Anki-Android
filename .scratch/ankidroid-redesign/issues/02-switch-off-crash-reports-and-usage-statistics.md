# 02: Switch off crash reports and usage statistics

**What to build:** The Fork never sends a crash report to Upstream's server and never sends usage statistics anywhere. The settings that controlled them are gone, and nothing asks the owner to opt in.

**Blocked by:** None (can start immediately)

**Status:** done

- [ ] A forced crash in a debug build sends nothing to Upstream's crash-report server.
- [ ] No usage-statistics event is sent on launch or during a study session.
- [x] The opt-in prompt no longer appears, and the related settings entries are removed.
- [x] The app still shows its own error dialog when something fails.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Comments

**Implemented** on `ticket/02`: ACRA and the Google Analytics client are removed from the build, with the report dialog, report collectors, throwable filter, previous-ANR reporter, opt-in prompt, analytics exception handler and settings-change reporting. Removed settings entries: "Error reporting mode" and "Share feature usage" (General), "Switch Analytics to dev mode" (Developer options). The "Send troubleshooting report" help item is gone. `CrashReportService` remains and only writes reports to the log; `Analytics` in `:common` remains as a no-op so Upstream's call sites compile.

**Deferred to the device check (ticket 13):** the first two criteria. `NoReportingTest` shows that no crash report sender (ACRA) or usage-statistics client (Google Analytics) is on the app's classpath and that `BuildConfig` has no crash report server address, so nothing in the app can send either. Watching a forced crash ("Trigger test crash" in Developer options) and a launch plus study session on a device, with network traffic observed, is still to do.

**Tests:** `NoReportingTest` also covers the removed settings entries, no prompt on upgrade and the app's error dialog for a failing task; `HelpDialogTest` covers the removed help item. Tests that existed only for the removed reporting code (ACRA, analytics, ANR reporting, throwable filter) were deleted. The full `:AnkiDroid:testPlayDebugUnitTest` suite (4424 tests) and `:libanki:test` pass; all other tests ran unchanged, apart from `DeckPickerTest`, which now checks "no crash report" with a recording reporter instead of a flag ACRA used to set.

**Left in place:** stale translations of the removed strings in `values-*/` (release lint ignores `ExtraTranslation`); comments elsewhere that still mention ACRA; the "Report a bug" and "Send feedback" help items, which link to Upstream's pages and are not crash reports.
