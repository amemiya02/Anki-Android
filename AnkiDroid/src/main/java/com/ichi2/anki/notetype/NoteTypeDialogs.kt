// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.compose.ui.dialogs.TextInputDialog
import java.util.Locale
import androidx.appcompat.R as AppCompatR
import com.ichi2.anki.common.android.R as CommonR

@Composable
internal fun AddNoteTypeSheet(
    options: List<AddNotetypeUiModel>,
    names: List<String>,
    onAdd: (AddNotetypeUiModel, String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (options.isEmpty()) return
    val paper = MaterialTheme.foundation
    var choice by rememberSaveable { mutableIntStateOf(0) }
    val active = options.getOrElse(choice) { options.first() }
    var name by rememberSaveable { mutableStateOf(suggestedName(active.name, names)) }
    val newName = name.trim()
    val duplicate = names.any { it.equals(newName, ignoreCase = true) }

    NoteTypeSheet(stringResource(R.string.note_type_add_title), onDismiss) {
        NoteTypeCaption(stringResource(R.string.note_type_add_lead))
        NoteTypeGroup(
            name = stringResource(R.string.note_type_standard),
            options = options.filter { it.isStandard },
            active = active,
        ) { item ->
            choice = options.indexOf(item)
            name = suggestedName(item.name, names)
        }
        NoteTypeGroup(
            name = stringResource(R.string.note_type_clone),
            options = options.filterNot { it.isStandard },
            active = active,
        ) { item ->
            choice = options.indexOf(item)
            name = suggestedName(item.name, names)
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(TR.deckConfigNamePrompt()) },
            singleLine = true,
            isError = duplicate,
            supportingText = if (duplicate) ({ Text(stringResource(CommonString.error_name_exists)) }) else null,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(CommonString.dialog_cancel))
            }
            Button(
                onClick = { onAdd(active, newName) },
                enabled = newName.isNotEmpty() && !duplicate,
                shape = paper.shapes.button,
                colors = ButtonDefaults.buttonColors(containerColor = paper.colors.primary, contentColor = paper.colors.onPrimary),
            ) {
                Text(stringResource(CommonString.dialog_add))
            }
        }
    }
}

private fun suggestedName(
    base: String,
    names: List<String>,
): String = generateSequence(2) { it + 1 }.map { "$base $it" }.first { candidate -> names.none { it.equals(candidate, ignoreCase = true) } }

@Composable
private fun NoteTypeGroup(
    name: String,
    options: List<AddNotetypeUiModel>,
    active: AddNotetypeUiModel,
    onSelect: (AddNotetypeUiModel) -> Unit,
) {
    if (options.isEmpty()) return
    val paper = MaterialTheme.foundation
    Text(name, Modifier.padding(top = 12.dp).semantics { heading() }, style = paper.type.label)
    FlowRow(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == active,
                onClick = { onSelect(option) },
                label = { Text(option.name, Modifier.padding(vertical = 4.dp), style = paper.type.caption) },
                modifier = Modifier.heightIn(min = paper.spacing.touchTarget),
                shape = paper.shapes.button,
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = paper.colors.primary,
                        selectedLabelColor = paper.colors.onPrimary,
                    ),
            )
        }
    }
}

@Composable
internal fun FieldActionsSheet(
    row: NoteFieldRow,
    onDismiss: () -> Unit,
    onAction: (FieldAction) -> Unit,
) {
    NoteTypeSheet(row.field.name, onDismiss) {
        val rename = stringResource(CommonString.model_field_editor_rename)
        val move = stringResource(R.string.note_type_field_move)
        val sort = stringResource(R.string.note_type_field_sort_action)
        val keyboard = stringResource(R.string.note_type_field_keyboard_action)
        val delete = stringResource(CommonString.model_field_editor_delete)
        NoteTypeDivider()
        FieldActionRow(rename, R.drawable.symbol_edit, onClick = { onAction(FieldAction.Rename) })
        FieldActionRow(move, R.drawable.symbol_sort, onClick = { onAction(FieldAction.Move) })
        FieldActionRow(
            sort,
            R.drawable.symbol_sort,
            subtitle = stringResource(R.string.note_type_field_sort_description),
            onClick = { onAction(FieldAction.Sort) },
        )
        FieldActionRow(
            keyboard,
            R.drawable.symbol_keyboard,
            subtitle = row.keyboard?.getDisplayName(row.keyboard) ?: stringResource(R.string.note_type_field_no_keyboard),
            onClick = { onAction(FieldAction.Keyboard) },
        )
        FieldActionRow(
            delete,
            CommonR.drawable.symbol_delete,
            subtitle = stringResource(R.string.note_type_field_delete_description),
            emphasized = true,
            onClick = { onAction(FieldAction.Delete) },
        )
    }
}

