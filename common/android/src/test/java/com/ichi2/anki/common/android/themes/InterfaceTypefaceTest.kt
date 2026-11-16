// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.common.android.themes

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.LocaleList
import androidx.annotation.FontRes
import androidx.test.core.app.ApplicationProvider
import com.ichi2.anki.common.android.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Interface text is drawn in the bundled faces: Latin in Newsreader, Chinese in LXGW WenKai,
 * Japanese in Klee One, and any other script in the system font.
 *
 * Each check draws text with [interfaceTypeface] and compares the pixels with the same text drawn
 * in the one face it should come from.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class InterfaceTypefaceTest {
    private val appContext: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `in Japanese, a Han character shared with Chinese shows its Japanese form`() {
        val japanese = interfaceIn(Locale.JAPANESE)
        assertDrawnLike(KLEE_ONE, japanese, SHARED_HAN)
        assertNotDrawnLike(LXGW_WENKAI, japanese, SHARED_HAN)
    }

    @Test
    fun `in Chinese, a Han character shared with Japanese shows its Chinese form`() {
        val chinese = interfaceIn(Locale.SIMPLIFIED_CHINESE)
        assertDrawnLike(LXGW_WENKAI, chinese, SHARED_HAN)
        assertNotDrawnLike(KLEE_ONE, chinese, SHARED_HAN)
    }

    @Test
    fun `in Japanese, a character only Chinese uses still comes from the bundled Chinese face`() {
        // 们 is Simplified Chinese only: Klee One has no glyph for it
        assertDrawnLike(LXGW_WENKAI, interfaceIn(Locale.JAPANESE), "们")
    }

    @Test
    fun `in any other language, Chinese and Japanese text uses the bundled faces`() {
        val english = interfaceIn(Locale.US)
        assertDrawnLike(LXGW_WENKAI, english, "汉语$SHARED_HAN")
        assertNotDrawnLike(null, english, "汉语$SHARED_HAN")
    }

    @Test
    fun `Latin letters and figures are Newsreader in every language`() {
        for (locale in listOf(Locale.US, Locale.SIMPLIFIED_CHINESE, Locale.JAPANESE, ARABIC)) {
            val typeface = interfaceIn(locale)
            assertDrawnLike(NEWSREADER, typeface, "Deck 1234", "$locale")
        }
    }

    @Test
    fun `a script without a bundled face uses the system font`() {
        for (locale in listOf(Locale.US, ARABIC)) {
            assertDrawnLike(null, interfaceIn(locale), "مجموعة", "$locale")
        }
    }

    private fun interfaceIn(locale: Locale): Typeface {
        val configuration = Configuration(appContext.resources.configuration).apply { setLocales(LocaleList(locale)) }
        return appContext.createConfigurationContext(configuration).interfaceTypeface()
    }

    /** Asserts that [typeface] draws [text] exactly as [face] alone does (`null`: the system font) */
    private fun assertDrawnLike(
        @FontRes face: Int?,
        typeface: Typeface,
        text: String,
        message: String = "",
    ) = assertTrue(draw(typeface, text).sameAs(draw(only(face), text)), "$text should be drawn in ${name(face)} $message")

    private fun assertNotDrawnLike(
        @FontRes face: Int?,
        typeface: Typeface,
        text: String,
    ) = assertFalse(draw(typeface, text).sameAs(draw(only(face), text)), "$text should not be drawn in ${name(face)}")

    private fun name(
        @FontRes face: Int?,
    ) = face?.let { appContext.resources.getResourceEntryName(it) } ?: "the system font"

    /**
     * A typeface of [face] alone, falling back to the system font; `null`: the system font.
     * Newsreader is drawn at its text optical size.
     */
    private fun only(
        @FontRes face: Int?,
    ): Typeface {
        if (face == null) return Typeface.create("serif", Typeface.NORMAL)
        val font =
            Font
                .Builder(appContext.resources, face)
                .setWeight(400)
                .apply { if (face == NEWSREADER) setFontVariationSettings("'opsz' 16") }
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
        val bitmap = Bitmap.createBitmap(600, 80, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            drawText(text, 4f, 60f, paint)
        }
        return bitmap
    }

    companion object {
        /** Drawn differently in Japanese and Chinese: the inner strokes of 骨 and the base of 直 */
        const val SHARED_HAN = "骨直"
        val NEWSREADER = R.font.newsreader
        val LXGW_WENKAI = R.font.lxgw_wenkai_regular
        val KLEE_ONE = R.font.klee_one_regular
        val ARABIC: Locale = Locale.forLanguageTag("ar")
    }
}
