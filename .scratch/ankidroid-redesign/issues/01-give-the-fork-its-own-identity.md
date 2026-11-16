# 01: Give the Fork its own identity

**What to build:** The Fork installs beside the Official App as a separate app with its own data, under the working name "AnkiDroid Redesign", for every build type. Opening one never opens or disturbs the other.

**Blocked by:** None (can start immediately)

**Status:** done

- [ ] A debug build and a release build both install beside the Official App without replacing it.
- [x] The launcher shows the working name "AnkiDroid Redesign" and an icon that cannot be mistaken for the Official App's.
- [x] The Fork's public API authority and permission no longer collide with the Official App's.
- [x] Sharing text into the Fork, its widgets and its file handling all resolve to the Fork, not the Official App.
- [x] Screen-logic tests and Engine tests still pass unchanged.
- [x] Opening a collection or Deck package from a file manager or another app offers the Fork as a place to open it.
- [ ] The working name appears only in development builds and is never the name another person sees.

## Comments

Done on `ticket/01`.

- Application IDs: release `com.ichi2.anki.redesign`, debug `com.ichi2.anki.redesign.debug` (Upstream's debug ID `com.ichi2.anki.debug` is no longer reused). Set once in `buildSrc/.../ForkIdentity.kt`, read by `:AnkiDroid` and `:api`.
- The API's default authority and permission, and the permission the API module's manifest requests, now follow the Fork's ID. Before this, the Fork requested the Official App's `com.ichi2.anki.permission.READ_WRITE_DATABASE`.
- Launcher icon: a placeholder cream "R" on the Quiet paper primary colour (adaptive, monochrome and legacy PNG). Ticket 09 replaces it with the Hayaki icon.
- `ForkIdentityTest` covers the application ID, launcher name, API authority and permission, and that shared text, `.apkg`/`.colpkg` files and the widgets resolve to the Fork. The two `:api` tests that asserted the Official App's package now assert the Fork's.
- Verified on assembled `playDebug` and `playRelease` APKs with `aapt2`: package, label "AnkiDroid Redesign", authorities and permissions carry the Fork's ID, and no Official App identifier remains in the manifest. Full `testPlayDebugUnitTest` (4488 tests), `:libanki:test` and `:api:testDebugUnitTest` pass; no screen-logic or Engine test was changed.

Deferred:

- "Installs beside the Official App without replacing it" needs a device: checked in ticket 13. The distinct application IDs are verified in both build types.
- The launcher name and icon, sharing and file handling are verified from the build; seeing them on a launcher and in a share sheet or file manager is part of ticket 13.
- "The working name appears only in development builds" is met for now only because no build is given to anyone: release builds also show "AnkiDroid Redesign" until ticket 09 applies the name Hayaki (`ForkIdentity.WORKING_NAME` / the `app_name` resValue).
