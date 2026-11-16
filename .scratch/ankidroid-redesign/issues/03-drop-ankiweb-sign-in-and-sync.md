# 03: Drop AnkiWeb sign-in and sync

**What to build:** The Fork can no longer sign in to or sync with AnkiWeb. Sync setup asks for the owner's own server address first, and without one there is no way to sign in. Browsing and downloading shared Decks from AnkiWeb's website still works.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] With no server address set, the sync button on the deck list leads to sync setup, not to a sign-in form.
- [ ] Sign-in is only possible after a server address has been entered, and it signs in to that server.
- [ ] The first-run screen no longer offers to sync from AnkiWeb.
- [ ] Links to AnkiWeb account sign-up and password reset are gone from the app.
- [ ] Syncing against a self-hosted sync server works end to end: sign in, upload, download.
- [ ] Shared Decks can still be browsed and downloaded.
- [ ] A rendered-screen test covers sync setup with and without a server address.
- [ ] Screen-logic tests and Engine tests still pass unchanged.
