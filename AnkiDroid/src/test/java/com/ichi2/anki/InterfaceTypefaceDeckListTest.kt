// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.core.view.allViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertTrue
import com.ichi2.anki.common.android.R as CommonR

/**
 * The deck list, an XML screen, draws its text in the bundled faces: Latin in Newsreader and Han
 * characters in the face of the interface language, even inside one Deck name.
 *
 * A Deck name is bold, so each check compares with the heavier face drawing the same text alone.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class InterfaceTypefaceDeckListTest : RobolectricTest() {
    @Test
    fun `in Japanese, a Deck name shows Japanese Han forms and Latin in Newsreader`() {
        RuntimeEnvironment.setQualifiers("ja")
        addDeck(DECK_NAME)
        withDeckPicker(deckCount = 0) { deckPicker ->
            val deckName = deckPicker.textView(DECK_NAME)
            assertDrawnLike(CommonR.font.klee_one_semibold, deckName, SHARED_HAN)
            assertDrawnLike(CommonR.font.newsreader, deckName, LATIN)
        }
    }

    @Test
    fun `in Chinese, a Deck name shows Chinese Han forms and Latin in Newsreader`() {
        RuntimeEnvironment.setQualifiers("zh-rCN")
        addDeck(DECK_NAME)
        withDeckPicker(deckCount = 0) { deckPicker ->
            val deckName = deckPicker.textView(DECK_NAME)
            assertDrawnLike(CommonR.font.lxgw_wenkai_medium, deckName, SHARED_HAN)
            assertDrawnLike(CommonR.font.newsreader, deckName, LATIN)
        }
    }

    private fun DeckPicker.textView(text: String): TextView =
        deckPickerBinding.decks.allViews
            .filterIsInstance<TextView>()
            .first { it.text.toString() == text }

    private fun assertDrawnLike(
        @FontRes face: Int,
        view: TextView,
        text: String,
    ) {
        val name = targetContext.resources.getResourceEntryName(face)
        assertTrue(draw(view.typeface, text).sameAs(draw(boldOnly(face), text)), "'$text' in the Deck name should be drawn in $name")
    }

    /** [face] alone at the bold weight; Newsreader at its text optical size */
    private fun boldOnly(
        @FontRes face: Int,
    ): Typeface {
        val font =
            Font
                .Builder(targetContext.resources, face)
                .setWeight(BOLD)
                .apply { if (face == CommonR.font.newsreader) setFontVariationSettings("'wght' $BOLD, 'opsz' 16") }
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
        const val BOLD = 700

        /** Drawn differently in Japanese and Chinese */
        const val SHARED_HAN = "骨直"
        const val LATIN = "Kanji"
        const val DECK_NAME = "$LATIN $SHARED_HAN"
    }
}
