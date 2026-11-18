// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.dialogs.SyncErrorDialog
import com.ichi2.anki.dialogs.SyncErrorDialog.Type
import com.ichi2.anki.utils.ext.DIALOG_FRAGMENT_TAG
import com.ichi2.testutils.TEST_SYNC_SERVER
import com.ichi2.testutils.ext.reopenWithLanguage
import com.ichi2.testutils.setSyncServer
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertNotNull

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class SyncErrorsScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun displaysPlainLanguageErrorsAndConflictChoices() {
        setPhoneQualifiers()
        runTest { col.reopenWithLanguage(language.option) }
        setSyncServer(TEST_SYNC_SERVER)
        val cases =
            listOf(
                Triple(Type.DIALOG_CONNECTION_ERROR, CommonString.paper_sync_unreachable_title, "unreachable"),
                Triple(Type.DIALOG_USER_NOT_LOGGED_IN_SYNC, CommonString.paper_sync_sign_in, "sign_in_required"),
                Triple(Type.DIALOG_SYNC_CONFLICT_RESOLUTION, CommonString.paper_sync_conflict_title, "conflict"),
                Triple(Type.DIALOG_SYNC_SANITY_ERROR, CommonString.paper_sync_sanity, "sanity"),
                Triple(Type.DIALOG_SYNC_BASIC_CHECK_ERROR, CommonString.paper_sync_db_error, "check_database"),
                Triple(Type.DIALOG_MEDIA_SYNC_ERROR, CommonString.paper_sync_media_error, "check_media"),
                Triple(Type.DIALOG_SYNC_CORRUPT_COLLECTION, CommonString.paper_sync_corrupt_error, "corrupt"),
                Triple(Type.DIALOG_SYNC_CLOCK_OFF, CommonString.paper_sync_clock_error, "clock"),
                Triple(Type.DIALOG_COLLECTION_TOO_LARGE, CommonString.paper_sync_too_large, "too_large"),
            )
        withDeckPicker(deckCount = 0) { deckPicker ->
            for ((type, label, capture) in cases) {
                deckPicker.showSyncErrorDialog(type)
                advanceRobolectricLooper()
                compose.onNodeWithText(targetContext.getString(label)).assertIsDisplayed()
                captureScreen(capture)
                if (type == Type.DIALOG_SYNC_CONFLICT_RESOLUTION || type == Type.DIALOG_SYNC_SANITY_ERROR) {
                    compose
                        .onNodeWithText(
                            targetContext.getString(CommonString.paper_sync_keep_phone),
                        ).performScrollTo()
                        .assertIsDisplayed()
                    compose
                        .onNodeWithText(
                            targetContext.getString(CommonString.paper_sync_keep_server),
                        ).performScrollTo()
                        .assertIsDisplayed()
                }
                assertNotNull(deckPicker.supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) as? SyncErrorDialog).dismiss()
                advanceRobolectricLooper()
            }
        }
    }
}
