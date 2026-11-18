# 44: Sync setup around a self-hosted server

**What to build:** The owner sets up, checks and uses sync with their own server from one redesigned place.

**Blocked by:** 41 (Mockups for the rest)

**Status:** ready-for-agent

- [x] Sync setup asks for the server address, then for sign-in to that server.
- [x] It shows whether the Fork is signed in, and to which server, and lets the owner sign out.
- [x] Sync errors say what went wrong in plain words, as worded in `docs/redesign/the-rest/README.md`; the conflict dialog names this phone and the server, not "Hayaki" and "AnkiWeb".
- [x] One Sync page holds the account and the settings now in Settings › Sync; Settings › Sync becomes a row that opens it (per the Mockups, decision 2).
- [x] Sign-in asks for a username, and the logo and AnkiWeb privacy button are gone (per the Mockups, decision 6).
- [x] There is still no way to sign in to AnkiWeb.
- [x] It matches the agreed Mockup.
- [ ] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [ ] Screen-logic tests and Engine tests still pass unchanged.


## Implementation

- The server address and optional root certificate precede username and password. Settings opens one Sync page with account status, Sync now, Sign out, and all sync settings. Decks receives the sign-in result from either setup step.
- The account page reports successful collection syncs separately from the timer for automatic sync attempts. Changing or removing the server clears credentials for the previous server.
- The conflict and sanity-check dialogs name both collections and say what each choice replaces. The Engine response carries no collection change timestamps, so the figures in the Mockup are illustrative.

## Verification

The original setup failed the rendered regression at the missing first step. On the emulator, username sign-in triggered a full upload to the disposable self-hosted server. A separate desktop Engine downloaded all 43 fixture Cards and 43 Notes. The account page showed the account, server, successful sync time and retained settings. Removing the emulator's connection produced the plain-language error with the server address. Retry reached the restored server, and Sign out kept the server address and collection.
