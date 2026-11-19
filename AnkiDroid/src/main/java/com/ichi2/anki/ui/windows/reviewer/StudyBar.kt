// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ichi2.anki.libanki.sched.Counts
import com.ichi2.anki.utils.remainingTime
import com.ichi2.compose.theme.foundation
import com.ichi2.anki.common.android.R as CommonR

/** Separates the levels of a Deck path; mirrored in right-to-left text. */
private const val DECK_PATH_SEPARATOR = " › "

/**
 * The text of the study bar: the Deck path above the remaining counts and the time-remaining
 * estimate. The back button, undo and the menu sit either side of it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyBarTitles(
    state: StudyBarState,
    modifier: Modifier = Modifier,
) {
    val foundation = MaterialTheme.foundation
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (state.deckPath.isNotEmpty()) {
            Text(
                text =
                    buildAnnotatedString {
                        state.deckPath.forEachIndexed { i, part ->
                            if (i > 0) {
                                withStyle(SpanStyle(color = foundation.colors.onBackgroundMuted, fontWeight = FontWeight.Normal)) {
                                    append(DECK_PATH_SEPARATOR)
                                }
                            }
                            append(part)
                        }
                    },
                style = foundation.type.studyTitle,
                color = foundation.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.counts == null && state.minutesLeft == null) return@Column
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            state.counts?.let { StudyBarCounts(it) }
            state.minutesLeft?.let { minutes ->
                Text(
                    text = remainingTime(LocalContext.current, minutes * 60L),
                    style = foundation.type.caption,
                    color = foundation.colors.onBackgroundMuted,
                )
            }
        }
    }
}

/** New, learning and due counts; the queue the current Card comes from sits on a tint of its colour. */
@Composable
private fun StudyBarCounts(counts: StudyCounts) {
    val colors = MaterialTheme.foundation.colors
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        StudyBarCount(counts.new, colors.countNew, isActive = counts.activeQueue == Counts.Queue.NEW)
        StudyBarCount(counts.learn, colors.countLearning, isActive = counts.activeQueue == Counts.Queue.LRN)
        StudyBarCount(counts.review, colors.countDue, isActive = counts.activeQueue == Counts.Queue.REV)
    }
}

@Composable
private fun StudyBarCount(
    count: String,
    color: Color,
    isActive: Boolean,
) {
    val foundation = MaterialTheme.foundation
    val isZero = count == "0"
    Text(
        text = count,
        style =
            foundation.type.studyCount.copy(
                fontWeight = if (isZero) FontWeight.Normal else foundation.type.studyCount.fontWeight,
            ),
        color = if (isZero) foundation.colors.onBackgroundMuted else color,
        modifier =
            Modifier
                .clip(CircleShape)
                .then(if (isActive) Modifier.background(color.copy(alpha = ACTIVE_TINT)) else Modifier)
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** The tint behind the active queue's count: its own colour, faint enough that the count reads as AA */
private const val ACTIVE_TINT = 0.12f

/** Material 3's linear progress under the study bar, gliding forward as Cards leave the session. */
@Composable
fun StudyProgress(
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    if (progress == null) return
    val foundation = MaterialTheme.foundation
    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = PROGRESS_MILLIS),
        label = "study progress",
    )
    LinearProgressIndicator(
        progress = { shown },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = foundation.spacing.studyMargin)
                .height(4.dp),
        color = foundation.colors.progressFill,
        trackColor = foundation.colors.progressTrack,
    )
}

/** Long enough to read as movement, short enough to finish before the next answer */
private const val PROGRESS_MILLIS = 300

/** Back, at the start of the study bar: drawn in the bar's ink, never the default black */
@Composable
fun StudyBackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(CommonR.drawable.symbol_arrow_back),
            contentDescription = stringResource(androidx.appcompat.R.string.abc_action_bar_up_description),
            tint = MaterialTheme.foundation.colors.onBackground,
        )
    }
}
