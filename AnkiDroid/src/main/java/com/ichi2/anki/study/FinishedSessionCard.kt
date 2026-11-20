// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.study

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ichi2.compose.theme.foundation
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import com.ichi2.anki.common.android.R as CommonR

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FinishedSessionCard(summary: ReviewSessionSummary) {
    val foundation = MaterialTheme.foundation
    val locale = LocalConfiguration.current.locales[0]
    val elapsed = summary.elapsedMillis.milliseconds
    val reviewed = remember(summary.reviewCount, locale) { NumberFormat.getIntegerInstance(locale).format(summary.reviewCount) }
    val ratings =
        listOf(
            RatingCount(CommonR.string.ease_button_again, foundation.colors.answerAgain, summary.again),
            RatingCount(CommonR.string.ease_button_hard, foundation.colors.answerHard, summary.hard),
            RatingCount(CommonR.string.ease_button_good, foundation.colors.answerGood, summary.good),
            RatingCount(CommonR.string.ease_button_easy, foundation.colors.answerEasy, summary.easy),
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(foundation.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SessionFigure(reviewed, CommonR.string.finished_reviewed)
            SessionFigure(elapsed.clock(locale), CommonR.string.finished_time_spent, elapsed.spoken(locale))
        }
        Row(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)) {
            for (rating in ratings) {
                if (rating.count > 0) {
                    Box(Modifier.weight(rating.count.toFloat()).height(8.dp).background(rating.color))
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (rating in ratings) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).background(color = rating.color, shape = CircleShape))
                    Text(
                        text = stringResource(CommonR.string.finished_rating_count, stringResource(rating.label), rating.count),
                        style = foundation.type.caption,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionFigure(
    value: String,
    @StringRes label: Int,
    spokenValue: String? = null,
) {
    val foundation = MaterialTheme.foundation
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = stringResource(label),
            style = foundation.type.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = foundation.type.pageTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = if (spokenValue == null) Modifier else Modifier.clearAndSetSemantics { contentDescription = spokenValue },
        )
    }
}

private data class RatingCount(
    @StringRes val label: Int,
    val color: Color,
    val count: Int,
)

private fun Duration.clock(locale: Locale): String =
    toComponents { hours, minutes, seconds, _ ->
        if (hours >
            0
        ) {
            String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(locale, "%d:%02d", minutes, seconds)
        }
    }

private fun Duration.spoken(locale: Locale): String =
    toComponents { hours, minutes, seconds, _ ->
        val components =
            buildList {
                if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
                if (minutes > 0) add(Measure(minutes, MeasureUnit.MINUTE))
                if (seconds > 0 || isEmpty()) add(Measure(seconds, MeasureUnit.SECOND))
            }
        MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE).formatMeasures(*components.toTypedArray())
    }
