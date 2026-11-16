// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.compose.theme

import android.content.Context
import androidx.annotation.DimenRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import com.ichi2.anki.common.android.R

/**
 * The Foundation as Compose reads it: the chosen Direction's colour roles, shapes, spacing and
 * elevation. Read it like the Material 3 slots: `MaterialTheme.foundation.colors.countNew`.
 *
 * Nothing here holds a value of its own. Colours come from the active XML theme's
 * `foundation*` attributes and sizes from the `foundation_*` dimensions (res/values/foundation.xml),
 * so Compose and the remaining XML screens always show the same look.
 */
@Immutable
data class Foundation(
    val colors: FoundationColors,
    val shapes: FoundationShapes,
    val spacing: FoundationSpacing,
    val elevation: FoundationElevation,
)

/** The Foundation's colour roles. Names match `docs/redesign/quiet-paper.values.json`. */
@Immutable
data class FoundationColors(
    val background: Color,
    val onBackground: Color,
    val onBackgroundMuted: Color,
    /** Hairlines: decoration only, never the only cue */
    val outline: Color,
    /** The one primary action, as a fill */
    val primary: Color,
    val onPrimary: Color,
    /** Cinnabar on the background, as a mark or as text */
    val emphasis: Color,
    val countNew: Color,
    val countLearning: Color,
    val countDue: Color,
    val answerAgain: Color,
    val answerHard: Color,
    val answerGood: Color,
    val answerEasy: Color,
    val progressTrack: Color,
    val progressFill: Color,
)

/** Corner shapes and rule widths. Small corners and hairlines instead of filled cards. */
@Immutable
data class FoundationShapes(
    val cardSheet: CornerBasedShape,
    val button: CornerBasedShape,
    val addButton: CornerBasedShape,
    val iconButton: CornerBasedShape,
    val hairline: Dp,
    val currentTabRule: Dp,
    val answerRule: Dp,
)

/** Spacing: the [scale] steps, then the named sizes of the Mockups */
@Immutable
data class FoundationSpacing(
    val scale: List<Dp>,
    val screenMargin: Dp,
    val studyMargin: Dp,
    val touchTarget: Dp,
    val deckRow: Dp,
    val deckRowNested: Dp,
    val countColumn: Dp,
    val primaryButton: Dp,
    val answerButton: Dp,
    val addButton: Dp,
    val bottomBar: Dp,
    val listRow: Dp,
    val secondaryButton: Dp,
    val pageMark: Dp,
    val emptyMark: Dp,
)

/**
 * Surfaces are flat and separated by hairlines. Nothing casts a shadow except the add button,
 * whose shadow is [addButtonShadow].
 */
@Immutable
data class FoundationElevation(
    val flat: Dp,
    val addButton: Dp,
    val addButtonShadow: Color,
)

/** Provided by [AnkiDroidTheme]; the default only shows up in previews and tests without it. */
val LocalFoundation = staticCompositionLocalOf<Foundation> { error("No Foundation: wrap the content in AnkiDroidTheme") }

/** Access like [MaterialTheme.colorScheme]: `MaterialTheme.foundation.colors.primary`. */
val MaterialTheme.foundation: Foundation
    @Composable
    @ReadOnlyComposable
    get() = LocalFoundation.current

