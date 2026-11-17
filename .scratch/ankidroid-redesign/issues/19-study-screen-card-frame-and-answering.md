# 19: Study screen: Card Frame and answering

**What to build:** The owner studies in the redesigned study screen by default: the Card Content is the focus, the answer is one tap away, and the four answer buttons are easy to hit.

**Blocked by:** 14 (Mockups for the Core Loop)

**Status:** done (device checks deferred to ticket 24)

- [x] Upstream's newer study screen, redesigned, is the Fork's default; the old study screen stays selectable in settings until it is removed.
- [x] Card Content renders exactly as it does today; only the Card Frame changes.
- [x] Show answer, then Again, Hard, Good and Easy work, and each answer button shows when the Card comes back.
- [x] The remaining counts are visible, and undo is within reach.
- [x] Flag, mark, edit, Card info, tags, bury, suspend, delete, reschedule and the other Card actions are reachable from the menu.
- [x] Gestures, tap zones, hardware keys and controller bindings behave as before.
- [x] Deck title, time-remaining estimate and answer-button position follow the Mockup.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Comments

Built on `ticket/19`. What changed: the redesigned study screen (`ReviewerFragment`) is the default (the
old one is still chosen with the "New study screen" switch off); the study bar shows the Card's Deck as
a path, the remaining counts and the time-remaining estimate, with undo and the menu beside it and a
progress line under it (Mockup A of `decide-studybar.png`); the Card sits in a hairline frame; Show answer
and the four answer buttons sit at the bottom (Mockup A of `decide-buttons.png`); every Card action is
in the menu by default.

**How each criterion was checked**

- Default and old screen selectable: `StudyScreenDefaultTest`.
- Card Content: the Card's HTML, CSS and WebView setup are untouched (`stdHtml`, `CardViewerFragment`);
  only the frame around the WebView changed. Robolectric draws no Card Content, so the look on a device
  is **deferred to ticket 24**.
- Answering and next times: `ReviewerViewModelTest`, `AnswerAreaTest`, and the "answer" pictures
  (Show answer is clicked by its label).
- Counts and undo, Deck title and estimate: `ReviewerViewModelTest`, `StudyBarTest`, pictures.
- Menu: `ReviewerMenuRepositoryTest` (Card info, previous Card info, tags, set due date, reset, add,
  Deck options and replay were switched off by default in the newer screen; now in the menu as in the
  old screen).
- Gestures, tap zones, keys and controllers: their code is unchanged and `GestureParserTest`,
  `ReviewerFragmentTest`, `ReviewerShakeTest`, `ReviewerWhiteboardGestureTest` pass. The newer screen's
  bindings were always its own (Upstream's design), so "as before" on a device with the owner's real
  bindings is **deferred to ticket 24**.
- Mockup and Quality Bar: `StudyScreenCardFrameScreenshotTest` (`@CheckAccessibility`), 48 pictures with
  `-Ptheme=light,dark,black` (en, zh-cn, ja, ar; normal and 200%), all passing contrast and touch-target
  checks, compared by eye with `question-*.png` and `answer-*.png`.
- Logic and Engine tests: full `testPlayDebugUnitTest` (4553 tests), `:common:android` and `:libanki:test`
  pass. Two old-screen tests now choose the old screen explicitly, since it is no longer the default
  (`ReviewCompletionTest`, and the XML key check in `ControlsSettingsFragmentTest`); no assertion changed.
  `StudyCountsViewTest` was deleted with `StudyCountsView`; `StudyBarTest` covers the same counts.

**Differences from the Mockup, on purpose**

- Show answer is 56dp and the answer buttons 66dp as drawn, but the area keeps the answer buttons'
  height on both sides, so Show answer has 10dp more space above it and the Card does not change size
  (and reflow) when the answer is shown.
- The estimate reuses the old screen's translated string ("6 minutes left", "剩余 6 分钟", "残り約6分")
  instead of the Mockup's "about 6 min left", as the spec asks to reuse existing strings.
- The active queue's count stays underlined, as in the newer screen, so the person still sees which
  kind of Card is showing.
- The back button and the menu are still Views around Compose pieces; the menu
  (`ReviewerMenuView`, user-configurable) was not rebuilt.

**Found and fixed on the way:** Compose ignored the type scale's weights (every role drew at 400) because
the Foundation wrapped one Android typeface in a `FontFamily`. Fixed in `Foundation.kt` with
`FoundationWeightTest`; it affects every Compose surface.

**Left to ticket 20:** everything marked "build" in `old-study-screen-features.md` and the look of the
type-in-the-answer field, whiteboard, recorder and timer.
