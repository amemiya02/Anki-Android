# Finding and adding Mockups

These are the Mockups for ticket 25: the card browser, the note editor and the previewer, drawn in Quiet paper. They were drawn on 2026-11-18, ahead of their Milestone, because the owner asked to see every remaining surface at once.

Each surface is shown in English and Simplified Chinese. Each picture puts Light, Dark and Black side by side.

Colours come from `../quiet-paper.values.json`, with one exception: the seven flag colours, which the values file does not have yet (Decision 4). The pairings these Mockups add are checked by `node mockups/scripts/check-find-contrast.mjs`, and every one passes. Those pairings are:

- each flag against the background;
- ink on the progress-track fill, used by an active chip and a selected row;
- a ticked box against emphasis.

To see the Mockups, run `npm run dev` in `mockups/` and open the URL in the last column of the table below. `lang` is `en` or `zh`.

## Pictures

| Picture | Shows | URL |
|---|---|---|
| `overview-en.png`, `overview-zh.png` | Every surface below, in Light | `/?find=overview&lang=en` |
| `browse-en.png`, `browse-zh.png` | Browse as it opens: the search field, filter chips, result count, sort, the Cards/Notes switch, and two-line result rows with flag, marked, suspended and buried marks | `/?find=browse&lang=en` |
| `browse-search-en.png`, `browse-search-zh.png` | The search page: a query typed, the chips filled in from it, recent searches, saved searches and a link to the manual's search syntax | `/?find=browse-search&lang=en` |
| `browse-flags-en.png`, `browse-flags-zh.png` | The Flags chip's sheet: several flags at once, No flag, and Rename flags | `/?find=browse-flags&lang=en` |
| `browse-sort-en.png`, `browse-sort-zh.png` | The sort sheet: No sorting, the sortable columns, and the direction | `/?find=browse-sort&lang=en` |
| `browse-menu-en.png`, `browse-menu-zh.png` | Browse's menu: Undo, Select all, Save this search, Create filtered deck, Find and replace, Columns, Rename flags, Browser options | `/?find=browse-menu&lang=en` |
| `browse-select-en.png`, `browse-select-zh.png` | Selecting Cards: a tick box on every row and the common actions at the bottom (Deck, Flag, Tags, Suspend, More) | `/?find=browse-select&lang=en` |
| `browse-actions-en.png`, `browse-actions-zh.png` | More, while selecting: every other bulk action | `/?find=browse-actions&lang=en` |
| `browse-columns-en.png`, `browse-columns-zh.png` | Columns: the shown columns in order, and the available ones, for each mode | `/?find=browse-columns&lang=en` |
| `editor-add-en.png`, `editor-add-zh.png` | Adding a Note: Note type and Deck, fields with the keep pin and attach, Tags, Cards, and the formatting toolbar | `/?find=editor-add&lang=en` |
| `editor-typing-en.png`, `editor-typing-zh.png` | Typing a cloze, with the keyboard up: the cloze buttons lead the toolbar | `/?find=editor-typing&lang=en` |
| `editor-duplicate-en.png`, `editor-duplicate-zh.png` | The duplicate warning under the first field | `/?find=editor-duplicate&lang=en` |
| `editor-attach-en.png`, `editor-attach-zh.png` | Attaching to a field: image, camera, drawing, record audio, audio file, video file | `/?find=editor-attach&lang=en` |
| `editor-edit-en.png`, `editor-edit-zh.png` | Editing a Note, with its menu: Add note, Copy note, field text size, capitalise sentences, toolbar layout | `/?find=editor-edit&lang=en` |
| `preview-question-en.png`, `preview-question-zh.png` | The previewer, question side | `/?find=preview-question&lang=en` |
| `preview-answer-en.png`, `preview-answer-zh.png` | The previewer, answer side | `/?find=preview-answer&lang=en` |
| `preview-templates-en.png`, `preview-templates-zh.png` | The note editor's preview: one tab per Card the Note makes | `/?find=preview-templates&lang=en` |
| `decide-rows.png` | Decision 1, A beside B, in English and Chinese | `/?find=decide-rows` |
| `decide-search.png` | Decision 2, A beside B | `/?find=decide-search` |
| `decide-save.png` | Decision 3, A beside B | `/?find=decide-save` |

