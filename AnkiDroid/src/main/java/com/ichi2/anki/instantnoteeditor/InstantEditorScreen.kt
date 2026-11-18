// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.instantnoteeditor

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.instantnoteeditor.InstantNoteEditorActivity.EditMode
import com.ichi2.anki.preferences.SettingsChevron
import com.ichi2.anki.preferences.SettingsRow
import com.ichi2.anki.preferences.SettingsSectionTitle
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.PaperDialog

internal data class InstantEditorScreenState(
    val deck: String,
    val fields: List<String>,
    val extraValues: List<String>,
    val clozeFieldIndex: Int,
    val clozeText: String,
    val nextClozeNumber: Int,
    val incrementsCloze: Boolean,
    val editMode: EditMode,
    val words: List<InstantEditorWord>,
    val warning: String?,
    val saving: Boolean,
)

internal data class InstantEditorWord(
    val label: String,
    val clozeNumber: Int?,
)

@Composable
internal fun InstantEditorScreen(
    state: InstantEditorScreenState,
    onBack: () -> Unit,
    onPickDeck: () -> Unit,
    onSwitchEditMode: () -> Unit,
    onSwitchClozeMode: () -> Unit,
    onOpenEditor: () -> Unit,
    onClozeText: (String) -> Unit,
    onExtraField: (Int, String) -> Unit,
    onWord: (Int) -> Unit,
    onWordLongPress: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val colors = MaterialTheme.foundation.colors
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val editFullText = state.editMode == EditMode.ADVANCED
    PaperDialog(
        title = stringResource(CommonString.menu_add_note),
        onDismissRequest = onBack,
        actions =
            listOf(
                DialogAction(stringResource(CommonString.menu_add), onSave, DialogActionStyle.Filled, enabled = !state.saving),
            ),
    ) {
        SettingsRow(
            title = stringResource(CommonString.CardEditorNoteDeck).trimEnd(':'),
            value = state.deck,
            onClick = onPickDeck,
            trailing = { SettingsChevron() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InstantEditorTool(
                if (editFullText) R.drawable.symbol_touch_app else R.drawable.symbol_edit,
                CommonString.change_editor_mode,
                onClick = {
                    focus.clearFocus()
                    keyboard?.hide()
                    onSwitchEditMode()
                },
            )
            InstantEditorTool(
                if (state.incrementsCloze) R.drawable.symbol_post_add else R.drawable.symbol_data_array,
                CommonString.change_cloze_mode,
                onClick = onSwitchClozeMode,
            )
            InstantEditorTool(R.drawable.symbol_open_in_new, CommonString.open_note_editor, onOpenEditor)
        }
        if (editFullText) {
            InstantEditorFields(state, onClozeText, onExtraField)
        } else {
            state.fields.getOrNull(state.clozeFieldIndex)?.let { SettingsSectionTitle(it) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.words.forEachIndexed { index, word ->
                    InstantEditorWordChip(word, { onWord(index) }, { onWordLongPress(index) })
                }
            }
        }
        state.warning?.let { Text(it, color = colors.emphasis) }
    }
}

@Composable
private fun InstantEditorTool(
    icon: Int,
    label: Int,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(onClick = onClick) {
        Icon(painterResource(icon), stringResource(label))
    }
}

@Composable
private fun InstantEditorFields(
    state: InstantEditorScreenState,
    onClozeText: (String) -> Unit,
    onExtraField: (Int, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.fields.forEachIndexed { index, name ->
            if (index == state.clozeFieldIndex) {
                InstantEditorClozeField(name, state.clozeText, state.nextClozeNumber, onClozeText)
            } else {
                OutlinedTextField(
                    value = state.extraValues[index],
                    onValueChange = { onExtraField(index, it) },
                    label = { Text(name) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun InstantEditorClozeField(
    name: String,
    text: String,
    nextClozeNumber: Int,
    onText: (String) -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(text)) }
    LaunchedEffect(text) {
        if (value.text != text) value = TextFieldValue(text)
    }
    OutlinedTextField(
        value,
        onValueChange = {
            value = it
            onText(it.text)
        },
        label = { Text(name) },
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(
        modifier = Modifier.heightIn(min = MaterialTheme.foundation.spacing.touchTarget),
        onClick = {
            val selection = value.selection
            val prefix = "{{c$nextClozeNumber::"
            val cloze = prefix + value.text.substring(selection.min, selection.max) + "}}"
            val updated = value.text.replaceRange(selection.min, selection.max, cloze)
            value = TextFieldValue(updated, TextRange(selection.min + prefix.length))
            onText(updated)
        },
    ) {
        Text(stringResource(CommonString.multimedia_editor_popup_cloze))
    }
}

@Composable
private fun InstantEditorWordChip(
    word: InstantEditorWord,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val selected = word.clozeNumber != null
    Row(
        Modifier
            .heightIn(min = foundation.spacing.touchTarget)
            .clip(foundation.shapes.button)
            .border(
                foundation.shapes.hairline,
                if (selected) foundation.colors.emphasis else foundation.colors.outline,
                foundation.shapes.button,
            ).semantics { this.selected = selected }
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick.takeIf { selected })
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(word.label, Modifier.weight(1f, fill = false), color = foundation.colors.onBackground)
        word.clozeNumber?.let { Text(it.toString(), color = foundation.colors.emphasis, style = foundation.type.caption) }
    }
}
