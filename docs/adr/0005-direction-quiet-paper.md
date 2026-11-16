---
status: accepted
---

# The Redesign's Direction is Quiet paper

Three Directions were drawn as Mockups of the deck list and the study screen, in English and Simplified Chinese: Quiet paper, Crisp tool, and Soft and friendly. The owner compared them by eye and picked Quiet paper: warm paper backgrounds, ink-dark text, hairline rules, small corners, and a single cinnabar accent, set in a serif Latin face with brush-style Chinese and Japanese faces.

## Considered Options

- **Crisp tool** (cool neutrals, IBM Plex, dense rows, one vivid indigo). The densest and the most conventional; rejected as less distinctive and less calm over long sessions.
- **Soft and friendly** (lavender, rounded cards, coloured pills, Nunito). The most approachable; rejected because it shows the fewest Decks per screen, leaves the least room for long Deck names, and has no openly licensed rounded Chinese face to match its Latin and Japanese ones.

## Consequences

- The Foundation is built from Quiet paper's design values, recorded in `docs/redesign/quiet-paper.values.json`.
- The typefaces are Newsreader (Latin), LXGW WenKai (Chinese) and Klee One (Japanese), all under the SIL Open Font License. LXGW WenKai is derived from Klee One, so the Chinese and Japanese faces are siblings.
- The interface is serif and brush-style throughout. Card Content is unaffected and keeps whatever its Card Template sets.
- Colour is used sparingly: cinnabar for the one primary action and for emphasis, and three quiet hues for the new, learning and due counts.
- Shapes are restrained: hairlines instead of filled cards, small corner radii, almost no shadow. Material 3 components need their default tonal surfaces, large radii and elevation turned down to match.
- The other two Directions remain in the Mockup project for reference only.
