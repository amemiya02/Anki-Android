# 10: Sweep hard-coded colours and text sizes out of layouts

**What to build:** No screen that has not been rebuilt yet shows a leftover colour or text size from the old look. This is a mechanical sweep, done in batches so the app stays green between them.

**Blocked by:** 07 (Foundation: colours, shape and the three themes); 08 (Foundation: bundled typefaces and the type scale)

**Status:** ready-for-agent

- [x] No layout sets a colour directly; each uses a Foundation colour role.
- [x] No layout sets a fixed text size; each uses a Foundation type style.
- [x] A check fails the build if a layout hard-codes a colour or a text size again.
- [ ] Every screen with an existing screenshot test is recaptured, and no picture shows the old blue or an unreadable pairing in any of the three themes.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Comments

- The check is `LayoutsUseFoundationTest` (AnkiDroid unit tests, so `testPlayDebugUnitTest` fails on it).
  It reads every layout of `:AnkiDroid`, `:common:android`, `:widgets` and `:anki-common`, and the
  Fork's own styles a layout names (with their parents). A colour must be a theme attribute, a colour
  state list made only of theme attributes, or transparent. A text size must be a
  `TextAppearance.Foundation.*` style, or a theme slot that Light and Dark both point at one;
  `android:textSize` is never allowed. Before the sweep it listed 91 colours and 169 text sizes
  (including styles and unmapped theme slots); a planted `#123456` and `14sp` were each reported.
- One exemption, named in the test: the whiteboard's six pen swatches keep their ink colours, because
  each swatch shows the colour its pen draws in.
- Values with no exact role, and the nearest one used: the whiteboard's checked-tool grey ->
  `progressTrack`; the audio recorder's blue file icon and the "slide to cancel" grey ->
  `onBackgroundMuted`; the record button's grey disc -> `background` with an `emphasis` ring; the
  first-run divider, the tablet settings divider and the switch separator -> `outline`; the small
  widget's blue badge -> `primary` with `onPrimary`. Labels above fields that used Material's
  LabelLarge (a button style) -> Body; category headers in AppCompat Body2 -> Body.
- The AppCompat and Material 2 type slots (`textAppearanceListItem*`, `Headline1-6`, `Subtitle1-2`,
  `Body1-2`, `Caption`, `Button`, `Overline`) now point at Foundation styles in Light and Dark.
- Found while looking at the pictures and fixed: the primary-filled add buttons (deck list, review
  reminders, note types, fields) were labelled in Material's on-primary, which Dark and Black map to
  the background (dark text on dark cinnabar, about 3:1). They now use `foundationOnPrimary`.
- Found while sweeping and fixed: layout text in a Foundation type style was always drawn at weight
  400, because the themes' font family overrides a type style's `textFontWeight` in AppCompat and the
  platform. It went unseen while Deck names were bold. `FoundationViewInflater` now draws each text at
  its type style's `foundationTextWeight` (never lighter than the view already was);
  `InterfaceTypefaceDeckListTest` checks a Deck name at DeckName's 500.
- Pictures: every screenshot test recorded twice with `-Pscreenshot -Ptheme=light,dark,black` (phone,
  English, plus the languages and sizes some classes declare), the second time after the weight fix;
  all passed, including the classes that check accessibility. At least one Dark picture of every
  screenshot class was looked at, and a sample in Light and Black.
- `:AnkiDroid:testPlayDebugUnitTest` (whole suite), `:libanki:test`, `:common:android:testDebugUnitTest`
  and `:AnkiDroid:lintPlayRelease` pass. The only existing test changed is
  `InterfaceTypefaceDeckListTest`, a typeface proof from ticket 08 whose Deck name was bold.
- Criterion 4 is met for everything a layout or style sets. The old blue still shows in graphics,
  which ticket 12 owns: the first-run gradient (`intro_gradient.xml`), the navigation drawer header
  image, the empty-collection box, the sync account illustrations, and the small widget's icons (also
  ticket 46). Colours set in Kotlin are outside this sweep too, for example the light blue
  `PreviousAnswerIndicator` gives the chosen answer on the old study screen.
- `CardSideSelectionDialogScreenshotTest` shows a light dialog in Dark and Black. The test opens the
  dialog on a bare `FragmentActivity` rather than an app screen, which looks like the cause; no layout
  colour is involved. Left for the dialogs tickets (33 to 35).

