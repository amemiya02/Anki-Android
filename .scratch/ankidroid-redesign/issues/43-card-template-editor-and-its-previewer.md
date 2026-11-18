# 43: Card Template editor and its previewer

**What to build:** The owner edits a Card Template and previews the result in the redesigned look.

**Blocked by:** 41 (Mockups for the rest)

**Status:** ready-for-agent

- [x] The front, back and styling of a Card Template can be edited and saved.
- [x] Card Templates can be added, removed and reordered for a Note Type.
- [x] The template previewer shows the Card as the study screen would.
- [x] The template editor stays usable with a hardware keyboard.
- [x] On a tablet-sized screen the surface still works in its existing layout.
- [x] It matches the agreed Mockup.
- [ ] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Implementation

- Card Template tabs and the Front, Back and Styling controls stay above the source editor. The editor highlights field references and keeps the platform keyboard and text-selection actions. The field row inserts at the caret, and the full picker also offers special fields.
- The existing Engine save, add, delete and reposition operations back the new editor. Browser appearance uses the same Compose palette and still saves as part of the Card Template.
- The preview uses the study renderer. It uses the current Note when opened from a Note, the first Note of the Note Type when one is available, or placeholder values for a Note Type with no Notes. The tablet pane refreshes while editing and keeps the answer visible when it was already open.

## Verification

The keyboard regression failed on the old editor and passed after the redesign. All 66 focused editor, reposition, Note Type and preview tests passed. The Engine suite passed 143 tests, with seven existing skips.

On Android, the Note Type detail regression reproduced an ICU regex crash when opening Basic. It passed after the placeholder pattern was fixed. The same device test opened the template, edited its source and switched to the Back template with Ctrl+2. The MathJax preview test passed. The emulator showed an unsaved source edit in the phone preview and refreshed the displayed answer after typing in the tablet pane.

The screenshot suite covers the editor with the IME open, the actions sheet, standalone preview and tablet preview. The first full run identified selected-section contrast, clipped touch targets in the field row, and overlapping editor and preview roots in the capture test. After those fixes, all 24 Dark cases passed across English, Simplified Chinese, Japanese and Arabic at normal and 200% font size. They produced 32 captures. The Light and Black reruns are pending.
