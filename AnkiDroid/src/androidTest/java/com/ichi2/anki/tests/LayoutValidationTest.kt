// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.tests

import android.os.Build
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.ichi2.anki.widgets.R
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/** Android 12's RemoteViews API inflates these views to render widget marks. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class LayoutValidationTest : InstrumentedTest() {
    @Test
    fun widgetDrawableHostsInflate() {
        val drawableHosts =
            listOf(
                R.layout.widget_add_note_drawable_v31,
                R.layout.widget_card_analysis_drawable_v31,
                R.layout.widget_deck_picker_drawable_v31,
                R.layout.widget_small_drawable_v31,
            )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val inflater = LayoutInflater.from(testContext)
            for (layout in drawableHosts) {
                val image = inflater.inflate(layout, null) as ImageView
                assertNotNull(image.drawable)
            }
        }
    }
}