/** Reads the Foundation from the active XML theme and the `foundation_*` dimensions. */
@VisibleForTesting
fun Context.toFoundation(): Foundation {
    var colors: FoundationColors? = null
    withStyledAttributes(attrs = R.styleable.Foundation) {
        fun color(index: Int) = Color(getColor(index, 0))
        colors =
            FoundationColors(
                background = color(R.styleable.Foundation_foundationBackground),
                onBackground = color(R.styleable.Foundation_foundationOnBackground),
                onBackgroundMuted = color(R.styleable.Foundation_foundationOnBackgroundMuted),
                outline = color(R.styleable.Foundation_foundationOutline),
                primary = color(R.styleable.Foundation_foundationPrimary),
                onPrimary = color(R.styleable.Foundation_foundationOnPrimary),
                emphasis = color(R.styleable.Foundation_foundationEmphasis),
                countNew = color(R.styleable.Foundation_foundationCountNew),
                countLearning = color(R.styleable.Foundation_foundationCountLearning),
                countDue = color(R.styleable.Foundation_foundationCountDue),
                answerAgain = color(R.styleable.Foundation_foundationAnswerAgain),
                answerHard = color(R.styleable.Foundation_foundationAnswerHard),
                answerGood = color(R.styleable.Foundation_foundationAnswerGood),
                answerEasy = color(R.styleable.Foundation_foundationAnswerEasy),
                progressTrack = color(R.styleable.Foundation_foundationProgressTrack),
                progressFill = color(R.styleable.Foundation_foundationProgressFill),
            )
    }
    val foundationColors = checkNotNull(colors)
    return Foundation(
        colors = foundationColors,
        shapes =
            FoundationShapes(
                cardSheet = RoundedCornerShape(dp(R.dimen.foundation_shape_card_sheet)),
                button = RoundedCornerShape(dp(R.dimen.foundation_shape_button)),
                addButton = RoundedCornerShape(dp(R.dimen.foundation_shape_add_button)),
                iconButton = CircleShape,
                hairline = dp(R.dimen.foundation_shape_hairline),
                currentTabRule = dp(R.dimen.foundation_shape_current_tab_rule),
                answerRule = dp(R.dimen.foundation_shape_answer_rule),
            ),
        spacing =
            FoundationSpacing(
                scale =
                    listOf(
                        R.dimen.foundation_space_4,
                        R.dimen.foundation_space_8,
                        R.dimen.foundation_space_12,
                        R.dimen.foundation_space_16,
                        R.dimen.foundation_space_20,
                        R.dimen.foundation_space_24,
                        R.dimen.foundation_space_32,
                    ).map { dp(it) },
                screenMargin = dp(R.dimen.foundation_spacing_screen_margin),
                studyMargin = dp(R.dimen.foundation_spacing_study_margin),
                touchTarget = dp(R.dimen.foundation_spacing_touch_target),
                deckRow = dp(R.dimen.foundation_spacing_deck_row),
                deckRowNested = dp(R.dimen.foundation_spacing_deck_row_nested),
                countColumn = dp(R.dimen.foundation_spacing_count_column),
                primaryButton = dp(R.dimen.foundation_spacing_primary_button),
                answerButton = dp(R.dimen.foundation_spacing_answer_button),
                addButton = dp(R.dimen.foundation_spacing_add_button),
                bottomBar = dp(R.dimen.foundation_spacing_bottom_bar),
                listRow = dp(R.dimen.foundation_spacing_list_row),
                secondaryButton = dp(R.dimen.foundation_spacing_secondary_button),
                pageMark = dp(R.dimen.foundation_spacing_page_mark),
                emptyMark = dp(R.dimen.foundation_spacing_empty_mark),
            ),
        elevation =
            FoundationElevation(
                flat = dp(R.dimen.foundation_elevation_flat),
                addButton = dp(R.dimen.foundation_elevation_add_button),
                addButtonShadow =
                    foundationColors.primary.copy(
                        alpha = ResourcesCompat.getFloat(resources, R.dimen.foundation_elevation_add_button_shadow_alpha),
                    ),
            ),
    )
}

/** Material 3's corner scale, set from the Foundation: small corners everywhere but the add button. */
internal fun Foundation.toMaterialShapes() =
    Shapes(
        extraSmall = shapes.cardSheet,
        small = shapes.button,
        medium = shapes.button,
        large = shapes.addButton,
        extraLarge = shapes.button,
    )

private fun Context.dp(
    @DimenRes id: Int,
): Dp = (resources.getDimension(id) / resources.displayMetrics.density).dp
