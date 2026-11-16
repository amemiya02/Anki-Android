# Core Loop Mockups

Mockups for ticket 14: the bottom bar and More, the deck list, the study screen and the finished screen, in Quiet paper. Each surface is shown in English and Simplified Chinese, and each picture shows Light, Dark and Black side by side. Colours come from `../quiet-paper.values.json`; `node mockups/scripts/check-contrast.mjs` passes for every pairing in it.

Start the Mockups with `npm install && npm run dev` in `mockups/`, then open the URL in the last column. `lang` is `en` or `zh`.

## Pictures

| Picture | Shows | URL |
|---|---|---|
| `overview-en.png`, `overview-zh.png` | Every surface below, in Light | `/?core=overview&lang=en` |
| `more-en.png`, `more-zh.png` | The bottom bar with More current, and the More surface: Settings, Help, Support AnkiDroid | `/?core=more&lang=en` |
| `decks-en.png`, `decks-zh.png` | The deck list with the sample Decks | `/?core=decks&lang=en` |
| `decks-empty-en.png`, `decks-empty-zh.png` | The deck list for a Newcomer with no Decks | `/?core=decks-empty&lang=en` |
| `decks-long-en.png`, `decks-long-zh.png` | The deck list with long Chinese and Japanese Deck names | `/?core=decks-long&lang=en` |
| `question-en.png`, `question-zh.png` | The study screen, question side | `/?core=question&lang=en` |
| `answer-en.png`, `answer-zh.png` | The study screen, answer side | `/?core=answer&lang=en` |
| `finished-later-en.png`, `finished-later-zh.png` | Finished: more Cards later today | `/?core=finished-later&lang=en` |
| `finished-limit-en.png`, `finished-limit-zh.png` | Finished: daily limit reached | `/?core=finished-limit&lang=en` |
| `finished-buried-en.png`, `finished-buried-zh.png` | Finished: Cards buried, with Unbury | `/?core=finished-buried&lang=en` |
| `finished-done-en.png`, `finished-done-zh.png` | Finished: nothing left today | `/?core=finished-done&lang=en` |
| `decide-counts.png` | Decision 1, A beside B, in English and Chinese | `/?decide=counts` |
| `decide-studybar.png` | Decision 2, A beside B | `/?decide=studybar` |
| `decide-buttons.png` | Decision 3, A beside B | `/?decide=buttons` |

## Decisions for the owner

The surface pictures above all show the recommended answer, A.

1. **How the deck list shows the new, learning and due counts** (`decide-counts.png`).
   - A, recommended: three columns of coloured figures under a header row, a zero in regular weight and a muted colour. The columns line up down the list, so a glance down one column answers "where is anything due?", and long names never push them.
   - B: one total per Deck, with the non-zero kinds named under the name in their colours. Easier to read without the header, but each row is taller and comparing one kind down the list is harder.
2. **Deck title and time-remaining estimate on the study screen** (`decide-studybar.png`).
   - A, recommended: both shown. The Deck path sits above the counts and the estimate follows the counts in a muted caption. When studying a parent Deck it says which sub-Deck the Card came from, and the estimate helps decide whether to finish now. In the old screen both were settings, off by default; A shows them with no setting.
   - B: counts only, as the newer study screen has today.
3. **Where the answer buttons sit** (`decide-buttons.png`).
   - A, recommended: at the bottom, in the same place as Show answer, so the thumb does not move between showing and answering. No setting.
   - B: at the top, above the Card. Picking B keeps the old screen's top-or-bottom setting.
4. **Each feature only the old study screen has**: build or drop, in `old-study-screen-features.md` (32 rows, each with a recommendation and an empty "Owner's decision" column).

Smaller choices drawn without an alternative, which the owner can still change:

- **Long Deck names** wrap onto a second line and are then cut with an ellipsis; the counts stay in place. The full name stays in the text for screen readers, and the Deck's menu can show it in full.
- **The empty deck list** offers "Get shared decks" as the primary action, then "Import a file" and "Create a deck". Sync stays in the top bar.
- **More** lists Settings, Help and Support AnkiDroid, the drawer's lower group. Help and Support open web pages, so they carry an "opens outside" mark. "Support AnkiDroid" keeps Upstream's name and string, since it supports the project the Fork is built on.
- **The finished screen** has one primary button, "Back to decks", where Show answer sits on the study screen. Custom study and Deck options are always listed; the row the state points to (Custom study, when a daily limit was reached) is in cinnabar; "Unbury and keep studying" appears only when Cards are buried. Today's study summary closes the page. It says only what the Engine reports (whether new or review Cards remain over the limit, not how many).

## Not drawn

- Japanese interface text: the Mockup ticket asks for English and Simplified Chinese. Japanese Deck names are drawn, in Klee One.
- The finished screen for a filtered Deck (ticket 16): the same layout with the filtered-Deck explanation in place of the state text.
- Tablets, which keep the drawer and their two-pane layouts (ticket 15), 200% font size and right-to-left: these are checked on the rebuilt surfaces, not here.
- The study screen's tools around the Card (whiteboard, voice recorder, type-in-the-answer), which ticket 20 matches to the Card Frame drawn here.
