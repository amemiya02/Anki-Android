// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.compose.ui.platform.ComposeView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentContainerView
import com.ichi2.anki.common.utils.android.getColorFromAttr
import com.ichi2.anki.noteeditor.Toolbar as EditorToolbar
import com.ichi2.anki.ui.ResizablePaneLayout
import com.ichi2.anki.ui.ResizingDivider
import com.ichi2.anki.common.android.R as CommonR

/** Keeps the system text editing and IME behavior of the note fields. */
internal class NoteFieldsLayout(
    context: Context,
) {
    val rootLayout = CoordinatorLayout(context).apply { id = R.id.root_layout }
    val root: View get() = rootLayout
    val noteEditorLayout = ScrollView(context).apply { id = R.id.note_editor_layout }
    val editorSelectors = ComposeView(context).apply { id = R.id.editor_selectors }
    val editorDetails = ComposeView(context).apply { id = R.id.editor_details }
    val editorToolbar = EditorToolbar(context, null).apply { id = R.id.editor_toolbar }
    val toolbarLayoutDialog = ComposeView(context).apply { id = R.id.toolbar_layout_dialog }

    init {
        val fields =
            LinearLayout(context).apply {
                id = R.id.CardEditorEditFieldsLayout
                orientation = LinearLayout.VERTICAL
                setPadding(0, context.editorDp(14), 0, 0)
            }
        val column =
            LinearLayout(context).apply {
                id = R.id.CardEditorLayout
                orientation = LinearLayout.VERTICAL
                addView(editorSelectors, MATCH_PARENT, WRAP_CONTENT)
                addView(fields, MATCH_PARENT, WRAP_CONTENT)
                addView(editorDetails, MATCH_PARENT, WRAP_CONTENT)
            }
        val margin = context.resources.getDimensionPixelSize(CommonR.dimen.foundation_spacing_screen_margin)
        noteEditorLayout.setPadding(margin, 0, margin, 0)
        noteEditorLayout.addView(column, MATCH_PARENT, WRAP_CONTENT)
        rootLayout.addView(
            noteEditorLayout,
            CoordinatorLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.note_editor_toolbar_height)
            },
        )
        rootLayout.addView(editorToolbar, CoordinatorLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { gravity = Gravity.BOTTOM })
        rootLayout.addView(toolbarLayoutDialog, WRAP_CONTENT, WRAP_CONTENT)
    }
}

/** The editor's shared app bar and optional resizable live preview. */
internal class NoteEditorPaneLayout(
    context: Context,
) {
    val root = CoordinatorLayout(context).apply { id = R.id.root_layout }
    val toolbarContainer = FrameLayout(context).apply { id = R.id.toolbar_container }
    val editorAppBar = ComposeView(context).apply { id = R.id.editor_app_bar }
    val noteEditorFragmentFrame = FragmentContainerView(context).apply { id = R.id.note_editor_fragment_frame }
    val noteEditorXlView: ResizablePaneLayout?
    val noteEditorResizingDivider: ResizingDivider?
    val previewerFrameLayout: LinearLayout?
    val previewerFrame: FragmentContainerView?

    init {
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        toolbarContainer.setBackgroundColor(getColorFromAttr(context, CommonR.attr.foundationBackground))
        toolbarContainer.addView(editorAppBar, MATCH_PARENT, WRAP_CONTENT)
        column.addView(toolbarContainer, MATCH_PARENT, WRAP_CONTENT)
        if (context.resources.configuration.smallestScreenWidthDp >= 600) {
            val panes =
                ResizablePaneLayout(context).apply {
                    id = R.id.note_editor_xl_view
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundColor(getColorFromAttr(context, android.R.attr.colorBackground))
                }
            val divider = ResizingDivider(context).apply { id = R.id.note_editor_resizing_divider }
            val preview = FragmentContainerView(context).apply { id = R.id.previewer_frame }
            val previewPane =
                LinearLayout(context).apply {
                    id = R.id.previewer_frame_layout
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(getColorFromAttr(context, CommonR.attr.colorSurfaceDim))
                    addView(preview, MATCH_PARENT, MATCH_PARENT)
                }
            panes.addView(noteEditorFragmentFrame, LinearLayout.LayoutParams(0, MATCH_PARENT, 1f))
            panes.addView(divider, context.editorDp(8), MATCH_PARENT)
            panes.addView(previewPane, LinearLayout.LayoutParams(0, MATCH_PARENT, 1f))
            column.addView(panes, MATCH_PARENT, MATCH_PARENT)
            noteEditorXlView = panes
            noteEditorResizingDivider = divider
            previewerFrameLayout = previewPane
            previewerFrame = preview
        } else {
            column.addView(noteEditorFragmentFrame, MATCH_PARENT, MATCH_PARENT)
            noteEditorXlView = null
            noteEditorResizingDivider = null
            previewerFrameLayout = null
            previewerFrame = null
        }
        root.addView(column, MATCH_PARENT, MATCH_PARENT)
    }
}

private fun Context.editorDp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
