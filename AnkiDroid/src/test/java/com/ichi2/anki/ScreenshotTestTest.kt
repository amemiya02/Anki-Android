// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.view.View
import com.ichi2.anki.FontSizeConfig.NORMAL
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.ENGLISH
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.utils.VersionUtils
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.robolectric.Robolectric.buildActivity
import com.ichi2.anki.common.android.R as CommonR

/** Meta tests for [ScreenshotTest] */
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class ScreenshotTestTest : ScreenshotTest() {
    @Test
    fun `version name does not change between releases - issue 21453`() {
        // 'Info' displays the version in its toolbar, which shows as a break in screenshot diffs
        assertThat(VersionUtils.pkgVersionName, equalTo(STABLE_VERSION_NAME))
    }

    @Test
    fun `a screen is shown in the declared language and its direction`() {
        buildActivity(AnkiActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            val expectedCancel =
                when (language) {
                    ENGLISH -> "Cancel"
                    SIMPLIFIED_CHINESE -> "取消"
                    JAPANESE -> "キャンセル"
                    ARABIC -> "إلغاء"
                }
            assertThat(activity.getString(CommonR.string.dialog_cancel), equalTo(expectedCancel))
            val expectedDirection = if (language == ARABIC) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            assertThat(activity.window.decorView.layoutDirection, equalTo(expectedDirection))
        }
    }

    @Test
    fun `a screen is shown at the declared font size`() {
        buildActivity(AnkiActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            val expectedScale =
                when (fontSize) {
                    NORMAL -> 1.0f
                    PERCENT_200 -> 2.0f
                }
            assertThat(activity.resources.configuration.fontScale, equalTo(expectedScale))
        }
    }
}
