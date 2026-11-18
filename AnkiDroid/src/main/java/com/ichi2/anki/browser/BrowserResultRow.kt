// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.Flag
import com.ichi2.anki.R
import com.ichi2.anki.browser.search.CardState
import com.ichi2.anki.libanki.QueueType
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.compose.theme.FoundationColors
import com.ichi2.compose.theme.foundation
import net.ankiweb.rsdroid.BackendException
import timber.log.Timber
import anki.search.BrowserRow.Color as EngineRowColor

/** Loaded only while the corresponding ID is visible in the browser. */
internal data class BrowserResultRow(
    val title: String,
    val details: String,
    val flags: List<Flag>,
    val marked: Boolean,
    val states: List<CardState>,
)

internal suspend fun loadBrowserResultRow(
    id: CardOrNoteId,
    mode: CardsOrNotes,
    showMediaFilenames: Boolean,
): BrowserResultRow? =
    withCol {
        try {
            val row = backend.browserRowForId(id.cardOrNoteId)
            val cells = row.cellsList.map { cell -> BrowserMultiColumnAdapter.removeSounds(cell.text, showMediaFilenames) }
            val base =
                BrowserResultRow(
                    title = cells.firstOrNull().orEmpty(),
                    details = cells.drop(1).filter(String::isNotBlank).joinToString(" · "),
                    flags = emptyList(),
                    marked = false,
                    states = emptyList(),
                )
            // The Engine's Card colour already distinguishes ordinary, suspended and buried
            // Cards with no other marks. Only a higher-priority colour can hide another mark.
            if (mode == CardsOrNotes.CARDS) {
                when (row.color) {
                    EngineRowColor.COLOR_DEFAULT -> return@withCol base
                    EngineRowColor.COLOR_SUSPENDED -> return@withCol base.copy(states = listOf(CardState.Suspended))
                    EngineRowColor.COLOR_BURIED -> return@withCol base.copy(states = listOf(CardState.Buried))
                    else -> {}
                }
            }
            val cards =
                if (mode == CardsOrNotes.CARDS) {
                    listOf(getCard(id.cardOrNoteId))
                } else {
                    cardIdsOfNote(id.cardOrNoteId).map(::getCard)
                }
            val marked =
                if (mode == CardsOrNotes.NOTES || row.color == EngineRowColor.COLOR_MARKED) {
                    row.color == EngineRowColor.COLOR_MARKED
                } else {
                    cards
                        .single()
                        .note(this)
                        .tags
                        .any { it.equals("marked", ignoreCase = true) }
                }
            base.copy(
                flags =
                    cards
                        .map { Flag.fromCode(it.flags and 7) }
                        .distinct()
                        .filterNot { it == Flag.NONE }
                        .sortedBy(Flag::code),
                marked = marked,
                states =
                    cards
                        .mapNotNull { card ->
                            when (card.queue) {
                                QueueType.Suspended -> CardState.Suspended
                                QueueType.ManuallyBuried, QueueType.SiblingBuried -> CardState.Buried
                                else -> null
                            }
                        }.distinct(),
            )
        } catch (e: BackendException) {
            // An edit or sync can remove this ID before the pending search refresh.
            Timber.d(e, "Browser row no longer available: %s", id)
            null
        }
    }

@Composable
internal fun BrowserResult(
    row: BrowserResultRow?,
    flagNames: Map<Flag, String>,
    isSelected: Boolean,
    isInSelection: Boolean,
    isInPane: Boolean,
    truncated: Boolean,
    fontScale: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    val rightClick by rememberUpdatedState(onRightClick)
    val highlighted = isSelected || isInPane
    val secondaryInk = if (highlighted) colors.onBackground else colors.onBackgroundMuted
    val titleStyle =
        remember(foundation.type.rowTitle, fontScale) {
            val style = foundation.type.rowTitle
            style.copy(fontSize = style.fontSize * fontScale, lineHeight = style.lineHeight * fontScale)
        }
    val detailStyle =
        remember(foundation.type.caption, fontScale) {
            val style = foundation.type.caption
            style.copy(fontSize = style.fontSize * fontScale, lineHeight = style.lineHeight * fontScale)
        }
    Column(modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = foundation.spacing.listRow)
                    .background(if (highlighted) colors.progressTrack else colors.background)
                    .pointerInput(row != null) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (row == null || event.type != PointerEventType.Press || !event.buttons.isSecondaryPressed) continue
                                event.changes.forEach { it.consume() }
                                rightClick()
                            }
                        }
                    }.combinedClickable(
                        enabled = row != null,
                        onClick = onClick,
                        onLongClick = onLongClick,
                        onLongClickLabel =
                            stringResource(
                                if (isInSelection) R.string.browse_select_range else R.string.browse_select_row,
                            ),
                        role = if (isInSelection) Role.Checkbox else null,
                    ).then(
                        if (isInSelection) Modifier.semantics { toggleableState = ToggleableState(isSelected) } else Modifier,
                    ).padding(horizontal = foundation.spacing.screenMargin, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isInSelection) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    colors =
                        CheckboxDefaults.colors(
                            checkedColor = colors.emphasis,
                            checkmarkColor = colors.background,
                            uncheckedColor = secondaryInk,
                        ),
                )
            }
            if (row != null) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = row.title,
                        style = titleStyle,
                        color = if (row.states.isEmpty()) colors.onBackground else secondaryInk,
                        maxLines = if (truncated) 1 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (row.details.isNotEmpty()) {
                        Text(
                            text = row.details,
                            style = detailStyle,
                            color = secondaryInk,
                            maxLines = if (truncated) 1 else Int.MAX_VALUE,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    row.flags.forEach { flag ->
                        Icon(
                            painterResource(R.drawable.symbol_flag_red),
                            contentDescription = flagNames[flag],
                            modifier = Modifier.size(18.dp),
                            tint = colors.flag(flag),
                        )
                    }
                    if (row.marked) {
                        Icon(
                            painterResource(R.drawable.symbol_star),
                            contentDescription = stringResource(R.string.browse_state_marked),
                            modifier = Modifier.size(18.dp),
                            tint = secondaryInk,
                        )
                    }
                    row.states.forEach { state ->
                        Text(text = state.label, style = foundation.type.label, color = secondaryInk)
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = foundation.spacing.screenMargin),
            thickness = foundation.shapes.hairline,
            color = colors.outline,
        )
    }
}

internal fun FoundationColors.flag(flag: Flag): Color =
    when (flag) {
        Flag.NONE -> onBackgroundMuted
        Flag.RED -> flagRed
        Flag.ORANGE -> flagOrange
        Flag.GREEN -> flagGreen
        Flag.BLUE -> flagBlue
        Flag.PINK -> flagPink
        Flag.TURQUOISE -> flagTurquoise
        Flag.PURPLE -> flagPurple
    }
