// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.compose.theme

import android.content.Context
import android.graphics.Typeface
import android.view.ContextThemeWrapper
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.test.core.app.ApplicationProvider
import com.ichi2.anki.common.android.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

/**
 * Compose draws each type role at its weight. A `FontFamily` wrapping one Android [Typeface]
 * ignores the requested weight, so every role drew at 400.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [Config.TARGET_SDK])
class FoundationWeightTest {
    private val context: Context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_Light)

    private fun TextStyle.drawnWeight(): Int {
        val resolver = createFontFamilyResolver(context)
        val typeface = resolver.resolve(fontFamily, fontWeight ?: FontWeight.Normal).value as Typeface
        return typeface.weight
    }

    @Test
    fun `each role is drawn at its weight`() {
        val type = context.toFoundation().type
        assertEquals(500, type.answer.drawnWeight(), "answer")
        assertEquals(500, type.studyTitle.drawnWeight(), "studyTitle")
        assertEquals(400, type.body.drawnWeight(), "body")
    }

    @Test
    fun `a role copied at another weight is drawn at that weight`() {
        val count = context.toFoundation().type.count
        assertEquals(400, count.copy(fontWeight = FontWeight.Normal).drawnWeight())
    }
}
