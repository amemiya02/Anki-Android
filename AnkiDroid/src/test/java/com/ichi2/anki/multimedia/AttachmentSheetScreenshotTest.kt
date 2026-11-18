// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.multimedia

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.DrawingFragment
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.R
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.SingleFragmentActivity
import org.junit.Rule
import org.junit.Test

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class AttachmentSheetScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun showsAttachmentChoices() {
        setPhoneQualifiers()
        val activity =
            startActivityNormallyOpenCollectionWithIntent(
                SingleFragmentActivity::class.java,
                DrawingFragment.getIntent(targetContext),
            )
        MultimediaBottomSheet.forField(0, "Front").showNow(activity.supportFragmentManager, MultimediaBottomSheet.TAG)
        advanceRobolectricLooper()
        compose.onNodeWithText(getResourceString(R.string.note_editor_attach_image)).performScrollTo().assertIsDisplayed()
        captureScreen("image_choices")

        val video = compose.onNodeWithText(getResourceString(R.string.note_editor_attach_video_file))
        video.performScrollTo().assertIsDisplayed()
        captureScreen("audio_video_choices")
        video.performClick()
        compose.onNodeWithText(activity.getString(R.string.note_editor_attach_to_field, "Front")).assertDoesNotExist()
    }
}
