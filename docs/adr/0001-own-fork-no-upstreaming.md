---
status: accepted
---

# The Redesign lives in its own fork and is never offered upstream

The Redesign changes the look, navigation and task flows of the whole app at once, and it is built with AI assistance. Upstream AnkiDroid accepts only small single-purpose changes reviewed by two maintainers, and its AI policy bars AI-assisted work from contributors with fewer than three merged pull requests. We therefore build the Redesign in a personal fork with its own name, icon and app ID, and do not shape any of it to be mergeable upstream.

## Considered Options

- **Contribute upstream in small pieces.** Rejected: a whole-app Redesign cannot be split into changes upstream would take one at a time, and the AI policy rules the work out regardless.
- **Fork, but keep each piece upstreamable.** Rejected: it would bind every design decision to upstream's conventions (five themes, Compose only for new screens, its in-flight navigation work) for a benefit the AI policy removes.

## Consequences

- The Fork follows Upstream for the Engine only. Upstream's interface changes are ignored; Engine updates are taken whenever sync requires it or a few times a year, because an Engine frozen in time would eventually stop syncing with AnkiWeb and newer desktop Anki. (This replaces the first plan, which merged all of Upstream at Milestone boundaries; see ADR-0003.)
- Other people may use the Fork, so it must be clearly not AnkiDroid: its own name without "AnkiDroid" in it, its own launcher icon, and an about screen that says it is an unofficial fork and links to its source. How other people get it is undecided.
- The Fork installs beside the Official App and keeps its own collection. A user can also move a Deck or collection between the two apps by exporting and importing a package.

## Sync decision revised on 2026-11-19

We chose to restore AnkiWeb sign-in and sync through the upstream Engine and retain self-hosted sync. The earlier self-hosted-only decision relied on AnkiWeb's list of approved clients. When asked about a separately distributed AnkiDroid fork, Anki's author said that using the original sync code should be fine if the fork causes no problems for the service. He also recommended a distinct name. Hayaki retains its own identity. His reply concerned another fork and gives the rationale for this decision. [AnkiWeb access terms](https://ankiweb.net/account/terms#access) · [Author's reply](https://forums.ankiweb.net/t/would-it-be-bad-to-upload-an-ankidroid-fork-to-the-play-store-for-free/67022/20)

- A new profile's Sync action opens AnkiWeb sign-in and offers a self-hosted server. Existing self-hosted profiles keep their server and login on upgrade. Signing in from Welcome or the Decks Sync action continues the requested sync. Signing in from Settings returns to Settings. File import remains available from Welcome.
- A signed-in user confirms a change of sync server for the current profile. The local collection and review history stay on the device. Credentials, custom certificate trust and pending work for the old server are isolated from the new one. Replacing either collection requires a separate decision whenever the Engine reports a conflict.
- The app shows collection and media sync progress separately. WorkManager retries interrupted media transfers with backoff up to five times and enforces the profile's network settings. Cancelling media prevents automatic transfers across app restarts until the next manual sync. Card sync can continue during this pause. Authentication errors and collection conflicts require user action. The Engine continues to own the sync protocol and merge rules.

Kioku runs desktop Anki on the phone. Its mobile wrapper keeps transfers alive and retries interrupted media separately from collection sync. Hayaki adopts those behaviors with Android WorkManager and the profile's existing network settings. Kioku can also host Anki's sync server on the phone. Hayaki can connect through its self-hosted option. Running a server inside Hayaki would be a separate product decision. [Kioku media handling](https://github.com/krmanik/kioku/blob/7f5fe5b5d1b13724bccfb5dc0fa8443111e63d1e/shared/python/kioku/ankiwebsync.py) · [Kioku server implementation](https://github.com/krmanik/kioku/blob/7f5fe5b5d1b13724bccfb5dc0fa8443111e63d1e/shared/python/kioku/syncserver.py)
