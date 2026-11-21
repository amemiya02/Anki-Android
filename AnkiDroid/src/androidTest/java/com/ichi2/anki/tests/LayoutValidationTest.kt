// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.tests

import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.view.allViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.ichi2.anki.widgets.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull

/** Checks the Android 12 widget-picker previews with the same inflater as widget hosts. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class LayoutValidationTest : InstrumentedTest() {
    @Test
    fun widgetPreviewsInflate() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val widgetHost = FrameLayout(testContext)
            for (layout in listOf(R.layout.widget_add_note_drawable_v31, R.layout.widget_small_drawable_v31)) {
                val mark = RemoteViews(testContext.packageName, layout).apply(testContext, widgetHost) as ImageView
                assertNotNull(mark.drawable)
            }
            val previewDeck = testContext.getString(R.string.deck1Name_deck_picker_widget)
            for (layout in listOf(R.layout.widget_card_analysis_drawable_v31, R.layout.widget_deck_picker_drawable_v31)) {
                val preview = RemoteViews(testContext.packageName, layout).apply(testContext, widgetHost) as ViewGroup
                assertNotNull(preview.background)
                val labels =
                    preview.allViews
                        .filterIsInstance<TextView>()
                        .map { it.text.toString() }
                        .toList()
                assertTrue("Missing deck name in widget preview", previewDeck in labels)
                assertTrue("Missing card counts in widget preview", labels.count { it.toIntOrNull() != null } >= 3)
            }
        }
    }
}
