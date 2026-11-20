// SPDX-License-Identifier: GPL-3.0-or-later
package com.ichi2.anki.cardviewer

import android.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.previewer.stdHtml
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContains
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class CardStudyCanvasTest : RobolectricTest() {
    @Test
    fun `study page uses the selected theme's default canvas in each appearance`() {
        val cases =
            listOf(
                Triple(CommonR.style.HayakiTheme_Paper_Light, false, "#F6F1E7"),
                Triple(CommonR.style.HayakiTheme_Paper_Dark, true, "#1E1A15"),
                Triple(CommonR.style.HayakiTheme_Midnight_Light, false, "#EEF3FA"),
                Triple(CommonR.style.HayakiTheme_Midnight_Dark, true, "#0B1426"),
                Triple(CommonR.style.HayakiTheme_Paper_Black, true, "#000000"),
                Triple(CommonR.style.HayakiTheme_Midnight_Black, true, "#000000"),
            )
        for ((style, night, colour) in cases) {
            val page = stdHtml(ContextThemeWrapper(targetContext, style), nightMode = night)
            assertContains(page, "--canvas: $colour;")
        }
    }
}
