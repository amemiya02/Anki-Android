// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import anki.scheduler.CardAnswer.Rating
import com.ichi2.compose.theme.foundation
import com.ichi2.anki.common.android.R as CommonR

/**
 * Show answer, then Again, Hard, Good and Easy, in the same place at the bottom of the study screen.
 *
 * The area keeps the answer buttons' height on both sides, so the Card does not move when the
 * answer is shown; Show answer sits at the bottom of it, where the answer buttons will be.
 */
@Composable
fun AnswerArea(
    isAnswerShown: Boolean,
    nextTimes: AnswerButtonsNextTime?,
    onRatingClicked: (Rating) -> Unit,
    onShowAnswerClicked: () -> Unit,
    modifier: Modifier = Modifier,
    showHardAndEasyButtons: Boolean = true,
    buttonsHeightPercentage: Int = 100,
) {
    val foundation = MaterialTheme.foundation
    val spacing = foundation.spacing
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .relativeHeight(buttonsHeightPercentage)
                    .padding(top = 6.dp, bottom = 8.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .heightIn(min = spacing.answerButton)
                        .invisibleIf(!isAnswerShown),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val colors = foundation.colors
                RatingButton(CommonR.string.ease_button_again, nextTimes?.again, colors.answerAgain) { onRatingClicked(Rating.AGAIN) }
                if (showHardAndEasyButtons) {
                    RatingButton(CommonR.string.ease_button_hard, nextTimes?.hard, colors.answerHard) { onRatingClicked(Rating.HARD) }
                }
                RatingButton(CommonR.string.ease_button_good, nextTimes?.good, colors.answerGood) { onRatingClicked(Rating.GOOD) }
                if (showHardAndEasyButtons) {
                    RatingButton(CommonR.string.ease_button_easy, nextTimes?.easy, colors.answerEasy) { onRatingClicked(Rating.EASY) }
                }
            }
            Button(
                onClick = onShowAnswerClicked,
                modifier =
                    Modifier
                        .matchParentSize()
                        .padding(top = spacing.answerButton - spacing.primaryButton)
                        .invisibleIf(isAnswerShown),
                shape = foundation.shapes.button,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = foundation.colors.primary,
                        contentColor = foundation.colors.onPrimary,
                    ),
                elevation = null,
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Text(stringResource(CommonR.string.show_answer), style = foundation.type.button, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * A paper button outlined by a hairline, with a rule in the answer's colour along its top edge.
 * The time until the Card comes back sits above the answer's name.
 */
@Composable
private fun RowScope.RatingButton(
    @StringRes easeName: Int,
    nextTime: String?,
    ruleColor: Color,
    onClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val shape = foundation.shapes.button
    val name = stringResource(easeName)
    val text =
        buildAnnotatedString {
            if (nextTime != null) {
                val label = foundation.type.label
                withStyle(
                    SpanStyle(fontSize = label.fontSize, fontWeight = label.fontWeight, color = foundation.colors.onBackgroundMuted),
                ) {
                    append(nextTime)
                }
                // one Text keeps the time and the name together for screen readers
                append("\n")
            }
            append(name)
        }
    Box(
        modifier =
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(shape)
                .border(foundation.shapes.hairline, foundation.colors.outline, shape)
                .drawWithContent {
                    drawContent()
                    drawRect(ruleColor, size = Size(size.width, foundation.shapes.answerRule.toPx()))
                }.clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 2.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            // each line takes the direction of its own text, so `<10m` reads the same in Arabic
            style = foundation.type.answer.copy(textDirection = TextDirection.Content),
            color = foundation.colors.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Modifier.relativeHeight(percentage: Int): Modifier {
    if (percentage <= 100) return this
    return layout { measurable, constraints ->
        val height =
            (measurable.minIntrinsicHeight(constraints.maxWidth) * percentage / 100)
                .coerceIn(constraints.minHeight, constraints.maxHeight)
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}

private fun Modifier.invisibleIf(invisible: Boolean): Modifier {
    if (!invisible) return this
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {}
    }
}
