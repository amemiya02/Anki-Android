// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import android.content.Context
import android.graphics.Color
import android.webkit.WebResourceResponse
import androidx.core.content.res.use
import com.google.android.material.color.MaterialColors
import com.ichi2.anki.common.android.R
import com.ichi2.utils.toRGBHex
import java.io.SequenceInputStream
import java.util.Locale

/** Hayaki's colours and fonts, served alongside the bundled Anki Pages. */
internal object AnkiPageStyle {
    private const val STYLESHEET = "/hayaki/anki-pages.css"
    private val fonts =
        mapOf(
            "/hayaki/newsreader.ttf" to R.font.newsreader,
            "/hayaki/lxgw-wenkai-regular.ttf" to R.font.lxgw_wenkai_regular,
            "/hayaki/lxgw-wenkai-medium.ttf" to R.font.lxgw_wenkai_medium,
            "/hayaki/klee-one-regular.ttf" to R.font.klee_one_regular,
            "/hayaki/klee-one-semibold.ttf" to R.font.klee_one_semibold,
        )

    fun attachTo(
        html: String,
        context: Context,
    ): String {
        val isLight =
            context.obtainStyledAttributes(R.styleable.ComposeTheme).use {
                it.getBoolean(R.styleable.ComposeTheme_isLightTheme, true)
            }
        val scheme = if (isLight) "light" else "dark"
        val isJapanese =
            context.resources.configuration.locales[0]
                .language == Locale.JAPANESE.language
        val language = if (isJapanese) "ja" else "default"
        return html
            .replaceFirst("<html ", """<html data-hayaki-page="$scheme" data-hayaki-font="$language" """)
            .replaceFirst("<head>", """<head><meta name="color-scheme" content="$scheme">""")
            .replaceFirst("</head>", """<link rel="stylesheet" href="$STYLESHEET"></head>""")
    }

    /** Exact paths on the page origin, backed by the same font resources as the native interface. */
    fun responseFor(
        context: Context,
        path: String,
    ): WebResourceResponse? {
        if (path == STYLESHEET) {
            val css = SequenceInputStream(paletteCss(context).byteInputStream(), context.assets.open("anki-pages.css"))
            return WebResourceResponse("text/css", "UTF-8", css).apply {
                // The URL is shared by every page and the active theme can change.
                responseHeaders = mapOf("Cache-Control" to "no-store")
            }
        }
        val font = fonts[path] ?: return null
        return WebResourceResponse("font/ttf", null, context.resources.openRawResource(font))
    }

    private fun paletteCss(context: Context): String =
        buildString {
            appendLine("html[data-hayaki-page] {")

            fun color(
                property: String,
                attr: Int,
                rgb: Boolean = false,
            ) {
                val color = MaterialColors.getColor(context, attr, "Anki Page")
                appendLine("  --hayaki-$property: ${color.toRGBHex()};")
                if (rgb) {
                    appendLine("  --hayaki-$property-rgb: ${Color.red(color)}, ${Color.green(color)}, ${Color.blue(color)};")
                }
            }
            color("paper", R.attr.foundationBackground, rgb = true)
            color("ink", R.attr.foundationOnBackground, rgb = true)
            color("muted", R.attr.foundationOnBackgroundMuted)
            color("rule", R.attr.foundationOutline)
            color("primary", R.attr.foundationPrimary, rgb = true)
            color("on-primary", R.attr.foundationOnPrimary)
            color("emphasis", R.attr.foundationEmphasis, rgb = true)
            color("new", R.attr.foundationCountNew)
            color("learning", R.attr.foundationCountLearning)
            color("review", R.attr.foundationCountDue)
            appendLine("}")
        }
}
