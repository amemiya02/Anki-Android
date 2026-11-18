# 20: Study screen: tools around the Card

**What to build:** Everything else the study screen offers looks like part of the same screen: typing the answer, drawing on the whiteboard, recording a voice, the timer, auto-advance and timeboxing.

**Blocked by:** 19 (Study screen: Card Frame and answering)

**Status:** done (device checks deferred to ticket 24)

- [x] The type-in-the-answer field matches the Card Frame and works with Chinese and Japanese input.
- [x] The whiteboard's tools match the Card Frame, and drawing, erasing, undo and redo work.
- [x] The voice recording controls match the Card Frame, and record and replay work.
- [x] The timer, auto-advance and the timebox message match the Card Frame and behave as before.
- [x] The toolbar customisation chosen in settings is respected.
- [x] Every feature marked "build" in the Core Loop Mockup ticket works in the study screen.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Comments

Built on `ticket/20`. The tools keep their places and behaviour; what changed is their look in Quiet
paper and every "build" row of `docs/redesign/core-loop/old-study-screen-features.md` that belongs here.

**How each criterion was checked**

- Type-in-the-answer: the field is flat in the Card's hairline frame, on the same paper, with Foundation
  type and a muted hint (pictures `type_answer`). Chinese and Japanese input: `ReviewerFragmentTest` types
  "熟悉" (still composing) and "勉強する" (committed) through the field's input connection, presses Done and
  reads the comparison; both match. A real input method on a device is **deferred to ticket 24**.
- Whiteboard: toolbar on paper with a hairline; its labels now use the full text colour (the muted
  label failed contrast on the selected brush). Drawing, erasing, undo and redo are Upstream's whiteboard,
  unchanged and covered by `WhiteboardViewModelTest`, `WhiteboardViewTest`, `WhiteboardEraseGestureTest`,
  `WhiteboardFragmentTest`, which pass.
- Voice recorder: the record, play, cancel and lock buttons are outlined by a hairline with the button
  radius, like the answer buttons (they were invisible against the page). Record and replay logic is
  unchanged (`AudioRecordViewTest`, `AudioPlayViewTest`, `CheckPronunciationViewModelTest` pass); recording
  with a microphone is **deferred to ticket 24**.
- Timer, auto-advance, timebox: the timer is now a muted caption with tabular figures at the end of the
  study bar (it sat on its own line and made the bar taller); over the limit it turns cinnabar as before.
  Auto-advance and the timebox message are unchanged code (`AutoAdvanceTest`, `AnswerTimerTest`); the
  timebox dialog's look belongs to ticket 34 (study screen dialogs).
- Toolbar customisation: `ReviewerFragmentTest` sets actions to the bar, the menu and off in settings and
  finds them where chosen in the study bar.
- "Build" rows (owner's decisions of 2026-11-17):
  - 1 Save a whiteboard drawing: "Save whiteboard" in the whiteboard's menu saves a PNG on the Card's
    paper and says where (`ReviewerFragmentTest`). PNG because `CompatV29` rejects the `image/jpg` type
    the old screen passed.
  - 4 Minimum press time: Show answer acts once held for "Show answer long-press time"; a tap says once
    where it is set (`AnswerAreaTest`). The setting shows again for the study screen.
  - 5 Double-tap timeout: implemented as the setting's own summary and the old screen did it (a second
    answer-button or Show answer press within the interval is ignored, `AnswerAreaTest`), not as the
    feature list's guess ("passed to `GestureParser`"). Gestures keep the system double-tap time.
  - 7, 8 Estimate and Deck title: done in ticket 19.
  - 11 Centre vertically, 12 image zoom, 13 `ankidroid_dark_mode`: `ReviewerViewModelTest`,
    `ReviewerFragmentTest`; centre vertically is now in the study screen's settings, image zoom shows in
    Accessibility.
  - 16 Answer-button size: one-time copy, preference upgrade 31 (`PreferenceUpgradeServiceTest`).
  - 17 Gestures off: preference upgrade 32 clears gesture bindings for someone who studied in the old
    screen with gestures off; gestures bound by someone who used the newer screen are kept.
  - 20 Hide the whiteboard but keep it on: "Hide whiteboard"/"Show whiteboard" in the whiteboard's menu
    (`WhiteboardFragmentTest`), remembered for the Deck (`ReviewerFragmentTest`).
  - 22, 23 Per-Deck whiteboard and voice recorder: on/off and the drawing's visibility are kept in MetaDB
    for the Deck being studied, where the old screen kept them (`ReviewerViewModelTest`). A Deck never set
    follows the earlier setting for every Deck.
  - 30 Recovery when the Card view crashes: already in the study screen from Upstream
    (`SafeWebViewLayout` recreates the WebView and `CardViewerFragment.onWebViewRecreated` reloads the
    Card; `ReviewerFragmentTest` covers it). Nothing to build.
  - 32 Keep the screen on during auto-advance: `ReviewerFragmentTest`.
  - 6 and 26 (JavaScript API) are tickets 21 and 22.
- Mockup and Quality Bar: `StudyScreenToolsScreenshotTest` (`@CheckAccessibility`, zh-cn/ja/ar, 200%),
  96 pictures with `-Ptheme=light,dark,black`, all passing contrast and touch-target checks, compared by
  eye with the Card Frame of `question-*.png`/`answer-*.png` (the Mockup draws no tools; ticket 14 says
  ticket 20 matches them to the Card Frame). Owner gate: Mockups approved in advance by the owner on
  2026-11-17.
- Screen-logic and Engine tests: full `testPlayDebugUnitTest` (4589 tests) and `:libanki:test` pass with no
  assertion changed; `StudyScreenCardFrameScreenshotTest` (ticket 19, 48 pictures) still passes and looks
  the same. Under very heavy machine load `ReviewerFragmentTest.nosuggest resets for the next typing card`
  once hit its 60s timeout; it passes alone and in the full runs.

**Different from the feature list, on purpose**

- Row 22 also named the stylus mode and pen colour per Deck. The newer whiteboard has a list of brushes
  instead of one pen colour (row 21 dropped the pen colour command for the same reason), and keeps stylus
  mode as one whiteboard setting; both stay global.
