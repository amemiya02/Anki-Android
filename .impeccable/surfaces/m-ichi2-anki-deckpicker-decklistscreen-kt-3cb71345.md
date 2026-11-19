---
version: 1
slug: "m-ichi2-anki-deckpicker-decklistscreen-kt-3cb71345"
primary_target: "AnkiDroid/src/main/java/com/ichi2/anki/deckpicker/DeckListScreen.kt"
related_targets: ["AnkiDroid/src/main/java/com/ichi2/anki/ui/windows/reviewer/AnswerArea.kt","AnkiDroid/src/main/java/com/ichi2/anki/study/FinishedScreen.kt"]
---

# Deck list (and the Core Loop it opens)

Mode: Operate. The first surface of the Material 3 redesign; the study screen and the finished screen share its world.

- Audience: the owner, a Daily Reviewer studying Chinese, Japanese and English Decks on a phone most days.
- Job: see what is due today, start a Deck with one tap, answer fast, finish, come back tomorrow.
- Constraints: every Deck action and count stays; Anki's words verbatim; Card Content untouched; Light, Dark and Black from a fixed palette; WCAG AA, 48dp targets, RTL, 200% font.
- Craft bar: Google Calendar, Keep and Tasks (owner's choice, 2026-11-19).
- Owner's "feels wrong" list: gamification, a generic template look, few Decks per screen, motion that slows answering.

## Direction contract

THESIS: Hayaki becomes a first-class Material 3 app finished to the level of Google Calendar, Keep and Tasks. It refuses both the paper-and-serif costume of Quiet paper and AnkiDroid's flat table of numbers: tonal surfaces group the work, the type scale carries hierarchy, and every control is the stock Material 3 one a fluent Android user already trusts.

OWN-WORLD: A fixed Material 3 scheme seeded from #6750A4 in Light, Dark and Black (true-black surfaces), never wallpaper colour. Tonal surface containers instead of hairlines: surfaceContainerLow groups on the surface, primaryContainer for today's summary. Corners 8/12/16/28dp, pill buttons, a 16dp FAB. Roboto with the system's Noto CJK, Material Symbols Outlined. New, learning and due keep Anki's blue, red and green, set as tinted count pills.

STORY: The reviewer opens Decks and reads today's remaining cards and progress in one glance, scans compact grouped Decks with their counts, taps one to study, answers with a row of four pill buttons that show each interval, lands on a finished screen that says when to come back, and returns to Decks with the progress ring already moved.

FIRST VIEWPORT: Top bar with today's date over "Decks" (headline) and sync and overflow icon buttons. Under it a primaryContainer summary card, 28dp corners: a determinate circular progress of studied over studied plus remaining, "N cards to study today" in titleLarge and "N studied so far" beside it. Then quiet column headings and the Deck tree: each top-level Deck and its open sub-Decks in one surfaceContainerLow group with 16dp corners and 8dp between groups, 56dp top-level rows and 48dp nested rows, counts as three aligned tinted pills. The add-Note FAB sits bottom-right in primaryContainer above the navigation bar.

FORM: The category standard (the standing exit of the direction round), played straight at full craft; not on the grounded list. seed:61442a8fe0caa2e9. Signature interaction: coming back from a session, the summary ring animates from the old progress to the new one (emphasized decelerate, 400ms). Motion grammar: Material 3 standard easing throughout; expand chevrons rotate and sub-Decks animate into place in 200ms; card and answer transitions never exceed 120ms; the system's remove-animations setting cuts every motion to instant.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## Unresolved

- Browse, Note editor, Settings and dialogs inherit the new Foundation in this pass; their own Material 3 restructuring follows in later Milestones.
