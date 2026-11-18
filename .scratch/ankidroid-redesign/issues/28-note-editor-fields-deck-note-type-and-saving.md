# 28: Note editor: fields, Deck, Note Type and saving

**What to build:** The owner adds or edits a Note in a redesigned note editor where the fields are the focus.

**Blocked by:** 25 (Mockups for finding and adding)

**Status:** ready-for-agent

- [x] A Note can be added to a chosen Deck with a chosen Note Type, and an existing Note can be edited.
- [ ] Fields follow the Note Type, including after the Note Type is changed while editing.
- [x] Leaving with unsaved changes asks before discarding them.
- [x] Text shared from another app arrives in the fields.
- [ ] Duplicate and empty-field warnings appear as they do today.
- [x] Chinese and Japanese input works in every field.
- [x] On a tablet-sized screen the surface still works in its existing layout.
- [x] Add/Save sits where the owner decides in the Mockup (recommended: the top bar); the duplicate warning is written under the first field.
- [x] It matches the agreed Mockup.
- [ ] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [ ] Screen-logic tests and Engine tests still pass unchanged.


## Implementation

The Compose top bar carries Add or Save, Preview and the editor menu. The form pairs the Note Type and Deck selectors above the ruled fields. The existing Android editable text controls keep the input connection, language hints, selections, and media paste and drop. The field controls retain the keep pin, attachments and expansion. The tablet preview pane and the Card Browser's editor pane stay available.

## Verification

- The rendered duplicate-action test failed on the original editor because Show Duplicates was missing. After the change it passed the eight Light combinations of English, Simplified Chinese, Japanese and Arabic at normal and 200% font size. The accessibility scan also found 44dp formatting controls. Built-in and custom controls now use the 48dp Foundation target.
- On the Android 16 Pixel 9 emulator, Add cleared both fields after saving the Note, Show Duplicates opened the one matching Card, and reopening after Save showed the updated Back field. The discard prompt kept the edit when Keep editing was selected. The disposable test Note was `T28Smoke20261118` in Anatomy. [Device capture](../../../docs/redesign/finding-and-adding/editor-duplicate-pixel9.png)
- The emulator APK was `com.ichi2.anki.redesign.debug`, arm64-v8a, built on base `0691eb8ee3`. Its SHA-256 was `1d7ff4fd037efd1681e36c2176b0e0162f141bcd5e1d27543520e43eeb1b629e`.
- Sixteen focused Light cases passed for selection, saving and the tablet layout with IME insets. The test selected a Deck and Cloze Note Type, composed and committed Japanese and Chinese text into its two fields through Android's input connection, and saved through Add. A separate tablet case rendered the existing preview pane while the keyboard inset was applied.
- The final rendered matrix also opens the Engine in the selected interface locale so its field names and actions match the Android resources. The Dark and Black runs and the complete screen-logic run are tracked separately from the focused checks above.
