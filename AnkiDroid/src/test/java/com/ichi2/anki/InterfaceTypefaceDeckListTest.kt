// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import androidx.annotation.FontRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertTrue
import com.ichi2.anki.common.android.R as CommonR

/**
 * The deck list draws its text in the bundled faces: Latin in Newsreader and Han
 * characters in the face of the interface language, even inside one Deck name.
 *
 * Each check compares with the expected face alone drawing the same text at the same weight; a
 * Deck name is in the type scale's DeckName style, weight 500, which Chinese and Japanese draw in
 * their regular face.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class InterfaceTypefaceDeckListTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `in Japanese, a Deck name shows Japanese Han forms and Latin in Newsreader`() {
        RuntimeEnvironment.setQualifiers("ja")
        addDeck(DECK_NAME)
        withDeckPicker(deckCount = 0) { deckPicker ->
            val deckName = typefaceOf(DECK_NAME)
            assertDrawnLike(CommonR.font.klee_one_regular, deckName, SHARED_HAN)
            assertDrawnLike(CommonR.font.newsreader, deckName, LATIN, opticalSize = DECK_NAME_SIZE)
        }
    }

    @Test
    fun `in Chinese, a Deck name shows Chinese Han forms and Latin in Newsreader`() {
        RuntimeEnvironment.setQualifiers("zh-rCN")
        addDeck(DECK_NAME)
        withDeckPicker(deckCount = 0) { deckPicker ->
            val deckName = typefaceOf(DECK_NAME)
            assertDrawnLike(CommonR.font.lxgw_wenkai_regular, deckName, SHARED_HAN)
            assertDrawnLike(CommonR.font.newsreader, deckName, LATIN, opticalSize = DECK_NAME_SIZE)
        }
    }

    @Test
    fun `in Japanese, Latin in the app bar title is Newsreader`() {
        RuntimeEnvironment.setQualifiers("ja")
        withDeckPicker(deckCount = 0) {
            // the Engine's "Decks", which is English under Robolectric
            val title = typefaceOf("Decks")
            assertDrawnLike(CommonR.font.newsreader, title, "Decks", weight = SCREEN_TITLE_WEIGHT, opticalSize = SCREEN_TITLE_SIZE)
        }
    }

    /** The typeface Compose draws [text] in, from the style of its text layout */
    private fun typefaceOf(text: String): Typeface {
        val node = composeRule.onNode(hasText(text) and !hasAnyAncestor(hasTestTag("tab-home")), useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        checkNotNull(node.config[SemanticsActions.GetTextLayoutResult].action).invoke(layouts)
        val style = layouts.single().layoutInput.style
        return createFontFamilyResolver(targetContext).resolve(style.fontFamily, style.fontWeight ?: FontWeight.Normal).value as Typeface
    }

    private fun assertDrawnLike(
        @FontRes face: Int,
        typeface: Typeface,
        text: String,
        weight: Int = DECK_NAME_WEIGHT,
        opticalSize: Float = DECK_NAME_SIZE,
    ) {
        val name = targetContext.resources.getResourceEntryName(face)
        assertTrue(
            draw(typeface, text).sameAs(draw(only(face, weight, opticalSize), text)),
            "'$text' should be drawn in $name at $weight (drawn: ${typeface.weight})",
        )
    }

    /** [face] alone at [weight]; Newsreader at the optical size of the text's size, as Compose draws it */
    private fun only(
        @FontRes face: Int,
        weight: Int,
        opticalSize: Float,
    ): Typeface {
        val font =
            Font
                .Builder(targetContext.resources, face)
                .setWeight(weight)
                .apply { if (face == CommonR.font.newsreader) setFontVariationSettings("'wght' $weight, 'opsz' $opticalSize") }
                .build()
        return Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build()).setSystemFallback("serif").build()
    }

    private fun draw(
        typeface: Typeface,
        text: String,
    ): Bitmap {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = 48f
                color = Color.BLACK
            }
        return Bitmap.createBitmap(600, 80, Bitmap.Config.ARGB_8888).also {
            Canvas(it).apply {
                drawColor(Color.WHITE)
                drawText(text, 4f, 60f, paint)
            }
        }
    }

    companion object {
        const val DECK_NAME_WEIGHT = 500
        const val SCREEN_TITLE_WEIGHT = 500

        /** The type scale's sizes, which are also Newsreader's optical sizes */
        const val DECK_NAME_SIZE = 20f
        const val SCREEN_TITLE_SIZE = 38f

        /** Drawn differently in Japanese and Chinese */
        const val SHARED_HAN = "骨直"
        const val LATIN = "Kanji"
        const val DECK_NAME = "$LATIN $SHARED_HAN"
    }
}
