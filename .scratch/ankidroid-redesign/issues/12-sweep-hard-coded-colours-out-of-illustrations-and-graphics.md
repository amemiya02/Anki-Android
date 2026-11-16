# 12: Sweep hard-coded colours out of illustrations and graphics

**What to build:** Illustrations, empty states, backgrounds and other graphics that are not interface icons fit the chosen Direction in all three themes.

**Blocked by:** 07 (Foundation: colours, shape and the three themes)

**Status:** done

- [x] No graphic shows the old look's blue or a colour that clashes with the Foundation.
- [x] Each graphic is legible in Light, Dark and Black.
- [x] Graphics the chosen Direction does not want are removed or replaced.
- [x] The empty deck list and the first-run screen are recaptured in all three themes as proof.

## Comments

- Carried over from ticket 10: the old blue still shows in these graphics, which this ticket now owns: the first-run gradient (`intro_gradient.xml`), the navigation drawer header image, the empty-collection box, the sync account illustrations, and the small widget's icons (shared with ticket 46). Colours set in Kotlin also count, for example the light blue `PreviousAnswerIndicator` on the old study screen.
- Done. `GraphicsUseFoundationTest` now fails on any drawable that fixes a colour outside the Foundation, on any bitmap graphic, and on code that draws with a fixed colour (exemptions, each with a reason: launcher layers and shortcuts may use fixed Foundation colours, since the launcher cannot read the theme; masks the system or a view tints; whiteboard inks and a drawing's paper, which are content).
- Replaced with Hayaki's mark (the Mockup's two paper cards, in ink, muted ink and cinnabar): the empty-collection box, the first-run logo, the drawer header banner (three bitmaps), the sync account illustrations, the small and add-note widgets' pictures (vectors and bitmaps). Samsung's multi-window icon is the launcher icon.
- Removed: the first-run gradient; the shadows beside the tablet study pane and around the audio recorder panel (now hairlines); the widget picker bitmaps (`previewImage`), so launchers before Android 12 show the launcher icon there; the launcher shortcuts' blue discs and star (now Material Symbols in cinnabar on paper).
- Proof (Light, Dark, Black): empty deck list, first run (portrait, gesture, landscape), drawer, sync account, tablet deck list, small and add-note widgets, audio recorder, reminder troubleshooting, search history, Show answer.
- Notifications now use the launcher icon's cinnabar as their accent; ticket 47 still owns their look.
