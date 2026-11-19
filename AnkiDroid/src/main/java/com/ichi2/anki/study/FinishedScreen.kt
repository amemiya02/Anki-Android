// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.study

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.R
import com.ichi2.anki.common.time.TIME_HOUR
import com.ichi2.anki.common.time.TIME_MINUTE
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.ui.windows.reviewer.StudyBarState
import com.ichi2.anki.ui.windows.reviewer.StudyBarTitles
import com.ichi2.compose.theme.foundation
import java.util.Date
import kotlin.math.roundToInt
import com.ichi2.anki.common.android.R as CommonR

/**
 * The finished screen: a Deck has nothing left to study now. It leads with the reason
 * ([FinishedState.reason]), says when more Cards become available, offers to unbury Cards,
 * custom study and Deck options, and closes with one button back to the deck list.
 */
@Composable
fun FinishedScreen(
    state: FinishedState,
    onBack: () -> Unit,
    onUnbury: () -> Unit,
    onCustomStudy: () -> Unit,
    onDeckOptions: () -> Unit,
    onBackToDecks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val foundation = MaterialTheme.foundation
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(CommonR.drawable.symbol_arrow_back),
                    contentDescription = stringResource(androidx.appcompat.R.string.abc_action_bar_up_description),
                    tint = foundation.colors.onBackground,
                )
            }
            StudyBarTitles(StudyBarState(deckPath = state.deckPath), modifier = Modifier.weight(1f))
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = foundation.spacing.screenMargin, end = foundation.spacing.screenMargin, top = 28.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FinishedMark(state.reason.mark)
            Text(
                text = stringResource(state.titleRes),
                style = foundation.type.pageTitle,
                color = foundation.colors.onBackground,
                modifier = Modifier.padding(top = 6.dp).semantics { heading() },
            )
            for (paragraph in remember(state) { state.paragraphs(context) }) {
                Text(text = paragraph, style = foundation.type.lead, color = foundation.colors.onBackground)
            }
            if (state.hasBuried) {
                UnburyButton(onUnbury)
            }
            Column(
                modifier =
                    Modifier
                        .padding(top = 10.dp)
                        .clip(foundation.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                if (!state.isFiltered) {
                    FinishedRow(
                        icon = R.drawable.symbol_auto_stories,
                        title = with(context) { TR.sentenceCase.customStudy },
                        emphasis = state.reason == FinishedReason.DailyLimit,
                        onClick = onCustomStudy,
                    )
                }
                FinishedRow(
                    icon = R.drawable.symbol_tune,
                    title = with(context) { TR.sentenceCase.deckOptions },
                    emphasis = false,
                    onClick = onDeckOptions,
                )
            }
            if (state.studiedToday.isNotBlank()) {
                Text(text = state.studiedToday, style = foundation.type.caption, color = foundation.colors.onBackgroundMuted)
            }
            if (state.deckDescription.isNotBlank()) {
                Text(
                    text = remember(state.deckDescription) { AnnotatedString.fromHtml(state.deckDescription) },
                    style = foundation.type.body,
                    color = foundation.colors.onBackground,
                )
            }
        }

        Button(
            onClick = onBackToDecks,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = foundation.spacing.studyMargin, end = foundation.spacing.studyMargin, top = 6.dp, bottom = 8.dp)
                    .heightIn(min = foundation.spacing.primaryButton),
            shape = foundation.shapes.button,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = foundation.colors.primary,
                    contentColor = foundation.colors.onPrimary,
                ),
            elevation = null,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(stringResource(CommonR.string.finished_back_to_decks), style = foundation.type.button, textAlign = TextAlign.Center)
        }
    }
}

private val FinishedState.titleRes: Int
    get() =
        when (reason) {
            FinishedReason.DailyLimit -> CommonR.string.finished_title_limit
            FinishedReason.LearningLater, FinishedReason.Buried -> CommonR.string.finished_title_later
            // a filtered Deck gets no more Cards by itself tomorrow
            FinishedReason.Done -> if (isFiltered) CommonR.string.finished_title_later else CommonR.string.finished_title_done
        }

private val FinishedReason.mark: Int
    @DrawableRes get() =
        when (this) {
            FinishedReason.DailyLimit -> R.drawable.symbol_hourglass_bottom
            FinishedReason.LearningLater -> R.drawable.symbol_schedule
            FinishedReason.Buried -> R.drawable.symbol_inventory_2
            FinishedReason.Done -> R.drawable.symbol_check
        }

/**
 * Everything the Engine's congratulations page said, in the Engine's words, then when the next
 * day's Cards arrive. The reason the screen leads with comes first.
 */
internal fun FinishedState.paragraphs(context: Context): List<String> =
    buildList<String> {
        learningLater?.let { add(it.describe()) }
        if (newLimitReached) add(TR.schedulingTodayNewLimitReached())
        if (reviewLimitReached) add(TR.schedulingTodayReviewLimitReached())
        if (hasBuried) add(TR.schedulingBuriedCardsFound(TR.schedulingUnburyThem()))
        if (reason == FinishedReason.Done) add(TR.schedulingCongratulationsFinished())
        if (isFiltered) {
            add(TR.studyingThisIsASpecialDeckFor())
        } else if (learningLater == null) {
            val time = DateFormat.getTimeFormat(context).format(Date(nextDayStartsAtMillis))
            add(context.getString(CommonR.string.finished_next_day, time))
        }
    }.map { it.collapseLineBreaks() }

/** The Engine's longer strings are wrapped with line breaks, which its web page collapsed. */
private fun String.collapseLineBreaks() = replace(Regex("""\s*\n\s*"""), " ").trim()

/** As the congratulations page put it: how many come back, and when the next one is ready. */
private fun FinishedState.LearningLater.describe(): String {
    val seconds = nextIn.inWholeSeconds
    val (unit, amount) =
        when {
            seconds < TIME_MINUTE -> "seconds" to seconds.toDouble()
            seconds < TIME_HOUR -> "minutes" to seconds.toDouble() / TIME_MINUTE
            else -> "hours" to seconds.toDouble() / TIME_HOUR
        }
    return "${TR.schedulingLearnRemaining(remaining)} ${TR.schedulingNextLearnDue(unit, amount.roundToInt())}"
}

@Composable
private fun FinishedMark(
    @DrawableRes icon: Int,
) {
    val foundation = MaterialTheme.foundation
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier =
            Modifier
                .size(foundation.spacing.pageMark)
                .clip(CircleShape)
                .background(scheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = scheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun UnburyButton(onClick: () -> Unit) {
    val foundation = MaterialTheme.foundation
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = foundation.spacing.secondaryButton),
        shape = foundation.shapes.button,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.symbol_replay),
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp).size(20.dp),
        )
        Text(stringResource(CommonR.string.finished_unbury), style = foundation.type.button, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FinishedRow(
    @DrawableRes icon: Int,
    title: String,
    emphasis: Boolean,
    onClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val accent: Color = if (emphasis) foundation.colors.emphasis else foundation.colors.onBackground
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = foundation.spacing.listRow)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
        Text(title, Modifier.weight(1f), style = foundation.type.rowTitle, color = accent)
        Icon(
            painter = painterResource(CommonR.drawable.symbol_chevron_right),
            contentDescription = null,
            tint = foundation.colors.onBackgroundMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}
