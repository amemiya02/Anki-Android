// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.widget

import android.content.Context
import android.os.Build
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.EmptyApplicationCategory
import com.ichi2.anki.widgets.R
import com.ichi2.testutils.EmptyApplication
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(application = EmptyApplication::class)
@Category(EmptyApplicationCategory::class)
class AnkiDroidWidgetSmallTest {
    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `widget preview shows Hayaki's mark on Android 12`() =
        withSmallWidgetPreview {
            assertEquals(R.drawable.widget_small_mark, shadowOf(drawable).createdFromResId)
        }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `widget preview shows Hayaki's mark before Android 12`() =
        withSmallWidgetPreview {
            assertEquals(R.drawable.widget_small_mark, shadowOf(drawable).createdFromResId)
        }
}

/** Inflates the widget picker preview. */
internal fun withSmallWidgetPreview(block: ImageView.() -> Unit) {
    val context = getApplicationContext<Context>()
    val previewLayout =
        context.resources.getXml(R.xml.widget_provider_small).use { metadata ->
            while (metadata.eventType != XmlPullParser.START_TAG) {
                metadata.next()
            }
            metadata.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "previewLayout", 0)
        }
    val view = RemoteViews(context.packageName, previewLayout).apply(context, null)
    (view as ImageView).block()
}
