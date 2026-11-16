// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.common.android.themes

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.Build
import androidx.annotation.FontRes
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import com.ichi2.anki.common.android.R
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * The optical size Newsreader is drawn at unless a style asks for another: the face's default,
 * which suits text from 12 to 20sp.
 */
const val TEXT_OPTICAL_SIZE = 16f

/**
 * The typeface of interface text in [this] context's language, at its regular weight. Derive other
 * weights with `Typeface.create(typeface, weight, false)`.
 *
 * The Latin face comes first, so Latin letters and figures are always Newsreader. Chinese and
 * Japanese text falls to the bundled faces, chosen by the interface language so that Han characters
 * shared by both languages take that language's forms: Klee One then LXGW WenKai in Japanese, LXGW
 * WenKai in every other language. Any other script falls to the system font.
 *
 * A typeface can only fall back to faces of the app's choosing from Android 10. Before that it is the
 * language's own face alone (`res/font*` `foundation_interface`): Klee One in Japanese, LXGW WenKai in
 * Chinese, Newsreader otherwise; letters it lacks come from the system font. If it cannot be loaded,
 * the system serif font is used.
 *
 * @param opticalSize the size Newsreader's shapes are drawn for, in sp: larger sizes are finer
 */
fun Context.interfaceTypeface(opticalSize: Float = TEXT_OPTICAL_SIZE): Typeface {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        // AndroidX copies the font to a file to load it there, which can fail: text is still drawn
        return try {
            ResourcesCompat.getFont(this, R.font.foundation_interface)
        } catch (e: Resources.NotFoundException) {
            Timber.w(e, "interface typeface could not be loaded")
            null
        } ?: Typeface.SERIF
    }
    return InterfaceFaces.typeface(resources.configuration.locales[0], opticalSize, applicationContext.resources)
}

/** The bundled faces, loaded once: each font file is mapped from the app, not copied */
@RequiresApi(Build.VERSION_CODES.Q)
private object InterfaceFaces {
    /** The weights a family answers to exactly. Text asking for another weight gets the nearest. */
    private val weights = listOf(400, 500, 600, 700)

    private val typefaces = ConcurrentHashMap<Pair<Boolean, Float>, Typeface>()

    @Volatile private var chinese: FontFamily? = null

    @Volatile private var japanese: FontFamily? = null

    fun typeface(
        locale: Locale,
        opticalSize: Float,
        resources: Resources,
    ): Typeface {
        val isJapanese = locale.language == Locale.JAPANESE.language
        return typefaces.getOrPut(isJapanese to opticalSize) {
            val cjk = if (isJapanese) listOf(japanese(resources), chinese(resources)) else listOf(chinese(resources))
            Typeface
                .CustomFallbackBuilder(latin(resources, opticalSize))
                .apply { cjk.forEach { addCustomFallback(it) } }
                .setSystemFallback("serif")
                .build()
        }
    }

    /** Newsreader is one variable font: each weight is an instance of it */
    private fun latin(
        resources: Resources,
        opticalSize: Float,
    ): FontFamily {
        val fonts =
            weights.map { weight ->
                Font
                    .Builder(resources, R.font.newsreader)
                    .setWeight(weight)
                    .setFontVariationSettings("'wght' $weight, 'opsz' ${opticalSize.coerceIn(6f, 72f)}")
                    .build()
            }
        return fonts.drop(1).fold(FontFamily.Builder(fonts.first())) { builder, font -> builder.addFont(font) }.build()
    }

    /*
     * The CJK faces have a regular and a heavier face. Weight 500 takes the regular one and 600 the
     * heavier one, as in the Mockups: the browser that drew them chooses faces the same way.
     */
    private fun chinese(resources: Resources) =
        chinese ?: family(resources, R.font.lxgw_wenkai_regular, R.font.lxgw_wenkai_medium).also { chinese = it }

    private fun japanese(resources: Resources) =
        japanese ?: family(resources, R.font.klee_one_regular, R.font.klee_one_semibold).also { japanese = it }

    private fun family(
        resources: Resources,
        @FontRes regular: Int,
        @FontRes heavier: Int,
    ): FontFamily {
        val fonts = weights.map { weight -> Font.Builder(resources, if (weight < 600) regular else heavier).setWeight(weight).build() }
        return fonts.drop(1).fold(FontFamily.Builder(fonts.first())) { builder, font -> builder.addFont(font) }.build()
    }
}
