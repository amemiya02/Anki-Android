// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

/** Compatibility entry point for intents and tests using the former Note Editor component. */
class NoteEditorActivity : SingleFragmentActivity() {
    private val editor: NoteEditorPaneFragment
        get() = fragment as NoteEditorPaneFragment

    val noteEditorFragment: NoteEditorFragment
        get() = editor.noteEditorFragment

    val isPreviewerVisible: Boolean
        get() = editor.isPreviewerVisible

    val hasPreviewerPane: Boolean
        get() = editor.hasPreviewerPane

    internal val binding
        get() = editor.binding

    fun setPreviewerVisible(visible: Boolean) = editor.setPreviewerVisible(visible)

    fun loadNoteEditorPreviewer(forceReplace: Boolean) = editor.loadNoteEditorPreviewer(forceReplace)

    override fun createFragment(): NoteEditorPaneFragment =
        NoteEditorPaneFragment().apply { arguments = NoteEditorPaneFragment.arguments(intent) }
}
