// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import android.view.View
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.previewer.CardViewerActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ReviewerBigScreenTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    @Config(qualifiers = "w1000dp-h1000dp-480dpi")
    fun `the reviewer centers the Card and controls on a wide display`() {
        ensureCollectionLoadIsSynchronous()
        addBasicNote()
        val intent = ReviewerFragment.getIntent(targetContext)
        ActivityScenario.launch<CardViewerActivity>(intent).use { scenario ->
            composeRule.waitForIdle()
            scenario.onActivity { activity ->
                val frame = assertNotNull(activity.findViewById<View>(R.id.main_layout))
                val card = assertNotNull(frame.findViewById<View>(R.id.web_view_container))
                val controls = assertNotNull(frame.findViewById<View>(R.id.bottom_layout))
                val density = card.resources.displayMetrics.density
                assertTrue(frame.width > (820 * density).roundToInt())
                assertEquals((820 * density).roundToInt(), card.width)
                assertEquals((640 * density).roundToInt(), controls.width)
                assertTrue(abs(frame.width - (card.left + card.right)) <= 1)
                assertTrue(abs(frame.width - (controls.left + controls.right)) <= 1)
            }
        }
    }
}
