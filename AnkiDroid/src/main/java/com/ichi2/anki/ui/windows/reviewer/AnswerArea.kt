// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
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
    onShowAnswerPressTooShort: () -> Unit = {},
    showAnswerMinimumPressMs: Int = 0,
    doubleTapIntervalMs: Int = 0,
) {
    val foundation = MaterialTheme.foundation
    val spacing = foundation.spacing
    // "Double tap time interval": a press that follows the last one too soon is a slip, not an answer
    val scope = rememberCoroutineScope()
    var isCoolingDown by remember { mutableStateOf(false) }
    val guarded: (() -> Unit) -> Unit = { action ->
        if (!isCoolingDown) {
            action()
            if (doubleTapIntervalMs > 0) {
                isCoolingDown = true
                scope.launch {
                    delay(doubleTapIntervalMs.milliseconds)
                    isCoolingDown = false
                }
            }
        }
    }
    // "Show answer long-press time": Show answer acts once held that long; a tap only says why
    var wasShownByHolding by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .relativeHeight(buttonsHeightPercentage)
                    .padding(top = 6.dp, bottom = 8.dp),
        ) {
            val colors = foundation.colors

            fun rate(rating: Rating) = guarded { onRatingClicked(rating) }
            val firstRatings: @Composable RowScope.() -> Unit = {
                RatingButton(CommonR.string.ease_button_again, nextTimes?.again, colors.answerAgain) { rate(Rating.AGAIN) }
                if (showHardAndEasyButtons) {
                    RatingButton(CommonR.string.ease_button_hard, nextTimes?.hard, colors.answerHard) { rate(Rating.HARD) }
                }
            }
            val remainingRatings: @Composable RowScope.() -> Unit = {
                RatingButton(CommonR.string.ease_button_good, nextTimes?.good, colors.answerGood) { rate(Rating.GOOD) }
                if (showHardAndEasyButtons) {
                    RatingButton(CommonR.string.ease_button_easy, nextTimes?.easy, colors.answerEasy) { rate(Rating.EASY) }
                }
            }
            Column(modifier = Modifier.invisibleIf(!isAnswerShown), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showHardAndEasyButtons && LocalDensity.current.fontScale >= 1.5f) {
                    RatingRow(firstRatings)
                    RatingRow(remainingRatings)
                } else {
                    RatingRow {
                        firstRatings()
                        remainingRatings()
                    }
                }
            }
            Button(
                onClick = {
                    when {
                        showAnswerMinimumPressMs <= 0 -> guarded(onShowAnswerClicked)
                        wasShownByHolding -> wasShownByHolding = false
                        else -> onShowAnswerPressTooShort()
                    }
                },
                modifier =
                    Modifier
                        .matchParentSize()
                        .padding(top = spacing.answerButton - spacing.primaryButton)
                        .invisibleIf(isAnswerShown)
                        .pointerInput(showAnswerMinimumPressMs) {
                            if (showAnswerMinimumPressMs <= 0) return@pointerInput
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                val released =
                                    withTimeoutOrNull(showAnswerMinimumPressMs.toLong()) {
                                        waitForUpOrCancellation()
                                        true
                                    }
                                if (released == null) {
                                    wasShownByHolding = true
                                    guarded(onShowAnswerClicked)
                                }
                            }
                        },
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

@Composable
private fun RatingRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = MaterialTheme.foundation.spacing.answerButton),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * A tonal pill in the surface container, its name in the answer's colour. The time until the Card
 * comes back sits above the name. A press settles the button slightly, so the thumb feels it land.
 */
@Composable
private fun RowScope.RatingButton(
    @StringRes easeName: Int,
    nextTime: String?,
    answerColor: Color,
    onClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val shape = foundation.shapes.button
    val name = stringResource(easeName)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        animationSpec = tween(durationMillis = PRESS_MILLIS),
        label = "answer press",
    )
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
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(interactionSource = interactionSource, indication = ripple(), role = Role.Button, onClick = onClick)
                .padding(horizontal = 2.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            // each line takes the direction of its own text, so `<10m` reads the same in Arabic
            style = foundation.type.answer.copy(textDirection = TextDirection.Content),
            color = answerColor,
            textAlign = TextAlign.Center,
        )
    }
}

/** How far a pressed answer button settles, and how fast: well inside the 120ms answering budget */
private const val PRESSED_SCALE = 0.96f
private const val PRESS_MILLIS = 90

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
