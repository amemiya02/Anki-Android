// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import android.os.Parcelable
import android.text.InputType
import android.util.SparseArray
import android.view.ActionMode
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.isVisible
import com.ichi2.anki.common.android.themes.interfaceTypeface
import com.ichi2.anki.common.utils.android.getColorFromAttr
import com.ichi2.anki.common.utils.annotation.KotlinCleanup
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.noteeditor.NoteEditorFieldFooter
import com.ichi2.anki.noteeditor.NoteEditorFieldHeader
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.ui.AnimationUtil.collapseView
import com.ichi2.ui.AnimationUtil.expandView
import kotlinx.parcelize.Parcelize
import java.util.Locale
import com.ichi2.anki.common.android.R as CommonR

@KotlinCleanup("replace _name with `field`")
class FieldEditLine(
    context: Context,
) : LinearLayout(context) {
    val editText = FieldEditText(context)
    private val fieldHeader = ComposeView(context)
    private val fieldFooter = ComposeView(context)

    private var _name by mutableStateOf<String?>(null)
    private var expansionState by mutableStateOf(ExpansionState.EXPANDED)
    var sticky by mutableStateOf<Boolean?>(null)
    var focused by mutableStateOf(false)
    var duplicate by mutableStateOf(false)

    var enableAnimation = true

    init {
        orientation = VERTICAL
        editText.apply {
            id = generateViewId()
            background = null
            gravity = Gravity.START or Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minHeight = 48.dp.toPx(context)
            setPadding(0, 6.dp.toPx(context), 0, 9.dp.toPx(context))
            setTextAppearance(CommonR.style.TextAppearance_Foundation_EditorField)
            setTextColor(getColorFromAttr(context, CommonR.attr.foundationOnBackground))
            typeface = context.interfaceTypeface()
            init()
        }
        addView(fieldHeader, MATCH_PARENT, WRAP_CONTENT)
        addView(editText, MATCH_PARENT, WRAP_CONTENT)
        addView(fieldFooter, MATCH_PARENT, WRAP_CONTENT)
    }

    fun bindPresentation(
        onStickyToggle: () -> Unit,
        onAttach: () -> Unit,
        onShowDuplicates: () -> Unit,
    ) {
        fieldHeader.setContent {
            AnkiDroidTheme {
                NoteEditorFieldHeader(
                    label = name.orEmpty(),
                    expanded = expansionState == ExpansionState.EXPANDED,
                    sticky = sticky,
                    onExpandToggle = ::toggleExpansionState,
                    onStickyToggle = onStickyToggle,
                    onAttach = onAttach,
                )
            }
        }
        fieldFooter.setContent {
            AnkiDroidTheme {
                if (expansionState == ExpansionState.EXPANDED) {
                    NoteEditorFieldFooter(focused, duplicate, onShowDuplicates)
                }
            }
        }
    }

    private fun toggleExpansionState() {
        expansionState =
            when (expansionState) {
                ExpansionState.EXPANDED -> {
                    collapseView(editText, enableAnimation)
                    if (editText.isFocused) editText.clearFocus()
                    ExpansionState.COLLAPSED
                }
                ExpansionState.COLLAPSED -> {
                    expandView(editText, enableAnimation)
                    ExpansionState.EXPANDED
                }
            }
    }

    fun setActionModeCallbacks(callback: ActionMode.Callback?) {
        editText.customSelectionActionModeCallback = callback
        editText.customInsertionActionModeCallback = callback
    }

    fun setHintLocale(hintLocale: Locale?) {
        if (hintLocale != null) {
            editText.setHintLocale(hintLocale)
        }
    }

    fun setContent(
        content: String?,
        replaceNewline: Boolean,
    ) {
        editText.setContent(content, replaceNewline)
    }

    fun setOrd(i: Int) {
        editText.ord = i
    }

    var name: String?
        get() = _name
        set(name) {
            _name = name
            editText.contentDescription = name
        }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>) {
        dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
        dispatchThawSelfOnly(container)
    }

    // keeps platform defined nullability
    @Suppress("RedundantNullableReturnType")
    public override fun onSaveInstanceState(): Parcelable? =
        SavedState(
            state = super.onSaveInstanceState(),
            name = _name,
            content = editText.text?.toString(),
            ord = editText.ord,
            expansionState = expansionState,
            selectionStart = editText.selectionStart,
            selectionEnd = editText.selectionEnd,
        )

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        name = state.name
        setContent(state.content, replaceNewline = false)
        // put the cursor/selection back where it was, ignoring stale out of range values
        val textLength = editText.text?.length ?: 0
        if (state.selectionStart in 0..textLength && state.selectionEnd in 0..textLength) {
            editText.setSelection(state.selectionStart, state.selectionEnd)
        }
        editText.ord = state.ord
        expansionState = state.expansionState
        editText.isVisible = expansionState == ExpansionState.EXPANDED
    }

    @Parcelize
    internal class SavedState(
        val state: Parcelable?,
        val name: String?,
        val content: String?,
        val ord: Int,
        val expansionState: ExpansionState,
        val selectionStart: Int,
        val selectionEnd: Int,
    ) : BaseSavedState(state)

    enum class ExpansionState {
        EXPANDED,
        COLLAPSED,
    }
}
