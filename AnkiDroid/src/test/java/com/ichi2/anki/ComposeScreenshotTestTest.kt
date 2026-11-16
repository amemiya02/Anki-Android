// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.ichi2.anki.FontSizeConfig.NORMAL
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.ENGLISH
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import com.ichi2.anki.common.android.R as CommonR

/** Meta tests for [ComposeScreenshotTest] */
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class ComposeScreenshotTestTest : ComposeScreenshotTest() {
    @Test
    fun `content is shown in the declared language and its direction`() {
        val expectedCancel =
            when (language) {
                ENGLISH -> "Cancel"
                SIMPLIFIED_CHINESE -> "取消"
                JAPANESE -> "キャンセル"
                ARABIC -> "إلغاء"
            }
        var direction: LayoutDirection? = null
        capture("language", beforeCapture = { composeRule.onNodeWithText(expectedCancel).assertIsDisplayed() }) {
            direction = LocalLayoutDirection.current
            Text(stringResource(CommonR.string.dialog_cancel))
        }
        assertThat(direction, equalTo(if (language == ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr))
    }

    @Test
    fun `content is shown at the declared font size`() {
        var fontScale: Float? = null
        capture("font_size") {
            fontScale = LocalDensity.current.fontScale
            Text("Text")
        }
        val expectedScale =
            when (fontSize) {
                NORMAL -> 1.0f
                PERCENT_200 -> 2.0f
            }
        assertThat(fontScale, equalTo(expectedScale))
    }
}
