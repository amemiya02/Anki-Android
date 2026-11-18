# 30: Previewer

**What to build:** The owner can preview how a Card will look, from the card browser or the note editor, in a previewer that matches the study screen's Card Frame.

**Blocked by:** 25 (Mockups for finding and adding)

**Status:** ready-for-agent

- [x] A Card can be previewed from the card browser and from the note editor.
- [x] The question and the answer side can both be shown, and the owner can step between Cards.
- [x] Card Content renders exactly as it does in the study screen.
- [x] The note editor's preview (one tab per Card Template or cloze number) is rebuilt in the same Card Frame.
- [x] It matches the agreed Mockup.
- [ ] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [ ] Screen-logic tests and Engine tests still pass unchanged.

## Verification

The Pixel emulator opened the browser preview from a two-Card selection. It rendered both sides, advanced to the second Card with the existing keyboard shortcut, and jumped back with the slider. The note editor's Preview button opened the template tabs and showed both sides. At tablet width the preview stayed beside the editor and rendered the same answer there.

Android reported a focus time-out during the first cold WebView startup while several builds were running. Warm browser launches and the phone and tablet editor previews completed successfully.

The browser rendered regression failed on the old screen and then passed with 38 existing preview, renderer and link tests. The note editor rendered regression also failed on the old screen. Its green run, the remaining screen-logic checks and the screenshot matrix are queued.
