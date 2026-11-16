---
status: accepted
---

# Only the newer study screen is redesigned; the old one stays as an unstyled fallback

Upstream ships two study screens: the old default and a newer opt-in one. The Redesign covers every Native Surface, and no feature may be removed, but the study screen is the most complex screen in the app and redesigning it twice buys nothing. We redesign only the newer screen and make it the Fork's default; the old screen keeps its current look and stays reachable from settings.

## Consequences

- The old screen is a necessary fallback, not a courtesy. The newer screen has no support for the AnkiDroid JavaScript API, cannot save a whiteboard drawing, and lacks the legacy read-the-whole-card-aloud speech. When a Card needs the missing JavaScript API, the newer screen offers a one-tap switch to the old one.
- The Fork does not close the newer screen's functional gaps as a goal. Deck title, time-remaining estimate and answer-button position are decided when the study screen's Mockup is drawn.
- Settings must keep serving both screens, since each hides the other's options.
