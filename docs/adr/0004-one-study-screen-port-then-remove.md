---
status: accepted
---

# There is one study screen: the newer one gains what it lacks, then the old one is removed

Upstream ships two study screens. ADR-0002 redesigned only the newer one and kept the old one as an unstyled fallback, because the newer one lacks the AnkiDroid JavaScript API and a few smaller features. That left one old-look screen in the app for good, which contradicts redesigning all of it, and it matters more now that other people will use the Fork. We instead add the JavaScript API to the newer study screen, decide each smaller gap on purpose, and then delete the old study screen and the settings only it used.

This supersedes ADR-0002.

## Considered Options

- **Keep the old study screen as a fallback** (ADR-0002). Rejected: an old-look screen stays in the app indefinitely.
- **Delete the old study screen straight away.** Rejected: Cards whose Card Templates use the AnkiDroid JavaScript API would stop working, and shared Decks other people rely on use it.

## Consequences

- Implementing the JavaScript API in the newer study screen is non-UI work taken on deliberately. It is the one place the Redesign closes a functional gap as a goal.
- Each feature only the old study screen has (saving a whiteboard drawing, reading the whole Card aloud, answer-button position, minimum press time, double-tap timeout) is marked "build" or "drop" by the owner when the study screen's Mockup is drawn. A "drop" is a deliberate exception to "no existing feature is removed".
- Until the removal, the old study screen stays selectable in settings and is not redesigned.
- After the removal, "the study screen" needs no qualifier, and settings no longer serve two screens.
