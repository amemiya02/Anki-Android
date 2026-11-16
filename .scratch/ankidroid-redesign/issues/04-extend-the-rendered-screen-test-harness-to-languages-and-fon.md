# 04: Extend the rendered-screen test harness to languages and font sizes

**What to build:** A rendered-screen test can be run across languages and font sizes as well as themes and device sizes, so that the Quality Bar can be checked mechanically on any surface.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] A test can declare the languages it runs in; English, Simplified Chinese, Japanese and one right-to-left language are available.
- [x] A test can declare font sizes; normal and 200% are available.
- [x] The same options are available to tests of Compose surfaces and of screens not yet rebuilt.
- [x] One existing surface is captured across the full matrix as proof, and its pictures show the right language, direction and text size.
- [x] The accessibility check still runs at capture and fails a test on a contrast or touch-target problem.
- [x] Existing screenshot tests keep passing without declaring the new options.

## Comments

- How to use the harness: `implement-notes/04-screenshot-harness.md` in the shared notes.
- Languages and font sizes are declared with `@IncludeLanguages` and `@IncludeFontSizes`, or selected with `-Planguage` and `-PfontSize`. Arabic is the right-to-left language. Both base classes take them; meta tests `ScreenshotTestTest` and `ComposeScreenshotTestTest` check the strings, direction and font scale each one gets.
- The accessibility check did not fail tests before: it ran only with `-PaccessibilityScan` and only logged. It now fails on contrast and touch targets for a class annotated `@CheckAccessibility`, at every capture, in both base classes (`AccessibilityAtCaptureTest`, `ComposeAccessibilityAtCaptureTest`). It is opt-in so that Upstream's screens, which are not held to the Quality Bar, keep passing. ATF reports contrast measured from the picture as a warning, so contrast fails on warnings and touch targets on errors.
- Proof: `AllPermissionsExplanationScreenshotTest` (chosen over Introduction because its rows have a leading icon and a trailing switch, so mirroring is visible) records 48 pictures with `-Ptheme=light,dark,black`: 2 tests x 3 themes x 4 languages x 2 font sizes, on the phone. All were opened and show the right language, Arabic mirrored, and larger text at 200%. Devices other than the phone were not part of the proof.
- Existing tests: the whole screenshot suite passes (391 tests, 0 failures). Its default pictures were recorded with this change and then verified with the old harness: no differences.
- The Japanese 200% portrait of the permissions screen already overflows and scrolls; that is a finding about the old screen, not the harness.
- Cutting the themes to Light, Dark and Black (spec, Testing Decisions) is not part of this ticket.
