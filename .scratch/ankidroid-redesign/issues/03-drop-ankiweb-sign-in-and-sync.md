# 03: Drop AnkiWeb sign-in and sync

**What to build:** The Fork can no longer sign in to or sync with AnkiWeb. Sync setup asks for the owner's own server address first, and without one there is no way to sign in. Browsing and downloading shared Decks from AnkiWeb's website still works.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] With no server address set, the sync button on the deck list leads to sync setup, not to a sign-in form.
- [x] Sign-in is only possible after a server address has been entered, and it signs in to that server.
- [x] The first-run screen no longer offers to sync from AnkiWeb.
- [x] Links to AnkiWeb account sign-up and password reset are gone from the app.
- [ ] Syncing against a self-hosted sync server works end to end: sign in, upload, download.
- [x] Shared Decks can still be browsed and downloaded.
- [x] A rendered-screen test covers sync setup with and without a server address.
- [x] Screen-logic tests and Engine tests still pass unchanged.
- [x] Exporting a collection or a Deck still works and is reachable, so data can leave the Fork and reach AnkiWeb through the Official App.
- [x] The first-run screen offers to import a collection or Deck package in the place where it used to offer AnkiWeb sync.
- [x] Where a person would look for AnkiWeb sign-in, the app explains how to bring Decks in from AnkiWeb: sync into the Official App or desktop Anki, export a package there, and import it here.

## Comments

**Implemented** on `ticket/03`. The gate is in the interface (`Sync.kt`), not the Engine: `syncServerAddress()` is the address from sync setup (the custom sync URL with its switch on) or nothing, and `getEndpoint()`, `syncAuth()` and `isLoggedIn()` give the Engine credentials only with that address, because the Engine falls back to AnkiWeb when it gets no endpoint. `LoginViewModel` refuses to sign in without an address and signs in to that address. Sync setup is the former "Custom sync server" screen, now "Sync server": an explanation (own server; how to bring decks from AnkiWeb through AnkiDroid or desktop Anki, export and import), Sync URL, certificate, and a "Sync account" row (the existing `menu_my_account` string) that is enabled only once an address is set. `AccountActivity.getIntent` opens sync setup while no address is set, so the deck list's sync button, Settings > Sync > Sync account and the sign-in dialogs all lead there. The first-run screen's second button is "Import a collection or deck", which opens the deck list's .apkg/.colpkg picker. New text is in `common/android/src/main/res/values{,-zh-rCN,-ja}/21-fork.xml`.

**Also removed**, beyond sign-up and password reset: "Forgot email?", the signed-in screen's "Remove account" (it opened AnkiWeb's account deletion page; `RemoveAccountFragment` is deleted), and the sign-up button of the shared decks rate-limit dialog. Logging in on AnkiWeb's website from shared decks stays, as browser access.

**Deferred to the device check (ticket 13):** "Syncing against a self-hosted sync server works end to end". Covered so far: `LoginViewModelTest` shows sign-in is sent to the configured server; `LoginFragmentTest` signs in with the real Engine against a local HTTP server set as the sync server; `NoAnkiWebSyncTest` shows the credentials for sync, upload and download carry that address and that none exist without it. The sync code paths themselves (normal sync, full upload, full download, media) are unchanged and their existing tests pass. Sign-in, upload and download against a real sync server on a device are still to do. Exporting is untouched (`ExportDialogFragmentTest` passes and the deck list menu still has "Export collection"); the round trip with the Official App is part of the same device check.

**Tests:** new `NoAnkiWebSyncTest`, `SyncSetupTest`, `SyncSetupScreenshotTest` (sync setup with and without an address, in English, Simplified Chinese and Japanese), `DeckPickerImportOnStartTest`; new cases in `LoginViewModelTest`, `IntroductionActivityTest`, `MyAccountTest`. Screen-logic tests did not change in what they check, but those whose precondition is "signed in" now sign in to a test server (`signInToSyncServer()` in `testutils/SyncServer.kt`) instead of setting only an hkey, since an hkey alone no longer counts as signed in: `SyncTest`, `SyncConflictTest`, `IntentSyncTest`, `DeckPickerAutomaticSyncTest`, `DeckPickerLoginSyncStateTest`, `DeckPickerMeteredConfirmationTest`, `ForegroundMediaNetworkTest`, `AutomaticSyncActionTest`, `SwitchProfilesIsolationReviewTest`, `LoginFragmentTest`, `MyAccountTest`, `AccountInsetsTest`, `AccountActivityScreenshotTest`. Tests of removed features were deleted (the five "remove account" inset tests and its screenshot) or replaced (shared decks sign-up now asserts there is none; the first-run sync tests now cover import). The full `:AnkiDroid:testPlayDebugUnitTest` suite ran before the rebase onto ticket 07: 4440 tests, 3 failures (`CardBrowserDeckSelectionTest`, `DeckPickerBackgroundTest`, `StandardSearchFragmentTest`), all three timing-based and passing when rerun alone. After the rebase the touched tests pass again, and the sync setup, first-run and account screenshots record in Light, Dark and Black. `:libanki:test` is unchanged and passes, and `:AnkiDroid:lintPlayRelease` passes.

**Left in place:** stale translations of removed strings in `values-*/`; the username field's "Email address" hint; the AnkiWeb privacy policy and terms in Help, which still apply to shared decks. The screen is not restyled.

