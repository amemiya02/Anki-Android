# 02: Switch off crash reports and usage statistics

**What to build:** The Fork never sends a crash report to Upstream's server and never sends usage statistics anywhere. The settings that controlled them are gone, and nothing asks the owner to opt in.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] A forced crash in a debug build sends nothing to Upstream's crash-report server.
- [x] No usage-statistics event is sent on launch or during a study session.
- [x] The opt-in prompt no longer appears, and the related settings entries are removed.
- [x] The app still shows its own error dialog when something fails.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Comments

**Implemented** on `ticket/02`: ACRA and the Google Analytics client are removed from the build, with the report dialog, report collectors, throwable filter, previous-ANR reporter, opt-in prompt, analytics exception handler and settings-change reporting. Removed settings entries: "Error reporting mode" and "Share feature usage" (General), "Switch Analytics to dev mode" (Developer options). The "Send troubleshooting report" help item is gone. `CrashReportService` remains and only writes reports to the log; `Analytics` in `:common` remains as a no-op so Upstream's call sites compile.

**Deferred to the device check (ticket 13):** the first two criteria. `NoReportingTest` shows that no crash report sender (ACRA) or usage-statistics client (Google Analytics) is on the app's classpath and that `BuildConfig` has no crash report server address, so nothing in the app can send either. Watching a forced crash ("Trigger test crash" in Developer options) and a launch plus study session on a device, with network traffic observed, is still to do.

**Tests:** `NoReportingTest` also covers the removed settings entries, no prompt on upgrade and the app's error dialog for a failing task; `HelpDialogTest` covers the removed help item. Tests that existed only for the removed reporting code (ACRA, analytics, ANR reporting, throwable filter) were deleted. The full `:AnkiDroid:testPlayDebugUnitTest` suite (4424 tests) and `:libanki:test` pass; all other tests ran unchanged, apart from `DeckPickerTest`, which now checks "no crash report" with a recording reporter instead of a flag ACRA used to set.

**Left in place:** stale translations of the removed strings in `values-*/` (release lint ignores `ExtraTranslation`); comments elsewhere that still mention ACRA; the "Report a bug" and "Send feedback" help items, which link to Upstream's pages and are not crash reports.

**Device check (ticket 13):** On the Pixel 9 emulator, "Trigger test crash" crashed the debug build. The log showed no ACRA, and the Fork's network counters (`dumpsys netstats`, uid of `com.ichi2.anki.redesign.debug`) did not move in the 20 s that followed. Launching the app and studying on both study screens (showing and answering) also sent nothing. Two things for the owner. First, opening an Anki Page (finished screen, statistics) makes one DNS lookup, tagged as DNS in netstats. WebView DevTools shows these pages load only from `appassets.androidplatform.net`, the local asset host, which does not resolve. Second, once during the walk, about 12 kB in and 7 kB out of non-DNS traffic was counted between leaving the import page and opening Statistics. Three later runs of the same steps did not repeat it, and its destination was not identified: the emulator's packet capture came back empty.

**The unexplained traffic during the walk (investigated by reading the code, without the emulator).** About 7 kB out and 12 kB in, once, between leaving the import Anki Page and opening Statistics. Nothing in the Fork's own code, the Engine or the Anki Pages on that path opens a connection:

- The Fork's network clients are sync and sign-in (only with a sync server set, ticket 03), the shared-deck download (Android's DownloadManager, only on a tap) and the JavaScript add-on download (`HttpFetcher`, only on demand). None runs on that path. ACRA and analytics are gone (`NoReportingTest`). No image loader, Firebase or Play services library is in the build.
- The Engine opens connections only to sync, which needs a server.
- The Anki Pages on the path (the import page and `graphs`) reference no outside address. The bundled pages only point outside in the note editor's MathJax speech maps (`cdn.jsdelivr.net`), which is not on this path. The pages load from `appassets.androidplatform.net`, which `PageWebViewClient` serves from the app's assets; a request it does not answer (for example the pages' `<link rel="icon" href="/favicon.ico">`, as only `/favicon.png` is answered) goes to that host. The host has no DNS record (checked: no A or AAAA record), so such a request ends at the DNS lookup. That is the likely source of the one DNS lookup per process seen during the walk.
- WebView metrics are switched off in the manifest (`android.webkit.WebView.MetricsOptOut`).

**Most likely cause: Android System WebView itself**, which is benign for this ticket. WebView runs Chromium's network stack inside the app's process, so whatever WebView fetches on its own is counted under the Fork's uid, not WebView's. The traffic came once, as an Anki Page's WebView started, and did not come back in three repeats of the same steps, which fits a one-off WebView task rather than anything the Fork's code does on that path. Which WebView feature it was cannot be said without a packet capture, and the emulator's capture came back empty. Nothing was changed.

**Noticed on the way, for the owner:** every screen binds the browser's Custom Tabs service and calls `warmup()` when it starts (`AnkiActivity.onStart`, `CustomTabActivityHelper`). That starts the browser in the background, and whatever the browser then fetches is counted under the browser's uid, not the Fork's. It sends nothing about the Fork, but the owner may prefer not to wake the browser until a link is opened.
