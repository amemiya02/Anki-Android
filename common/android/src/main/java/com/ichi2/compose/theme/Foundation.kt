// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.compose.theme

import android.content.Context
import android.os.Build
import android.util.TypedValue
import androidx.annotation.DimenRes
import androidx.annotation.StyleRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import com.ichi2.anki.common.android.R
import com.ichi2.anki.common.android.themes.interfaceTypeface

/**
 * The Foundation as Compose reads it: the chosen Direction's colour roles, shapes, spacing,
 * elevation and type scale. Read it like the Material 3 slots: `MaterialTheme.foundation.colors.countNew`,
 * `MaterialTheme.foundation.type.deckName`.
 *
 * Nothing here holds a value of its own. Colours come from the active XML theme's
 * `foundation*` attributes and sizes from the `foundation_*` dimensions (res/values/foundation.xml),
 * and type from the `TextAppearance.Foundation.*` styles, so Compose and the remaining XML screens
 * always show the same look.
 */
@Immutable
data class Foundation(
    val colors: FoundationColors,
    val shapes: FoundationShapes,
    val spacing: FoundationSpacing,
    val elevation: FoundationElevation,
    val type: FoundationType,
)

/**
 * The type scale. Each role is the XML TextAppearance of the same name (`TextAppearance.Foundation.*`
 * in res/values/foundation.xml), drawn in the interface typeface. Hierarchy is size and weight only.
 */
@Immutable
data class FoundationType(
    val screenTitle: TextStyle,
    val pageTitle: TextStyle,
    val dialogTitle: TextStyle,
    val deckName: TextStyle,
    val deckNameNested: TextStyle,
    val summary: TextStyle,
    val count: TextStyle,
    val studyTitle: TextStyle,
    val button: TextStyle,
    val rowTitle: TextStyle,
    val lead: TextStyle,
    val answer: TextStyle,
    val body: TextStyle,
    val editorField: TextStyle,
    val studyCount: TextStyle,
    val caption: TextStyle,
    val label: TextStyle,
) {
    val all: List<TextStyle>
        get() =
            listOf(
                screenTitle,
                pageTitle,
                dialogTitle,
                deckName,
                deckNameNested,
                summary,
                count,
                studyTitle,
                button,
                rowTitle,
                lead,
                answer,
                body,
                editorField,
                studyCount,
                caption,
                label,
            )
}

/** The Foundation's colour roles. Names match `docs/redesign/quiet-paper.values.json`. */
@Immutable
data class FoundationColors(
    val background: Color,
    val onBackground: Color,
    val onBackgroundMuted: Color,
    /** Hairlines: decoration only, never the only cue */
    val outline: Color,
    val dialogBorder: Color,
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
    val flagRed: Color,
    val flagOrange: Color,
    val flagGreen: Color,
    val flagBlue: Color,
    val flagPink: Color,
    val flagTurquoise: Color,
    val flagPurple: Color,
    val progressTrack: Color,
    val progressFill: Color,
)

/** Corner shapes and rule widths. Small corners and hairlines instead of filled cards. */
@Immutable
data class FoundationShapes(
    val cardSheet: CornerBasedShape,
    val button: CornerBasedShape,
    val dialog: CornerBasedShape,
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
    val settingsRow: Dp,
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
                dialogBorder = color(R.styleable.Foundation_foundationDialogBorder),
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
                flagRed = color(R.styleable.Foundation_foundationFlagRed),
                flagOrange = color(R.styleable.Foundation_foundationFlagOrange),
                flagGreen = color(R.styleable.Foundation_foundationFlagGreen),
                flagBlue = color(R.styleable.Foundation_foundationFlagBlue),
                flagPink = color(R.styleable.Foundation_foundationFlagPink),
                flagTurquoise = color(R.styleable.Foundation_foundationFlagTurquoise),
                flagPurple = color(R.styleable.Foundation_foundationFlagPurple),
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
                dialog = RoundedCornerShape(dp(R.dimen.foundation_shape_dialog)),
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
                settingsRow = dp(R.dimen.foundation_spacing_settings_row),
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
        type =
            FoundationType(
                screenTitle = textStyle(R.style.TextAppearance_Foundation_ScreenTitle),
                pageTitle = textStyle(R.style.TextAppearance_Foundation_PageTitle),
                dialogTitle = textStyle(R.style.TextAppearance_Foundation_DialogTitle),
                deckName = textStyle(R.style.TextAppearance_Foundation_DeckName),
                deckNameNested = textStyle(R.style.TextAppearance_Foundation_DeckNameNested),
                summary = textStyle(R.style.TextAppearance_Foundation_Summary),
                count = textStyle(R.style.TextAppearance_Foundation_Count),
                studyTitle = textStyle(R.style.TextAppearance_Foundation_StudyTitle),
                button = textStyle(R.style.TextAppearance_Foundation_Button),
                rowTitle = textStyle(R.style.TextAppearance_Foundation_RowTitle),
                lead = textStyle(R.style.TextAppearance_Foundation_Lead),
                answer = textStyle(R.style.TextAppearance_Foundation_Answer),
                body = textStyle(R.style.TextAppearance_Foundation_Body),
                editorField = textStyle(R.style.TextAppearance_Foundation_EditorField),
                studyCount = textStyle(R.style.TextAppearance_Foundation_StudyCount),
                caption = textStyle(R.style.TextAppearance_Foundation_Caption),
                label = textStyle(R.style.TextAppearance_Foundation_Label),
            ),
    )
}

