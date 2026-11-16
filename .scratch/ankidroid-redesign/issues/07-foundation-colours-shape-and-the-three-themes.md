# 07: Foundation: colours, shape and the three themes

**What to build:** The whole app takes on the chosen Direction's colours and shapes at once, in Light, Dark or Black, including screens that have not been rebuilt yet. The Plain and E-ink themes are gone.

**Blocked by:** 04 (Extend the rendered-screen test harness to languages and font sizes); 06 (Complete the chosen Direction: Dark, Black, name and launcher icon)

**Status:** done

- [x] Colour roles, spacing, corner shapes and elevation are defined once and read by both Compose surfaces and the remaining XML screens.
- [x] Theme settings offer Light, Dark and Black, and the app follows the system's light or dark setting by default.
- [x] The Plain and E-ink themes and their settings entries are removed; someone who had one selected lands on Light.
- [x] Wallpaper-derived colour is not used anywhere.
- [x] The rendered-screen harness runs Light, Dark and Black only.
- [x] The deck list, the newer study screen and one dialog are captured in all three themes as proof.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Comments

- The Foundation lives in `common/android/src/main/res/values/foundation.xml` (XML theme attributes
  `?attr/foundation<Role>` and `foundation_*` dimensions) and is read by Compose as
  `MaterialTheme.foundation`. `FoundationTest` compares every colour, shape and spacing value with
  `docs/redesign/quiet-paper.values.json`.
- Theme settings: Theme = Follow system (default) / Light / Night; Night theme = Dark (default) / Black.
  The Day theme setting is gone, since Light is the only day theme.
- In Dark and Black, Material's `colorPrimary` (used for text buttons, links and checked controls on
  screens not yet rebuilt) is the lighter emphasis cinnabar with ink on it, because the Direction's
  button cinnabar is only 3.5:1 on the Dark background. The add button and Show answer use the
  Direction's primary.
- Proof pictures (Light, Dark, Black): `DeckPickerScreenshotTest` (`baseState`, `fabExpanded`,
  `hierarchy_lines`), `StudyScreenScreenshotTest` (newer study screen, every bar/frame variant) and
  `CustomStudyDialogScreenshotTest` (Extend daily limits). The study screen was captured without a
  Card, so the answer buttons themselves were not pictured; `ThemeTest` checks their colours.
- Left for later tickets, visible in the pictures: hard-coded colours in icons and graphics (the
  empty-collection illustration) and the typeface (ticket 08,
  tickets 10 to 12). The add-button speed-dial items still cast Material's grey shadow.
