// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.reviewreminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.preferences.SettingsChevron
import com.ichi2.anki.preferences.SettingsDivider
import com.ichi2.anki.preferences.SettingsRow
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogTitle
import com.ichi2.anki.common.android.R as CommonR

@Composable
internal fun ReminderEditor(
    draft: ReminderDraft,
    deckName: String?,
    editing: Boolean,
    onDraftChange: (ReminderDraft) -> Unit,
    onTime: () -> Unit,
    onDeck: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val paper = MaterialTheme.foundation
    val threshold = draft.validThreshold
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = paper.colors.background,
            contentColor = paper.colors.onBackground,
            border = BorderStroke(paper.shapes.hairline, paper.colors.outline),
        ) {
            Column(
                Modifier.padding(horizontal = paper.spacing.screenMargin).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReminderEditorHandle(onDismiss)
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DialogTitle(stringResource(if (editing) CommonString.edit_review_reminder else CommonString.add_review_reminder))
                    Column(Modifier.padding(top = 6.dp)) {
                        SettingsDivider()
                        SettingsRow(title = stringResource(R.string.review_reminder_time), onClick = onTime) {
                            Text(
                                draft.time.toFormattedString(LocalContext.current),
                                style = paper.type.body,
                                color = paper.colors.onBackgroundMuted,
                            )
                            Icon(
                                painterResource(R.drawable.symbol_schedule),
                                contentDescription = null,
                                tint = paper.colors.onBackgroundMuted,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        SettingsRow(title = stringResource(R.string.review_reminder_deck), onClick = onDeck) {
                            deckName?.let {
                                Text(
                                    it,
                                    Modifier.widthIn(max = 150.dp),
                                    style = paper.type.body,
                                    color = paper.colors.onBackgroundMuted,
                                    textAlign = TextAlign.End,
                                )
                            }
                            SettingsChevron()
                        }
                    }
                    Text(
                        stringResource(R.string.review_reminder_threshold),
                        Modifier.padding(top = 4.dp),
                        style = paper.type.label,
                        color = paper.colors.onBackgroundMuted,
                    )
                    ReminderThreshold(draft.threshold, threshold) { onDraftChange(draft.copy(threshold = it)) }
                    ReminderCountFilters(draft.filter) { onDraftChange(draft.copy(filter = it)) }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = draft.onlyIfNotStudied,
                                role = Role.Checkbox,
                                onValueChange = { onDraftChange(draft.copy(onlyIfNotStudied = it)) },
                            ).heightIn(min = paper.spacing.touchTarget),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Checkbox(checked = draft.onlyIfNotStudied, onCheckedChange = null)
                        Text(stringResource(R.string.review_reminder_only_if_unstudied), style = paper.type.body)
                    }
                    if (editing) {
                        TextButton(onClick = onDelete) {
                            Text(
                                stringResource(CommonString.dialog_positive_delete),
                                style = paper.type.button,
                                color = paper.colors.emphasis,
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).heightIn(min = paper.spacing.secondaryButton),
                        shape = paper.shapes.button,
                        border = BorderStroke(paper.shapes.hairline, paper.colors.outline),
                    ) {
                        Text(stringResource(CommonString.dialog_cancel), style = paper.type.button, textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = onSave,
                        enabled = threshold != null && deckName != null,
                        modifier = Modifier.weight(1f).heightIn(min = paper.spacing.primaryButton),
                        shape = paper.shapes.button,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = paper.colors.primary,
                                contentColor = paper.colors.onPrimary,
                            ),
                    ) {
                        Text(stringResource(CommonString.save), style = paper.type.button, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderEditorHandle(onDismiss: () -> Unit) {
    val paper = MaterialTheme.foundation
    val dismissDistance = with(LocalDensity.current) { 48.dp.toPx() }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 20.dp)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { dragDistance += it },
                onDragStarted = { dragDistance = 0f },
                onDragStopped = { if (dragDistance > dismissDistance) onDismiss() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(width = 32.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(paper.colors.outline))
    }
}

@Composable
private fun ReminderThreshold(
    input: String,
    value: Int?,
    onChange: (String) -> Unit,
) {
    val paper = MaterialTheme.foundation
    val countLabel = stringResource(R.string.review_reminder_card_count)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.review_reminder_at_least), style = paper.type.body)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(((value ?: 1) - 1).toString()) }, enabled = value != 0) {
                Icon(
                    painterResource(CommonR.drawable.symbol_remove),
                    contentDescription = stringResource(R.string.review_reminder_decrease),
                )
            }
            OutlinedTextField(
                value = input,
                onValueChange = onChange,
                modifier = Modifier.widthIn(min = 74.dp, max = 126.dp).semantics { contentDescription = countLabel },
                singleLine = true,
                textStyle = paper.type.body.copy(textAlign = TextAlign.Center),
                isError = value == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            IconButton(onClick = { onChange(((value ?: 0) + 1).toString()) }, enabled = value != Int.MAX_VALUE) {
                Icon(painterResource(CommonR.drawable.symbol_add), contentDescription = stringResource(R.string.review_reminder_increase))
            }
        }
        Text(stringResource(R.string.review_reminder_cards), style = paper.type.body)
    }
    if (value == null) {
        Text(
            stringResource(
                if ((input.toIntOrNull() ?: 0) < 0) {
                    CommonString.add_edit_reminder_card_threshold_error_negative
                } else {
                    CommonString.add_edit_reminder_card_threshold_error_not_a_number
                },
            ),
            color = paper.colors.emphasis,
            style = paper.type.caption,
        )
    }
}

@Composable
private fun ReminderCountFilters(
    filter: ReviewReminderThresholdFilter,
    onChange: (ReviewReminderThresholdFilter) -> Unit,
) {
    val paper = MaterialTheme.foundation
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.review_reminder_count_these), style = paper.type.caption, color = paper.colors.onBackgroundMuted)
        ReminderCountChip(stringResource(R.string.review_reminder_new), filter.countNew) {
            onChange(filter.copy(countNew = !filter.countNew))
        }
        ReminderCountChip(stringResource(R.string.review_reminder_learning), filter.countLrn) {
            onChange(filter.copy(countLrn = !filter.countLrn))
        }
        ReminderCountChip(stringResource(R.string.review_reminder_due), filter.countRev) {
            onChange(filter.copy(countRev = !filter.countRev))
        }
    }
}

@Composable
private fun ReminderCountChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val paper = MaterialTheme.foundation
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = paper.type.caption) },
        leadingIcon =
            if (selected) {
                { Icon(painterResource(R.drawable.symbol_check), contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else {
                null
            },
        shape = paper.shapes.button,
        border = BorderStroke(paper.shapes.hairline, if (selected) paper.colors.onBackground else paper.colors.outline),
    )
}
