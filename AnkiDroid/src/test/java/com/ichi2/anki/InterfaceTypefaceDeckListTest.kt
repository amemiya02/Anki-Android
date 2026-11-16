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
import androidx.appcompat.widget.Toolbar
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
 * Each check compares with the expected face alone drawing the same text at the same weight; a
 * Deck name is in the type scale's DeckName style, weight 500, which Chinese and Japanese draw in
 * their regular face.
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
            assertDrawnLike(CommonR.font.klee_one_regular, deckName, SHARED_HAN)
            assertDrawnLike(CommonR.font.newsreader, deckName, LATIN)
        }
    }

    @Test
    fun `in Chinese, a Deck name shows Chinese Han forms and Latin in Newsreader`() {
        RuntimeEnvironment.setQualifiers("zh-rCN")
        addDeck(DECK_NAME)
        withDeckPicker(deckCount = 0) { deckPicker ->
            val deckName = deckPicker.textView(DECK_NAME)
            assertDrawnLike(CommonR.font.lxgw_wenkai_regular, deckName, SHARED_HAN)
            assertDrawnLike(CommonR.font.newsreader, deckName, LATIN)
        }
    }

    @Test
    fun `in Japanese, Latin in the app bar title is Newsreader`() {
        RuntimeEnvironment.setQualifiers("ja")
        withDeckPicker(deckCount = 0) { deckPicker ->
            val title =
                deckPicker
                    .findViewById<Toolbar>(R.id.toolbar)
                    .allViews
                    .filterIsInstance<TextView>()
                    .first { it.text.isNotEmpty() }
            assertDrawnLike(CommonR.font.newsreader, title, title.text.toString(), weight = title.typeface.weight)
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
        weight: Int = DECK_NAME_WEIGHT,
    ) {
        val name = targetContext.resources.getResourceEntryName(face)
        assertTrue(
            draw(view.typeface, text).sameAs(draw(only(face, weight), text)),
            "'$text' should be drawn in $name at $weight (view: ${view.typeface.weight})",
        )
    }

    /** [face] alone at [weight]; Newsreader at its text optical size */
    private fun only(
        @FontRes face: Int,
        weight: Int,
    ): Typeface {
        val font =
            Font
                .Builder(targetContext.resources, face)
                .setWeight(weight)
                .apply { if (face == CommonR.font.newsreader) setFontVariationSettings("'wght' $weight, 'opsz' 16") }
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

        /** Drawn differently in Japanese and Chinese */
        const val SHARED_HAN = "骨直"
        const val LATIN = "Kanji"
        const val DECK_NAME = "$LATIN $SHARED_HAN"
    }
}
