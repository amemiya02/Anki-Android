// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.instanteditor

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.instantnoteeditor.InstantNoteEditorActivity
import org.junit.Rule
import org.junit.Test

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class InstantEditorScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun selectionAndAdvancedEditing() {
        setPhoneQualifiers()
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Tokyo 東京")
            }
        startActivityNormallyOpenCollectionWithIntent(InstantNoteEditorActivity::class.java, intent)
        val word = compose.onNodeWithText("Tokyo")
        word.performScrollTo().performClick()
        word.assertIsSelected()
        captureScreen("single_tap")

        compose.onNodeWithContentDescription(getResourceString(CommonString.change_editor_mode)).performScrollTo().performClick()
        compose.onNodeWithText("{{c1::Tokyo}} 東京").performScrollTo().assertIsDisplayed()
        captureScreen("advanced")
    }
}
