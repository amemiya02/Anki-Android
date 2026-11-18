# Settings and dialogs Mockups

Mockups for ticket 32: how settings are grouped and searched, and the seven dialog patterns every dialog is built from, in Quiet paper. Each surface is shown in English and Simplified Chinese, and each picture shows Light, Dark and Black side by side. Colours come from `../quiet-paper.values.json`; no new colour pairing is added, and `node mockups/scripts/check-contrast.mjs` passes.

The Light pictures here show the earlier cream Light palette, replaced by 白纸 Plain paper on 2026-11-18 (see `../light-palette/README.md`); the Mockups themselves now draw Plain paper.

Drawn on 2026-11-18, ahead of Milestone 4, at the owner's request. The settings are drawn as they will be after ticket 23: the old study screen and the settings only it used are gone, as the owner decided on 2026-11-17 in `../core-loop/old-study-screen-features.md`.

Start the Mockups with `npm install && npm run dev` in `mockups/`, then open the URL in the last column. `lang` is `en` or `zh`.

## Pictures

| Picture | Shows | URL |
|---|---|---|
| `overview-en.png`, `overview-zh.png` | Every surface below, in Light | `/?settings=overview&lang=en` |
| `map-en.png`, `map-zh.png` | Every setting that exists today, in the group it moves to, with the settings removed with the old study screen and those behind a developer option | `/?settings=map&lang=en` |
| `home-en.png`, `home-zh.png` | Settings home: the search field and the ten groups, each saying what it holds | `/?settings=home&lang=en` |
| `search-en.png`, `search-zh.png` | Search while typing: matching settings with where each lives | `/?settings=search&lang=en` |
| `appearance-en.png`, `appearance-zh.png` | Appearance: the theme as four pictures, the night theme, the deck list background | `/?settings=appearance&lang=en` |
| `studying-en.png`, `studying-zh.png` | Studying: scheduling and the study screen (answer buttons and toolbar follow below the fold) | `/?settings=studying&lang=en` |
| `controls-en.png`, `controls-zh.png` | Controls: corner taps, swipe sensitivity, then each action with its bindings, for the study screen and the previewer | `/?settings=controls&lang=en` |
| `toolbar-en.png`, `toolbar-zh.png` | Toolbar actions: an action being dragged from Menu only to Always show | `/?settings=toolbar&lang=en` |
| `dialog-confirm-en.png`, `-zh` | Confirmation: the backup prompt | `/?settings=dialog-confirm&lang=en` |
| `dialog-destructive-en.png`, `-zh` | Destructive confirmation: restore a backup over the collection | `/?settings=dialog-destructive&lang=en` |
| `dialog-input-en.png`, `-zh` | Input: create a Deck | `/?settings=dialog-input&lang=en` |
| `dialog-single-en.png`, `-zh` | Single choice: fetch media on sync, over the Sync settings | `/?settings=dialog-single&lang=en` |
| `dialog-multiple-en.png`, `-zh` | Multiple choice: what to include in a Deck export | `/?settings=dialog-multiple&lang=en` |
| `dialog-progress-en.png`, `-zh` | Progress: importing a Deck package | `/?settings=dialog-progress&lang=en` |
| `dialog-error-en.png`, `-zh` | Error: the sync server cannot be reached | `/?settings=dialog-error&lang=en` |
| `decide-theme.png` | Decision 1, A beside B, in English and Chinese | `/?settings=decide-theme` |
| `decide-destructive.png` | Decision 2, A beside B | `/?settings=decide-destructive` |

## Decisions for the owner

The surface pictures above all show the recommended answer, A.

1. **How the theme is chosen** (`decide-theme.png`).
   - A, recommended: one choice of four, drawn as small pictures of the deck list: Follow system, Light, Dark and Black. Under it, "Night theme: Dark or Black" says which dark theme Follow system uses at night. It names the three themes the Fork designs, and Black reads as a choice rather than a sub-setting.
   - B: two settings as today, "Theme" (Follow system, Light, Night) and "Night theme" (Dark, Black), each opening a single-choice dialog.
   Either way the stored values stay those of today's `appTheme` and `nightTheme`, so nobody's theme changes on update.
