---
status: accepted
---

# The whole interface is rebuilt natively in Jetpack Compose, in place; web technology is used only for Mockups and Anki Pages

The Redesign was asked to use a modern stack, and two web component libraries (shadcn/ui and Meta's astryx) were named as candidates. Neither can draw a native Android screen, so the choice was between rebuilding the interface as web pages inside the app and rebuilding it natively. We rebuild every Native Surface in Jetpack Compose with Material 3, replacing screens one Milestone at a time inside the running app, and move to a single-activity structure with Compose navigation as screens are converted. Web technology (React, Tailwind, shadcn/ui) is used only to draw Mockups, and to apply Harmonisation to the Anki Pages, which are web pages already.

## Considered Options

- **Rebuild the screens as web pages inside the app.** Rejected: nothing existing is reused, widgets, notifications, the stylus whiteboard, voice recording and hardware keys must stay native regardless, and scrolling, the back gesture and text handling would have to be imitated.
- **Restyle the existing XML screens and use Compose only where a screen's structure changes.** This was agreed first, then rejected: its main benefit was cheap merges from Upstream, and the Fork no longer follows Upstream's interface.
- **Start a new app from an empty project on top of the Engine.** Rejected: nothing would be usable until every feature existed again, and no feature may be removed.

## Consequences

- The interface no longer follows Upstream. The Engine still does, and each Engine update may need interface code adjusted by hand.
- Only design values carry over from a Mockup: colour, type, spacing and shape. Components are built natively to match.
- Upstream's tests that inspect a screen stop applying once that screen is rebuilt and are replaced by new ones. Tests of logic and the Engine must keep passing.
- Home-screen widgets cannot be drawn with ordinary Compose and use Glance instead.
- No dependency-injection framework is introduced; it would change nothing visible.
- The old study screen is the one Native Surface left as it is (see ADR-0002).
