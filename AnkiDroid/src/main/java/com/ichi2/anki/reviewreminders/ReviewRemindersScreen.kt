// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.reviewreminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.preferences.SettingsChevron
import com.ichi2.anki.preferences.SettingsDivider
import com.ichi2.anki.preferences.SettingsRow
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.compose.theme.foundation
import com.ichi2.utils.LanguageUtil.withUnicodeIsolation
import androidx.appcompat.R as AppCompatR
import com.ichi2.anki.common.android.R as CommonR

internal data class ReminderRow(
    val reminder: ReviewReminder,
    val deckName: String?,
) {
    val deletedDeck get() = reminder.scope is ReviewReminderScope.DeckSpecific && deckName == null
}

@Composable
internal fun ReminderPage(
    onNavigateUp: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val paper = MaterialTheme.foundation
    Column(
        Modifier
            .fillMaxSize()
            .background(paper.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = paper.spacing.screenMargin),
    ) {
        if (onNavigateUp != null) {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    painterResource(CommonR.drawable.symbol_arrow_back),
                    contentDescription = stringResource(AppCompatR.string.abc_action_bar_up_description),
                    modifier = Modifier.scale(scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f, scaleY = 1f),
                    tint = paper.colors.onBackground,
                )
            }
        }
        content()
    }
}

@Composable
internal fun ReminderPageTitle(text: String) {
    val paper = MaterialTheme.foundation
    Text(
        text,
        Modifier.padding(top = 4.dp, bottom = 12.dp).semantics { heading() },
        style = paper.type.screenTitle,
        color = paper.colors.onBackground,
    )
}

@Composable
internal fun ReviewRemindersScreen(
    rows: List<ReminderRow>,
    deckName: String?,
    onNavigateUp: (() -> Unit)?,
    showWarning: Boolean,
    snackbar: SnackbarHostState,
    onAdd: () -> Unit,
    onEdit: (ReviewReminder) -> Unit,
    onToggle: (ReviewReminder, Boolean) -> Unit,
    onCheck: () -> Unit,
    onSettings: () -> Unit,
) {
    val paper = MaterialTheme.foundation
    // At large text sizes the action needs its own space below multiline settings rows.
    val reserveAddButton = LocalDensity.current.fontScale > 1f
    ReminderPage(onNavigateUp) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = if (reserveAddButton) 76.dp else 0.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = if (reserveAddButton) 4.dp else 96.dp),
            ) {
                item {
                    if (onNavigateUp != null) ReminderPageTitle(stringResource(CommonString.review_reminders_title))
                    deckName?.let { Text(it, style = paper.type.rowTitle, color = paper.colors.onBackground) }
                    Text(
                        stringResource(R.string.review_reminders_intro, stringResource(R.string.app_name)),
                        Modifier.padding(top = 4.dp, bottom = 20.dp),
                        style = paper.type.body,
                        color = paper.colors.onBackgroundMuted,
                    )
                    SettingsDivider()
                }
                if (rows.isEmpty()) {
                    item {
                        Text(
                            stringResource(CommonString.you_have_no_review_reminders),
                            Modifier.padding(vertical = 16.dp),
                            style = paper.type.body,
                            color = paper.colors.onBackgroundMuted,
                        )
                        SettingsDivider()
                    }
                }
                items(rows, key = { it.reminder.id.value }) { row ->
                    ReviewReminderRow(row, { onEdit(row.reminder) }) { onToggle(row.reminder, it) }
                }
                item {
                    Spacer(Modifier.height(6.dp))
                    SettingsDivider()
                    SettingsRow(
                        title = stringResource(R.string.review_reminders_check_action),
                        summary =
                            stringResource(
                                if (showWarning) CommonString.schedule_reminders_unavailable else R.string.review_reminders_check_summary,
                            ),
                        icon = R.drawable.symbol_health_and_safety,
                        onClick = onCheck,
                        trailing = { SettingsChevron() },
                    )
                    SettingsRow(
                        title = stringResource(CommonString.schedule_reminders_notification_settings_menu),
                        summary = stringResource(R.string.review_reminders_system_settings_summary),
                        icon = R.drawable.symbol_notifications,
                        onClick = onSettings,
                        trailing = {
                            Icon(
                                painterResource(R.drawable.symbol_open_in_new),
                                contentDescription = null,
                                tint = paper.colors.onBackgroundMuted,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                }
            }
            FloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp),
                shape = paper.shapes.addButton,
                containerColor = paper.colors.primary,
                contentColor = paper.colors.onPrimary,
            ) {
                Icon(painterResource(CommonR.drawable.symbol_add), contentDescription = stringResource(CommonString.add_reminder))
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp))
        }
    }
}

@Composable
private fun ReviewReminderRow(
    row: ReminderRow,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val paper = MaterialTheme.foundation
    val context = LocalContext.current
    val reminder = row.reminder
    val time = reminder.time.toFormattedString(context)
    val active = !row.deletedDeck && reminder.enabled
    val threshold = reminder.cardTriggerThreshold.threshold
    val conditions =
        buildList {
            if (row.deletedDeck) {
                add(stringResource(R.string.review_reminders_deleted_deck))
            } else {
                add(row.deckName?.let(::withUnicodeIsolation) ?: with(context) { TR.sentenceCase.allDecks })
                if (threshold > 1) {
                    add(pluralStringResource(R.plurals.review_reminders_condition_due, threshold, threshold))
                }
                if (reminder.onlyNotifyIfNoReviews) add(stringResource(R.string.review_reminders_condition_unstudied))
            }
        }.joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().heightIn(min = paper.spacing.listRow),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onEdit)
                .padding(vertical = 8.dp),
        ) {
            Text(
                time,
                style = paper.type.pageTitle,
                color = if (active) paper.colors.onBackground else paper.colors.onBackgroundMuted,
                textDecoration = if (row.deletedDeck) TextDecoration.LineThrough else null,
            )
            Text(
                conditions,
                style = paper.type.caption,
                color = if (row.deletedDeck) MaterialTheme.colorScheme.error else paper.colors.onBackgroundMuted,
            )
        }
        val label = stringResource(R.string.review_reminder_switch, time)
        Switch(
            checked = active,
            onCheckedChange = onToggle,
            enabled = !row.deletedDeck,
            modifier = Modifier.semantics { contentDescription = label },
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = paper.colors.onPrimary,
                    checkedTrackColor = paper.colors.primary,
                    uncheckedThumbColor = paper.colors.onBackgroundMuted,
                    uncheckedTrackColor = Color.Transparent,
                    uncheckedBorderColor = paper.colors.onBackgroundMuted,
                ),
        )
    }
    SettingsDivider()
}