2. **How a destructive choice is marked** (`decide-destructive.png`).
   - A, recommended: a warning mark above the title, and the destructive button outlined in cinnabar with a warning icon and a label that names the act ("Replace collection", "Delete deck"), never filled. Cancel is plain text and takes focus first. Cinnabar is already the colour of every primary button, so a filled cinnabar button cannot by itself say "dangerous"; the outline, the icon and the empty fill can, in every theme.
   - B: a filled primary button, like any confirmation, with the act named in its label. Simpler, but a destructive dialog then looks like every other.
3. **No setting is recommended for dropping.** Every setting that exists today is on `map-en.png`, in its new group; the only ones missing are those the owner already dropped with the old study screen (listed on the map with their row numbers in `old-study-screen-features.md`). Two were looked at and kept:
   - The deck list background image stays in Appearance. In the rebuilt deck list (ticket 17's surface) the image should sit under a veil of the background colour, so the counts keep their contrast; that is for ticket 36 to check.
   - "Full screen navigation drawer" stays, shown on tablets only, as today: phones have no drawer.

Smaller choices drawn without an alternative, which the owner can still change:

- **Grouping.** Ten groups on one flat list, each with a line saying what it holds: General, Appearance, Studying, Controls, Accessibility, Sync, Notifications, Backups, Advanced, About. Developer options appear below About once switched on, as today. "Reviewing", "New study screen" and the study-screen half of today's Appearance become one group, Studying, because after ticket 23 there is one study screen. "Display filenames in card browser" moves from Appearance to General. Everything else keeps its group.
- **Search** is a field at the top of settings home, always visible, not an icon. Results come as the person types, each with its path ("Studying › Answer buttons"); the matching part is set heavier, not coloured. Tapping a result opens its group scrolled to the setting, which is marked briefly, as today. Search covers titles and summaries, bindings included.
- **Rows** show the current value under the title ("Learn ahead limit / 20 minutes") rather than hiding it behind a dialog. A row with a second line is 64dp, a one-line row 56dp. Section names are 13sp muted text. A switch is outlined and muted when off and filled in the primary colour when on.
- **Controls** keeps today's tabs (study screen, previewer). Each action lists its bindings as small labelled keys (taps, gestures, keys, controller buttons); tapping the action opens the existing binding picker.
- **Toolbar actions** keeps today's three lists (Always show, Menu only, Disabled). A dragged row is lifted with a cinnabar outline and a slight tilt, not a shadow, since surfaces are flat; where it will land is hatched.
- **Dialogs** are flat paper on a dimmed screen: the background colour, a hairline edge (the muted text colour in Black, where the hairline alone would vanish), 6dp corners like buttons, no shadow. The title is 22sp at weight 500. Buttons sit at the bottom right, 48dp tall; a dialog has at most one filled button, and Cancel is always plain text. A mark in a circle (the finished screen's mark) opens a confirmation, a destructive confirmation and an error; input, choice and progress dialogs have none.
- **Single choice** applies a choice when it is tapped, so it has only Cancel. Multiple choice and input confirm with a named button ("Export", "Create").
- **Progress** uses the progress bar's colours from the Core Loop and says what it is working on and how far it is. It has no buttons when the work cannot be cancelled; work of unknown length uses the same bar, moving.
- **Errors** say what failed and what to do in plain words, keep technical text behind "Show details" (which also offers copying it), and offer the obvious next step ("Try again") as the one filled button.

New design values the Mockups use that are not yet in `quiet-paper.values.json`, to be added when the dialog patterns are built (ticket 33): a `dialogTitle` type role (22sp, 500, line height 1.3), a one-line settings row of 56dp, a dialog corner of 6dp, and the scrim (black at 45% in Light, 68% in Dark and Black).

## Not drawn

- The other settings groups (General, Accessibility, Sync, Notifications, Backups, Advanced, About, Developer options). They use the same rows; their contents are on the map. Sync setup is redrawn by ticket 44 and notifications by ticket 47.
- Settings' sub-screens that are lists or pickers already shaped by these patterns: the language list, the timebox screen, keyboard shortcuts, and the binding picker.
- Each of the app's roughly 50 dialogs. Tickets 33 to 35 build each one from the seven patterns; dialogs that are larger than one pattern (the tag editor, custom study, change Note Type, the deck options preset picker) are built from the same pieces: title, body, choices, fields and the button row.
- Review reminders and Switch profile, which stay behind their developer options.
- Japanese interface text: the ticket asks for English and Simplified Chinese. Japanese Deck names are drawn, in Klee One.
- Tablets, 200% font size and right-to-left: these are checked on the rebuilt surfaces, not here.