## Decisions for the owner

All the surface pictures above show the recommended answer, A.

### 1. How a result row shows its columns (`decide-rows.png`)

- **A, recommended: two lines per row.** The first chosen column is the row's title. The other chosen columns follow on one line under it, in their chosen order. Columns can still be chosen, reordered and remembered (`browse-columns`).
  - On a phone, a 17sp title and one caption line can be read at a glance, and nothing is cut to a few characters.
  - Long Chinese and Japanese text has room.
  - Flags, marked, suspended and buried show at the end of the row as icons and words.
- **B: a table of columns, as today.** It is reskinned, scrolls sideways, and draws the flag as a coloured rule at the start of the row. It suits many columns better, but on a phone the third and fourth columns are cut, and "Recognition" becomes "Recogniti…".

Tablets keep their existing layout either way (spec, Navigation).

### 2. Upstream's unfinished search tools (`decide-search.png`)

Whichever is picked, **Upstream's new search view is adopted and finished** (spec: "adopted, finished or dropped"). It becomes the only search, and the toolbar search box goes. Finishing it means:

- the chips take more than one choice;
- the chips are filled in from a typed or saved query;
- every label is translated;
- the flag summary is right whatever order the flags are picked in;
- flags can be renamed from the Flags sheet.

The chips are All decks, Card state, Flags and Tags. Sort moves to a button on the result line, beside the Cards/Notes switch.

- **A, recommended: drop the search tools and link to the manual.** The tools are the twelve tabs of example syntax behind the "Card browser search tools" developer flag. A gets a single row instead, "How to write a search", which opens the manual's search page outside the app.
  - The tools are a reference list, not a query builder.
  - Their text is English only and would need translating and keeping in step with the Engine's syntax.
  - The chips already cover the common filters.
- **B: finish the tools as a translated "Build a search" list.** It has one row per kind of search, each with two examples, and opens a short list of examples that are added to the query. It would cost about 100 new strings in each language, and it pushes saved searches below the fold.

### 3. Where Add and Save sit in the note editor (`decide-save.png`)

- **A, recommended: in the top bar,** as a small cinnabar button. It stays visible when the keyboard is up, and it leaves the fields the whole page.
- **B: a wide button above the formatting toolbar,** where Show answer sits on the study screen. It is easier to reach with a thumb, but it is pushed up by the keyboard and takes a line from the fields.

### 4. Flag colours

These need agreement, though no alternative is drawn. The Foundation has no flag colours. Upstream's (for example `#FF4747`, `#6FF06F`) are bright fills that clash with paper and fail contrast as marks on the light background.

The proposal is in `mockups/src/mock/find/flags.json`:

- Red, orange, green and blue reuse the Foundation's own hues (Again, Hard, Good, Easy).
- Pink, turquoise and purple are new and drawn to match.
- Every one reaches at least 5:1 on the background in all three themes.

They join `quiet-paper.values.json` and `foundation.xml` when ticket 26 builds the rows.

### 5. What I recommend dropping or moving

Nothing found in the code is dropped except Upstream's search tools (Decision 2, A). These things move:

- **Moved to the search page and the chips:**
  - Search by tag → Tags chip.
  - Search by flag → Flags chip.
  - Show marked and Show suspended → the Card state chip, which lists New, Learning, Review, Buried, Suspended and Marked.
  - My searches → Saved searches.
  - Change display order → the sort button.
  - Save search stays in the menu.
- **Row colours:** today a whole row is tinted for flagged, marked, suspended and buried Cards. That becomes the marks at the end of the row. Suspended and buried titles step back to the muted colour and the state is written out, so nothing is told by colour alone.
- **The Cards/Notes switch** moves out of Browser options to the result line. Truncate content, Ignore accents and Default search text stay in Browser options. Manage columns and Rename flag get their own menu rows.
- **Selection** keeps everything in today's multi-select menu.
  - Change deck, Flag, Tags and Suspend sit in the bottom bar.
  - Everything else is under More: Edit note, Preview, Card info, Mark, Change note type, Find and replace, Export, Set due date, Reposition, Grade now, Reset progress, Bury and Delete.
  - Select all is in the top bar.
  - Long-press still starts a selection, and a long-press while selecting selects a range.
