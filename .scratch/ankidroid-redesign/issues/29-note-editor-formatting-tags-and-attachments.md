# 29: Note editor: formatting, tags and attachments

**What to build:** The owner can still write rich Notes: formatted text, tags, images, audio and drawings.

**Blocked by:** 28 (Note editor: fields, Deck, Note Type and saving)

**Status:** ready-for-agent

- [x] The formatting toolbar applies the same formatting as today and its custom buttons still work.
- [x] Tags can be added and removed.
- [x] An image can be attached from the gallery or camera, an audio clip recorded, and a drawing made.
- [x] Cloze deletions can be inserted for a cloze Note Type.
- [x] The toolbar layout chosen in settings is respected.
- [x] The drawing (whiteboard) and the audio recorder opened from Attach are matched to the Foundation.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [ ] Screen-logic tests and Engine tests still pass unchanged.

## Implementation and evidence

- The Editor formats selected field text with the existing HTML and MathJax commands. The toolbar places the cloze commands first and uses the saved Scroll, Stack, or Hidden setting. Custom buttons use their saved markup and keyboard shortcuts.
- Attach identifies the field by name and offers the existing gallery, camera, drawing, recording, audio file and video file routes. The drawing and recorder use Foundation colors, type, shapes and accessible controls.
- The rendered regression for the missing attachment title failed in both English Light font sizes. The rendered cloze-order check failed on the old toolbar in all eight Light language and font-size combinations.
- The replacement attachment sheet and the Tags add/remove flow produced all eight Light captures. The formatting tests applied Bold and custom HTML, switched between all three layouts, and inserted and repeated a cloze in the same eight combinations. The drawing screen captured all eight with its Foundation app bar and accessibility checks.
- On the Pixel 9 AVD with the Fork debug APK, Add left a back-only draft open and displayed “The first field is empty.” On the same draft, the physical toolbar applied bold and kept the keyboard available. [Empty-front warning](../../../docs/redesign/finding-and-adding/editor-empty-front-pixel9.png)
- The Back field received a saved drawing stroke as WebP, a recorded 3GP clip, the selected gallery PNG and a camera JPG. Add saved that rich Note and cleared the fields. [Attachment choices](../../../docs/redesign/finding-and-adding/editor-attach-pixel9.png) · [Drawing](../../../docs/redesign/finding-and-adding/editor-drawing-pixel9.png) · [Recorded clip](../../../docs/redesign/finding-and-adding/editor-recording-pixel9.png) · [Rich draft](../../../docs/redesign/finding-and-adding/editor-rich-note-pixel9.png)

The final 168 rendered cases passed across all three themes, four languages and both font sizes, including 24 drawing cases and 144 editor cases.
