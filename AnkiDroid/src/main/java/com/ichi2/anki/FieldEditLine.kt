// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.ActionMode
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.isVisible
import com.ichi2.anki.common.android.themes.interfaceTypeface
import com.ichi2.anki.common.utils.annotation.KotlinCleanup
import com.ichi2.anki.databinding.ViewCardMultimediaEditlineBinding
import com.ichi2.anki.noteeditor.NoteEditorFieldFooter
import com.ichi2.anki.noteeditor.NoteEditorFieldHeader
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.ui.AnimationUtil.collapseView
import com.ichi2.ui.AnimationUtil.expandView
import kotlinx.parcelize.Parcelize
import java.util.Locale

@KotlinCleanup("replace _name with `field`")
class FieldEditLine : FrameLayout {
    val binding = ViewCardMultimediaEditlineBinding.inflate(LayoutInflater.from(context), this, true)
    private var _name by mutableStateOf<String?>(null)
    private var expansionState by mutableStateOf(ExpansionState.EXPANDED)
    var sticky by mutableStateOf<Boolean?>(null)
    var focused by mutableStateOf(false)
    var duplicate by mutableStateOf(false)

    var enableAnimation = true

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        binding.editText.apply {
            id = generateViewId()
            init()
            typeface = context.interfaceTypeface()
        }
    }

    fun bindPresentation(
        onStickyToggle: () -> Unit,
        onAttach: () -> Unit,
        onShowDuplicates: () -> Unit,
    ) {
        binding.fieldHeader.setContent {
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
        binding.fieldFooter.setContent {
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
                    collapseView(binding.editText, enableAnimation)
                    if (binding.editText.isFocused) binding.editText.clearFocus()
                    ExpansionState.COLLAPSED
                }
                ExpansionState.COLLAPSED -> {
                    expandView(binding.editText, enableAnimation)
                    ExpansionState.EXPANDED
                }
            }
    }

    fun setActionModeCallbacks(callback: ActionMode.Callback?) {
        binding.editText.customSelectionActionModeCallback = callback
        binding.editText.customInsertionActionModeCallback = callback
    }

    fun setHintLocale(hintLocale: Locale?) {
        if (hintLocale != null) {
            binding.editText.setHintLocale(hintLocale)
        }
    }

    fun setContent(
        content: String?,
        replaceNewline: Boolean,
    ) {
        binding.editText.setContent(content, replaceNewline)
    }

    fun setOrd(i: Int) {
        binding.editText.ord = i
    }

    var name: String?
        get() = _name
        set(name) {
            _name = name
            binding.editText.contentDescription = name
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
            content = binding.editText.text?.toString(),
            ord = binding.editText.ord,
            expansionState = expansionState,
            selectionStart = binding.editText.selectionStart,
            selectionEnd = binding.editText.selectionEnd,
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
        val textLength = binding.editText.text?.length ?: 0
        if (state.selectionStart in 0..textLength && state.selectionEnd in 0..textLength) {
            binding.editText.setSelection(state.selectionStart, state.selectionEnd)
        }
        binding.editText.ord = state.ord
        expansionState = state.expansionState
        binding.editText.isVisible = expansionState == ExpansionState.EXPANDED
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