- **Keyboard shortcuts** in the card browser, the note editor and the previewer are kept as they are.

## Smaller choices drawn without an alternative

The owner can still change any of these.

**Card browser**

- **Browse opens with a large title,** like Decks. The search field is under the title, not in the top bar. Tapping Browse in the bottom bar again focuses the field.
- **The search page replaces Browse while typing.** It has no bottom bar, and the back arrow returns to the results.
- **The add button** sits above the bottom bar, as on Decks.
- **Sheets and menus** are flat, with a hairline edge and no shadow. Sheets have 6dp corners.
- **Selected rows** take the progress-track fill. A ticked box is emphasis with the tick in the background colour.

**Note editor**

- **Note type and Deck** sit side by side at the top of the editor.
  - When adding, Note type is a choice.
  - When editing, it opens Change note type (the swap mark), as today.
- **A field** is its label, the pin and attach buttons, then the text above a ruled line. The line under the field being typed in is thicker and in emphasis.
- **The keep pin** (today's sticky field) shows only when adding, and turns emphasis when on.
- **The duplicate warning** is written under the first field, in emphasis with a warning mark, with Show to list the duplicates. Today the field's underline only changes colour.
- **Tags** show as hairline chips with a + to add more.
- **Cards** lists the Card Templates the Note makes and opens the Card Template editor.
- **The formatting toolbar** sits on the bottom edge, above the keyboard. It scrolls sideways. Its buttons are:
  - Bold, Italic, Underline, MathJax, Heading, Text size and Horizontal line;
  - the custom buttons (1, 2, …) and + to add one;
  - for a cloze Note Type, the two cloze buttons come first.
  The Toolbar layout setting (scroll, stack, hidden) still applies.
- **The menu** holds Field text size, Capitalise sentences (with its tick) and Toolbar layout. Add note and Copy note are added when editing.

**Previewer**

- **The previewer reuses the study screen's Card Frame:**
  - the Deck path, with "3 of 25" under it;
  - Flag (showing the Card's flag colour), Mark, Edit and a menu holding "Show the back side only";
  - a slider in place of the progress line, to jump between Cards;
  - at the bottom, a square Previous button beside one primary button, which shows the answer and then moves to the next Card.
  It has no answer buttons. The slider and position are hidden for a single Card.
- **The note editor's preview** has one tab per Card Template, or per cloze number, and one Show answer / Show question button. A tab whose front would be empty keeps today's mark.
- **Card Content** is drawn as Anki's default Card Template styles it, as on the study screen. It is not restyled.

## Not drawn

- **Dialogs reached from these screens.** They belong to tickets 32 and 34 (dialogs: study screen and card browser) and 35 (note editor). They are:
  - Change deck, Edit tags, Set due date, Reposition, Grade now, Reset progress and Find and replace;
  - Save search, Rename flag and Browser options;
  - Field text size and Toolbar layout;
  - the add-custom-button dialog;
  - discarding unsaved changes, and the cloze and "no Cards created" warnings.
- **The Deck picker and Note Type picker** opened from the chips and the editor. They follow the single-choice dialog pattern in ticket 33.
- **The drawing (whiteboard) and the audio recorder** opened from Attach. They are matched to the Foundation in ticket 29.
- **Image occlusion Notes.** Their editor is an Anki Page. The note editor keeps its Edit occlusions button and its Gallery, Camera and Paste image buttons.
- **The Card state and Tags sheets.** They are drawn like the Flags sheet: tick boxes and Done.
- **Notes mode.** It is the same rows with the Note's columns (by default Sort field, Note type, Card, Tags) and "N notes" on the result line.
- **An empty search result.** It reads "No cards match" under the chips, with Clear.
- **Tablets** keep the browser's side-by-side editor and the editor's preview pane in their existing layouts (spec, Navigation).
- **Japanese interface text, 200% font size and right-to-left.** These are checked on the rebuilt surfaces, as in the Core Loop. Japanese Card and Deck names are drawn in Klee One.
