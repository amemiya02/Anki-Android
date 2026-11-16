# 11: Move interface icons to one Material Symbols style

**What to build:** Every icon in the interface comes from one family in one style and weight, tinted from the Foundation. Done in batches by area of the app.

**Blocked by:** 07 (Foundation: colours, shape and the three themes)

**Status:** done

- [x] Every interface icon is a Material Symbols icon in the chosen style and weight.
- [x] Icons take their colour from a Foundation colour role, not from a colour inside the icon.
- [x] Icons that carry direction mirror in a right-to-left language.
- [x] Every icon-only control keeps its accessibility label.
- [x] No two controls that do different things share an icon.

## Comments

- The check is `InterfaceIconsAreMaterialSymbolsTest` (AnkiDroid unit tests). It reads every vector
  drawable of the six app modules. An interface icon must be `symbol_<name>.xml`, made by
  `tools/material-symbols/symbol.py` from Google's `google/material-design-icons` (Apache-2.0,
  recorded in `REUSE.toml`), in the style the values JSON names, with the path data recorded in
  `tools/material-symbols/symbols.lock`; its tint must be a theme attribute and its paths white; and
  it must mirror exactly when Google's own Android export of the symbol mirrors. The theme's up arrow
  and overflow icon are checked too. Before the conversion it listed 234 icons that were not symbols
  and 107 that carried their own colour. No icon font or icon library was added.
- 196 symbol drawables replace 225 icons (duplicates such as the five stars and three checks merged).
  Kept out, each named in the test with its ticket: the launcher icon and launcher shortcuts, the
  Samsung multi-window logo, the empty-collection, first-run and AnkiWeb illustrations, widget
  pictures, the notification icon, the gesture picker's tap zones and the colour picker's pin shape.
- One fixed colour remains, by design: a Flag's icon is the flag symbol tinted with that Flag's colour,
  which is what it shows. Answer icons are tinted with the Foundation's answer roles; a marked note's
  star and a saved search's heart with the emphasis role.
- Fill is 0 everywhere, so toggles that used a filled icon for "on" (auto advance, whiteboard, voice
  recorder, mark, bottom bar tabs) show the same outline symbol in both states; their titles or the
  bar's indicator already say which state is on.
- No fitting symbol, and the choice: cloze on the same card `data_array`, cloze on a new card
  `post_add`; question and answer side `question_mark` and `exclamation`; record
  `radio_button_checked` (a fill-0 dot is an empty ring); show all hints `lightbulb_circle`
  (Google's `tips_and_updates` is still an old 24-unit drawing); toolbar position `open_with`;
  whiteboard undo-stroke `u_turn_left`. Browse is `search`, as the Mockup draws it; the deck list's
  deck filter in the same bar became `filter_list`.
- Controls that did different things and shared an icon now differ: stylus mode vs edit, previous
  card info vs card info, clear whiteboard vs close vs empty filtered deck, reset progress vs backups,
  abort learning vs Again, "Other ways to support" vs "Get help", review reminders vs notifications.
  Left alone: labelled rows that do the same kind of thing (the help lists' several "open a web page"
  links, two manuals, two privacy policies), bury/suspend card vs note in the controls list, and the
  same command listed for both study screens.
- Accessibility labels: no content description changed; the drawer button keeps the toggle's
  "open/close drawer" labels (`MenuSymbolDrawable` only replaces the drawing).
- Checked by eye in Light and Dark, English and Arabic: deck list (with the add menu and bottom bar),
  both study screens, card browser, note editor, settings, the drawer and the review-reminder and
  change-note-type dialogs. Arrows, chevrons, undo/redo, flag, help and note_add mirror in Arabic;
  bold, italic and the text-format icons keep their direction.
- Icons that libraries draw were replaced too: the app bar's up arrow and overflow icon and the
  search field's icons through the theme, filter chips' check mark through
  `Widget.Foundation.Chip.Filter`, text fields' clear and password icons per field, and the settings
  search's icons by overriding the library's drawables by name, and the time picker's clock and
  keyboard switch through its style. Still drawn by the platform: spinners' own drop-down arrow.
