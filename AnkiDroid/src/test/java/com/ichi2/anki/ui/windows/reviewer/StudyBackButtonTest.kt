// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import android.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertTrue
import com.ichi2.anki.common.android.R as CommonR

/** The study bar's Back arrow is drawn in the bar's ink: in Dark it was the default black on the dark bar. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StudyBackButtonTest : RobolectricTest() {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `Back is drawn in the bar's light ink in Dark`() {
        val dark = ContextThemeWrapper(targetContext, CommonR.style.Theme_Dark)
        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides dark) {
                AnkiDroidTheme {
                    // the button sits on the study bar, which is the page's background
                    Box(Modifier.background(MaterialTheme.foundation.colors.background)) { StudyBackButton(onClick = {}) }
                }
            }
        }

        val pixels = composeRule.onNodeWithContentDescription("Navigate up").captureToImage().toPixelMap()
        val brightest =
            (0 until pixels.width)
                .flatMap { x -> (0 until pixels.height).map { y -> pixels[x, y] } }
                .maxOfOrNull { it.luminance() } ?: 0f
        assertTrue(brightest > 0.5f, "the arrow's brightest pixel has luminance $brightest: it is drawn dark on Dark's bar")
    }
}
