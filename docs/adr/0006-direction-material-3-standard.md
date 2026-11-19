---
status: accepted
---

# The Redesign's Direction is the Material 3 category standard

After the Redesign was signed off, the owner found that Hayaki still read as AnkiDroid with a paper skin: the same table of counts, the same study screen, a serif costume over them. A new direction round dealt a grounded direction (manuscript-grid paper), Impeccable's own pick (railway signage) and six catalogue challengers. The owner chose none of them. They took the standing exit, the category standard: a Material 3 app, finished to the level of Google Calendar, Keep and Tasks.

This supersedes [ADR-0005](0005-direction-quiet-paper.md).

## Considered Options

- **Manuscript grid (the dealt direction).** Today's reviews as a page of squared manuscript paper. Rejected by the owner.
- **Railway signage (Impeccable's pick).** The Deck tree drawn as a line diagram with line colours. Rejected by the owner. It also needed a colour per Deck, which Anki does not store.
- **Keep Quiet paper.** Rejected: the owner asked for a new look and a new structure.

## Consequences

- The Foundation's values come from a fixed Material 3 scheme seeded from #6750A4, in Light, Dark and Black. Wallpaper-derived dynamic colour is still not used: the palette stays fixed (spec story 9).
- Surfaces are tonal again. Groups sit in surface containers instead of between hairlines, and buttons are pills. Elevation follows Material 3.
- Interface text uses the system sans-serif stack: Roboto, with the system's CJK font in the interface language. The bundled Newsreader, LXGW WenKai and Klee One are removed, about 70 MB. Card Content is unaffected and keeps whatever its Card Template sets.
- The count colours keep Anki's convention: blue for new, red for learning, green for due.
- Again, Hard, Good and Easy keep Anki's red, amber, green and blue on their names, set on tonal buttons. The colour tells the four answers apart before the word is read, as Anki's own buttons do. Everything else on the study screen stays in the scheme.
- The Core Loop (deck list, study screen and finished screen) is rebuilt in the new world first. Other surfaces inherit the new Foundation at once and are restructured in later Milestones.
- Hierarchy still uses size and weight only: no italics, capitals or letter-spacing carry meaning.
