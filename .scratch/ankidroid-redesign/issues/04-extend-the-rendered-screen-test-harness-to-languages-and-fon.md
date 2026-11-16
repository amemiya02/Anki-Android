# 04: Extend the rendered-screen test harness to languages and font sizes

**What to build:** A rendered-screen test can be run across languages and font sizes as well as themes and device sizes, so that the Quality Bar can be checked mechanically on any surface.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] A test can declare the languages it runs in; English, Simplified Chinese, Japanese and one right-to-left language are available.
- [ ] A test can declare font sizes; normal and 200% are available.
- [ ] The same options are available to tests of Compose surfaces and of screens not yet rebuilt.
- [ ] One existing surface is captured across the full matrix as proof, and its pictures show the right language, direction and text size.
- [ ] The accessibility check still runs at capture and fails a test on a contrast or touch-target problem.
- [ ] Existing screenshot tests keep passing without declaring the new options.
