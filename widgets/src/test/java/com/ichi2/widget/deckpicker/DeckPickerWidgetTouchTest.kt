// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.widget.deckpicker

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.allViews
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.common.destinations.Navigator
import com.ichi2.testutils.AndroidTest
import com.ichi2.testutils.targetContext
import com.ichi2.widget.WidgetAppearance
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@OptIn(androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi::class)
class DeckPickerWidgetTouchTest : AndroidTest {
    @Test
    fun compactDeckRowIsAtLeast48dp() = verifyDeckRow(width = 120, height = 50)

    @Test
    fun fullDeckRowsAreAtLeast48dp() = verifyDeckRow(width = 250, height = 200)

    private fun verifyDeckRow(
        width: Int,
        height: Int,
    ) = runBlocking {
        Navigator.register(
            object : Navigator {
                override fun toIntent(destination: com.ichi2.anki.common.destinations.Destination): Intent =
                    Intent(targetContext, android.app.Activity::class.java)
            },
        )
        val snapshot = DeckWidgetSnapshot(listOf(DeckWidgetData(123, "Algebra", 1, 2, 3)), totalDue = 6)
        val remote =
            GlanceRemoteViews()
                .compose(targetContext, DpSize(width.dp, height.dp)) {
                    DeckListContent(WidgetAppearance(targetContext), snapshot) { Intent() }
                }.remoteViews
        val host = FrameLayout(targetContext)
        val content = remote.apply(targetContext, host)
        host.addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val density = targetContext.resources.displayMetrics.density
        val widthPx = (width * density).roundToInt()
        val heightPx = (height * density).roundToInt()
        host.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, widthPx, heightPx)

        val label = host.allViews.filterIsInstance<TextView>().single { it.text.toString() == "Algebra" }
        val ancestors = generateSequence<View>(label) { it.parent as? View }.toList()
        val target =
            checkNotNull(ancestors.firstOrNull { it.isClickable }) {
                "Deck does not have a click target: " + ancestors.map { it.javaClass.simpleName to it.height }
            }
        assertTrue(target.height >= (48 * density).roundToInt(), "Deck opens within a ${target.height / density}dp-tall row")
        assertEquals(heightPx, host.height)
    }
}