@Composable
private fun FieldActionRow(
    title: String,
    icon: Int,
    subtitle: String? = null,
    emphasized: Boolean = false,
    onClick: () -> Unit,
) {
    NoteTypeRow(
        title,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        leading = { Icon(painterResource(icon), contentDescription = null) },
        subtitle = { subtitle?.let { NoteTypeCaption(it) } },
        emphasized = emphasized,
    )
    NoteTypeDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteTypeSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val paper = MaterialTheme.foundation
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = paper.colors.background,
        contentColor = paper.colors.onBackground,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = paper.spacing.screenMargin)
                .padding(bottom = 16.dp),
        ) {
            Text(title, Modifier.semantics { heading() }, style = paper.type.screenTitle)
            content()
        }
    }
}

@Composable
internal fun FieldNameDialog(
    field: NoteFieldRef?,
    fields: List<NoteFieldRow>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val title = if (field == null) with(LocalContext.current) { TR.sentenceCase.addField } else stringResource(CommonString.model_field_editor_rename)
    val emptyName = stringResource(CommonString.toast_empty_name)
    val duplicate = stringResource(CommonString.toast_duplicate_field)
    TextInputDialog(
        title = title,
        label = TR.deckConfigNamePrompt(),
        initialText = field?.name.orEmpty(),
        confirmText = stringResource(if (field == null) CommonString.dialog_add else CommonString.rename),
        onConfirm = { onSave(cleanFieldName(it)) },
        onDismissRequest = onDismiss,
        errorMessageFor = {
            val name = cleanFieldName(it)
            when {
                name.isEmpty() -> emptyName
                name == field?.name -> ""
                fields.any { row -> row.field.name == name } -> duplicate
                else -> null
            }
        },
    )
}

@Composable
internal fun FieldPositionDialog(
    row: NoteFieldRow,
    fields: List<NoteFieldRow>,
    onDismiss: () -> Unit,
    onMove: (Int) -> Unit,
) {
    SingleChoiceDialog(
        title = stringResource(R.string.note_type_field_move),
        choices = fields.mapIndexed { index, field -> DialogChoice((index + 1).toString(), field.field.name) },
        selectedIndex = fields.indexOf(row),
        onSelect = onMove,
        onDismissRequest = onDismiss,
    )
}

@Composable
internal fun FieldKeyboardDialog(
    current: Locale?,
    onDismiss: () -> Unit,
    onSelect: (Locale?) -> Unit,
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val language =
        androidx.core.os.ConfigurationCompat
            .getLocales(config)[0] ?: Locale.getDefault()
    val locales =
        remember(language) {
            val ipa = Locale.forLanguageTag("und-fonipa")
            val collator = java.text.Collator.getInstance(language)
            (Locale.getAvailableLocales().toList() + ipa)
                .distinctBy(Locale::toLanguageTag)
                .map { it to it.label(context, language) }
                .sortedWith(Comparator { left, right -> collator.compare(left.second, right.second) })
        }
    var query by rememberSaveable { mutableStateOf("") }
    val results =
        remember(query, locales) {
            locales.filter { (locale, label) ->
                label.contains(query, ignoreCase = true) ||
                    locale.toLanguageTag().contains(query, ignoreCase = true)
            }
        }
    PaperDialog(
        title = stringResource(R.string.note_type_field_keyboard_action),
        onDismissRequest = onDismiss,
        actions = listOf(DialogAction(stringResource(CommonString.dialog_cancel), onDismiss)),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(AppCompatR.string.abc_search_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(Modifier.heightIn(max = 320.dp).selectableGroup()) {
            if (query.isBlank()) {
                item {
                    DialogChoiceRow(
                        choice = DialogChoice(stringResource(R.string.note_type_field_no_keyboard)),
                        checked = current == null,
                        onClick = { onSelect(null) },
                        multiple = false,
                    )
                }
            }
            items(results, key = { it.first.toLanguageTag() }) { (locale, label) ->
                DialogChoiceRow(
                    choice = DialogChoice(label),
                    checked = current?.toLanguageTag() == locale.toLanguageTag(),
                    onClick = { onSelect(locale) },
                    multiple = false,
                )
            }
        }
    }
}

@Composable
internal fun NoteTypeFullSyncDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    deletingField: Boolean = false,
) {
    ConfirmationDialog(
        title = stringResource(R.string.note_type_full_sync_title),
        message = stringResource(if (deletingField) R.string.note_type_full_sync_field_delete else CommonString.full_sync_confirmation),
        confirm =
            DialogAction(
                label = stringResource(if (deletingField) CommonString.model_field_editor_delete else CommonString.dialog_continue),
                onClick = onConfirm,
                style = if (deletingField) DialogActionStyle.Destructive else DialogActionStyle.Filled,
            ),
        onDismissRequest = onDismiss,
        markIcon = R.drawable.symbol_sync_problem,
    )
}

internal fun Locale.label(
    context: android.content.Context,
    language: Locale = Locale.getDefault(),
): String = getDisplayName(language).ifBlank { context.getString(CommonString.locale_selection_dialog_unknown_language, toLanguageTag()) }
