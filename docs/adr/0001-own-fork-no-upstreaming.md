---
status: accepted
---

# The Redesign lives in its own fork and is never offered upstream

The Redesign changes the look, navigation and task flows of the whole app at once, and it is built with AI assistance. Upstream AnkiDroid accepts only small single-purpose changes reviewed by two maintainers, and its AI policy bars AI-assisted work from contributors with fewer than three merged pull requests. We therefore build the Redesign in a personal fork with its own name, icon and app ID, and do not shape any of it to be mergeable upstream.

## Considered Options

- **Contribute upstream in small pieces.** Rejected: a whole-app Redesign cannot be split into changes upstream would take one at a time, and the AI policy rules the work out regardless.
- **Fork, but keep each piece upstreamable.** Rejected: it would bind every design decision to upstream's conventions (five themes, Compose only for new screens, its in-flight navigation work) for a benefit the AI policy removes.

## Consequences

- Upstream changes arrive only by merging at milestone boundaries; the fork stays pinned to a known upstream commit in between, and conflicts in redesigned screens are ours to resolve.
- The fork installs beside the official app with its own data, so Decks reach it through AnkiWeb sync or an import, not by sharing storage.