/**
 * Reads a type role from its XML TextAppearance. Newsreader is drawn at the optical size of the
 * text, as the Mockups' browser does.
 */
private fun Context.textStyle(
    @StyleRes style: Int,
): TextStyle {
    var textStyle: TextStyle? = null
    withStyledAttributes(style, R.styleable.FoundationTextAppearance) {
        fun sp(index: Int): TextUnit {
            val value = TypedValue()
            check(getValue(index, value) && value.complexUnit == TypedValue.COMPLEX_UNIT_SP) { "type sizes are in sp" }
            return TypedValue.complexToFloat(value.data).sp
        }
        val size = sp(R.styleable.FoundationTextAppearance_android_textSize)
        textStyle =
            TextStyle(
                fontFamily = interfaceFontFamily(interfaceTypeface(opticalSize = size.value)),
                fontSize = size,
                fontWeight = FontWeight(getInt(R.styleable.FoundationTextAppearance_foundationTextWeight, 400)),
                fontStyle = FontStyle.Normal,
                lineHeight = sp(R.styleable.FoundationTextAppearance_lineHeight),
                // Half the extra line height above the text and half below, as on the web
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
                letterSpacing = 0.sp,
                fontFeatureSettings = getString(R.styleable.FoundationTextAppearance_android_fontFeatureSettings),
            )
    }
    return checkNotNull(textStyle)
}

/**
 * Material 3's type slots, set from the type scale. The XML themes map the same slots
 * (`textAppearanceBodyMedium` and so on, in theme_light.xml and theme_dark.xml).
 */
fun Foundation.toMaterialTypography() =
    Typography(
        displayLarge = type.screenTitle,
        displayMedium = type.screenTitle,
        displaySmall = type.screenTitle,
        headlineLarge = type.screenTitle,
        headlineMedium = type.pageTitle,
        headlineSmall = type.deckName,
        titleLarge = type.deckName,
        titleMedium = type.rowTitle,
        titleSmall = type.rowTitle,
        bodyLarge = type.lead,
        bodyMedium = type.body,
        bodySmall = type.caption,
        labelLarge = type.button,
        labelMedium = type.label,
        labelSmall = type.label,
    )

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

/**
 * The interface typeface as a Compose [FontFamily] that answers to each weight the type scale uses.
 *
 * `FontFamily(Typeface(typeface))` hands Compose the one Android typeface whatever weight a style
 * asks for, so every role drew at 400. Here each weight is its own font, derived from the same
 * fallback chain the way XML text derives it (`Typeface.create(typeface, weight, false)`).
 */
private fun interfaceFontFamily(typeface: android.graphics.Typeface): FontFamily =
    FontFamily(INTERFACE_WEIGHTS.map { weight -> InterfaceFont(FontWeight(weight), typeface.atWeight(weight)) })

private val INTERFACE_WEIGHTS = listOf(400, 500, 600, 700)

private fun android.graphics.Typeface.atWeight(weight: Int): android.graphics.Typeface =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        android.graphics.Typeface.create(this, weight, false)
    } else {
        android.graphics.Typeface.create(this, if (weight >= 600) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

private class InterfaceFont(
    override val weight: FontWeight,
    val typeface: android.graphics.Typeface,
) : AndroidFont(FontLoadingStrategy.Blocking, InterfaceFontLoader, FontVariation.Settings()) {
    override val style: FontStyle = FontStyle.Normal
}

private object InterfaceFontLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(
        context: Context,
        font: AndroidFont,
    ): android.graphics.Typeface = (font as InterfaceFont).typeface

    override suspend fun awaitLoad(
        context: Context,
        font: AndroidFont,
    ): android.graphics.Typeface = loadBlocking(context, font)
}
