// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.compose.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ichi2.anki.common.android.R
import com.ichi2.compose.theme.foundation

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirm: DialogAction,
    onDismissRequest: () -> Unit,
    dismissText: String? = stringResource(R.string.dialog_cancel),
    @DrawableRes markIcon: Int? = null,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    val destructive = confirm.style == DialogActionStyle.Destructive
    val mark =
        if (destructive) {
            DialogMark(R.drawable.symbol_warning, warning = true)
        } else {
            DialogMark(markIcon ?: R.drawable.symbol_info)
        }
    PaperDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        actions =
            listOfNotNull(
                dismissText?.let { DialogAction(it, onDismissRequest) },
                confirm.copy(style = if (destructive) DialogActionStyle.Destructive else DialogActionStyle.Filled),
            ),
        mark = mark,
        properties = properties,
    ) {
        if (message.isNotEmpty()) DialogBody(message)
        extraContent()
    }
}

data class DialogChoice(
    val label: String,
    val description: String? = null,
    val enabled: Boolean = true,
    @DrawableRes val icon: Int? = null,
)

@Composable
fun SingleChoiceDialog(
    title: String,
    choices: List<DialogChoice>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    hint: String? = null,
) {
    PaperDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        actions = listOf(DialogAction(stringResource(R.string.dialog_cancel), onDismissRequest)),
    ) {
        Column(Modifier.selectableGroup()) {
            choices.forEachIndexed { index, choice ->
                DialogChoiceRow(choice, index == selectedIndex, { onSelect(index) }, multiple = false)
            }
        }
        if (hint != null) DialogDetail(hint)
    }
}

@Composable
fun MultipleChoiceDialog(
    title: String,
    choices: List<DialogChoice>,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    confirm: DialogAction,
    onDismissRequest: () -> Unit,
    hint: String? = null,
) {
    PaperDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        actions =
            listOf(
                DialogAction(stringResource(R.string.dialog_cancel), onDismissRequest),
                confirm.copy(style = DialogActionStyle.Filled),
            ),
    ) {
        if (hint != null) DialogDetail(hint)
        Column {
            choices.forEachIndexed { index, choice ->
                DialogChoiceRow(choice, index in selected, { onToggle(index) }, multiple = true)
            }
        }
    }
}

@Composable
fun DialogChoiceRow(
    choice: DialogChoice,
    checked: Boolean,
    onClick: () -> Unit,
    multiple: Boolean,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    val select =
        if (multiple) {
            Modifier.toggleable(checked, enabled = choice.enabled, role = Role.Checkbox, onValueChange = { onClick() })
        } else {
            Modifier.selectable(checked, enabled = choice.enabled, role = Role.RadioButton, onClick = onClick)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .then(select)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (multiple) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = choice.enabled,
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = colors.primary,
                        uncheckedColor = colors.onBackgroundMuted,
                        checkmarkColor = colors.onPrimary,
                    ),
            )
        } else {
            RadioButton(
                selected = checked,
                onClick = null,
                enabled = choice.enabled,
                colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.onBackgroundMuted),
            )
        }
        if (choice.icon != null) {
            Icon(painterResource(choice.icon), contentDescription = null, modifier = Modifier.size(24.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(choice.label, style = foundation.type.lead, color = if (choice.enabled) colors.onBackground else colors.onBackgroundMuted)
            if (choice.description != null) DialogDetail(choice.description)
        }
    }
}

@Composable
fun ProgressDialog(
    title: String,
    message: String? = null,
    progress: Float? = null,
    status: String? = null,
    note: String? = null,
    onCancel: (() -> Unit)? = null,
    cancelText: String = stringResource(R.string.dialog_cancel),
) {
    val cancelable = onCancel != null
    PaperDialog(
        title = title,
        onDismissRequest = onCancel ?: {},
        properties = DialogProperties(dismissOnBackPress = cancelable, dismissOnClickOutside = cancelable, usePlatformDefaultWidth = false),
        actions = if (onCancel == null) emptyList() else listOf(DialogAction(cancelText, onCancel)),
    ) {
        if (message != null) DialogBody(message)
        val colors = MaterialTheme.foundation.colors
        val bar = Modifier.fillMaxWidth().height(3.dp)
        if (progress == null) {
            LinearProgressIndicator(
                modifier = bar,
                color = colors.progressFill,
                trackColor = colors.progressTrack,
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp,
            )
        } else {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = bar,
                color = colors.progressFill,
                trackColor = colors.progressTrack,
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
        if (status != null) DialogDetail(status)
        if (note != null) DialogDetail(note)
    }
}

@Composable
fun ErrorDialog(
    title: String,
    message: String,
    details: String? = null,
    onDismissRequest: () -> Unit,
    primaryAction: DialogAction? = null,
    dismissText: String? = stringResource(R.string.close),
    @DrawableRes markIcon: Int? = null,
) {
    PaperDialog(
        title = title,
        mark = DialogMark(markIcon ?: R.drawable.symbol_info, warning = true),
        onDismissRequest = onDismissRequest,
        actions =
            listOfNotNull(
                dismissText?.let { DialogAction(it, onDismissRequest) },
                primaryAction?.copy(style = DialogActionStyle.Filled),
            ),
    ) {
        DialogBody(message)
        if (!details.isNullOrEmpty()) ErrorDetails(details)
    }
}

@Composable
fun ErrorDetails(details: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.foundation.colors
    val button = if (expanded) R.string.paper_dialog_hide_details else R.string.paper_dialog_show_details
    Row(
        modifier =
            Modifier
                .heightIn(min = MaterialTheme.foundation.spacing.touchTarget)
                .clickable(role = Role.Button, onClick = { expanded = !expanded }),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(if (expanded) R.drawable.symbol_expand_more else R.drawable.symbol_chevron_right),
            null,
            modifier = Modifier.size(20.dp),
            tint = colors.onBackgroundMuted,
        )
        Text(stringResource(button), color = colors.onBackgroundMuted)
    }
    if (expanded) {
        val clipboard = LocalContext.current.getSystemService(ClipboardManager::class.java)
        val label = stringResource(R.string.paper_dialog_copy_details)
        SelectionContainer { DialogDetail(details) }
        TextButton(onClick = { clipboard.setPrimaryClip(ClipData.newPlainText(label, details)) }) {
            Text(label)
        }
    }
}
