// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ichi2.anki.common.utils.dp
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.robolectric.Robolectric.buildActivity
import kotlin.test.assertFailsWith

/** A screen held to [CheckAccessibility] fails at capture on a contrast or touch-target problem */
@CheckAccessibility
class AccessibilityAtCaptureTest : ScreenshotTest() {
    @Test
    fun `low-contrast text fails the capture`() =
        withScreen(text(color = Color.rgb(0xBB, 0xBB, 0xBB))) {
            val failure = assertFailsWith<Throwable> { captureScreen("low_contrast") }
            assertThat(failure.message, containsString("TextContrastCheck"))
        }

    @Test
    fun `a small touch target fails the capture`() =
        withScreen(text(color = Color.BLACK), button(sizeDp = 24)) {
            val failure = assertFailsWith<Throwable> { captureScreen("small_touch_target") }
            assertThat(failure.message, containsString("TouchTargetSizeCheck"))
        }

    @Test
    fun `readable text and large touch targets pass`() =
        withScreen(text(color = Color.BLACK), button(sizeDp = 48)) {
            captureScreen("passes")
        }

    private fun text(color: Int): (LinearLayout) -> View =
        { parent ->
            TextView(parent.context).apply {
                text = "Study now"
                textSize = 18f
                setTextColor(color)
            }
        }

    private fun button(sizeDp: Int): (LinearLayout) -> View =
        { parent ->
            View(parent.context).apply {
                val size = sizeDp.dp.toPx(parent.context)
                layoutParams = LinearLayout.LayoutParams(size, size)
                setBackgroundColor(Color.BLACK)
                contentDescription = "Add"
                setOnClickListener { }
            }
        }

    private fun withScreen(
        vararg children: (LinearLayout) -> View,
        block: () -> Unit,
    ) {
        buildActivity(AnkiActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            val layout =
                LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.WHITE)
                }
            children.forEach { layout.addView(it(layout)) }
            activity.setContentView(layout)
            advanceRobolectricLooper()
            block()
        }
    }
}
