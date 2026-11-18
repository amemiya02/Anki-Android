# 44: Sync setup around a self-hosted server

**What to build:** The owner sets up, checks and uses sync with their own server from one redesigned place.

**Blocked by:** 41 (Mockups for the rest)

**Status:** ready-for-agent

- [ ] Sync setup asks for the server address, then for sign-in to that server.
- [ ] It shows whether the Fork is signed in, and to which server, and lets the owner sign out.
- [ ] Sync errors say what went wrong in plain words, as worded in `docs/redesign/the-rest/README.md`; the conflict dialog names this phone and the server, not "Hayaki" and "AnkiWeb".
- [ ] One Sync page holds the account and the settings now in Settings › Sync; Settings › Sync becomes a row that opens it (per the Mockups, decision 2).
- [ ] Sign-in asks for a username, and the logo and AnkiWeb privacy button are gone (per the Mockups, decision 6).
- [ ] There is still no way to sign in to AnkiWeb.
- [ ] It matches the agreed Mockup.
- [ ] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [ ] Screen-logic tests and Engine tests still pass unchanged.
