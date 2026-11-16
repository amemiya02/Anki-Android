// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.drawToBitmap
import androidx.test.filters.SdkSuppress
import com.google.testing.junit.testparameterinjector.TestParameter
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.settings.PrefsRepository
import com.ichi2.anki.settings.enums.AppTheme
import com.ichi2.anki.settings.enums.NightTheme
import com.ichi2.testutils.dispatchInsets
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

/**
 * The old study screen ([Reviewer]) is not redesigned (ADR-0004), but it must stay legible in
 * every theme: the 'Show answer' label can be read, and the strip below the answer buttons is
 * the same colour as the buttons, with navigation bar icons that can be seen on it.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
@RunWith(RobolectricTestParameterInjector::class)
class ReviewerLegibilityTest : RobolectricTest() {
    @TestParameter
    lateinit var theme: ThemeConfig

    // rendering a card requires a media folder
    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @Test
    fun `the Show answer label can be read on its button`() =
        withReviewer { reviewer ->
            val button = reviewer.findViewById<ViewGroup>(R.id.flashcard_layout_flip)
            val label = button.getChildAt(0) as TextView

            assertThat(
                "contrast of 'Show answer' on its button",
                ColorUtils.calculateContrast(label.currentTextColor, button.renderedColour()),
                greaterThanOrEqualTo(4.5),
            )
        }

    @Test
    fun `the strip below the answer buttons is the colour of the buttons`() =
        withReviewer { reviewer ->
            val button = reviewer.findViewById<View>(R.id.flashcard_layout_flip)
            val answerArea = reviewer.findViewById<View>(R.id.answer_options_layout)
            val strip = answerArea.drawToBitmap()

            assertThat(
                "the colour under the navigation bar is the button's colour, not a band of its own",
                strip.getPixel(strip.width / 2, strip.height - 1).toHex(),
                equalTo(button.renderedColour().toHex()),
            )
        }

    @Test
    fun `navigation bar icons follow the theme`() =
        withReviewer { reviewer ->
            val window = reviewer.window
            assertThat(
                "dark navigation bar icons on Light's paper, light icons on Dark and Black",
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars,
                equalTo(!theme.isNight),
            )
        }

    /** The colour a view draws at the middle of its top edge, clear of any label */
    private fun View.renderedColour(): Int = drawToBitmap().let { it.getPixel(it.width / 2, 1) }

    private fun Int.toHex() = "#%08X".format(this)

    private fun withReviewer(block: (Reviewer) -> Unit) {
        PrefsRepository(targetContext).apply {
            appTheme = if (theme.isNight) AppTheme.NIGHT else AppTheme.DAY
            (theme.theme as? NightTheme)?.let { nightTheme = it }
        }
        addBasicNote("Hello", "World")
        val reviewer = ReviewerTest.startReviewer(this)
        reviewer.dispatchInsets(navBarBottom = 48.dp)
        advanceRobolectricLooper()
        block(reviewer)
    }
}
