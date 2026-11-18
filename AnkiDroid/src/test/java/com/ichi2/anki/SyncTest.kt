// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.app.Dialog
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.backend.backendError
import anki.sync.SyncAuth
import anki.sync.SyncCollectionResponse
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.common.time.MockTime
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.dialogs.SyncErrorDialog
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.testutils.signInToSyncServer
import kotlinx.coroutines.runBlocking
import net.ankiweb.rsdroid.Backend
import net.ankiweb.rsdroid.BackendFactory
import net.ankiweb.rsdroid.exceptions.BackendSyncException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import java.text.DateFormat
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SyncTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var syncFailure: BackendSyncException

    @Before
    fun setUpFailingSync() {
        runBlocking { CollectionManager.discardBackend() }
        BackendFactory.setOverride {
            object : Backend() {
                override fun syncCollection(
                    auth: SyncAuth,
                    syncMedia: Boolean,
                ): SyncCollectionResponse = throw syncFailure
            }
        }
    }

    @After
    fun resetBackendFactory() {
        BackendFactory.setOverride(null)
    }

    @Test
    fun `clock errors use sync recovery dialog and open date settings`() =
        deckPicker {
            TimeManager.resetWith(MockTime(1_600_000_000_000))
            val currentTime = DateFormat.getDateTimeInstance().format(TimeManager.time.currentDate)
            val exception = BackendSyncException(backendError { message = CollectionManager.TR.syncClockOff() })

            val dialog = failSync(exception)

            assertTrue(supportFragmentManager.fragments.any { it is SyncErrorDialog })
            visible(CommonString.paper_sync_clock_error).assertIsDisplayed()
            visible(CommonString.paper_dialog_show_details).performClick()
            compose.onNodeWithText(currentTime).assertIsDisplayed()
            visible(CommonString.dialog_cancel).assertIsDisplayed()
            assertEquals("test", Prefs.hkey)

            visible(CommonString.open_settings).performClick()
            advanceRobolectricLooper()

            val intent = assertNotNull(shadowOf(this).nextStartedActivity)
            assertEquals(Settings.ACTION_DATE_SETTINGS, intent.action)
            assertFalse(dialog.isShowing)
        }

    @Test
    fun `cancelling a clock error does not open settings`() =
        deckPicker {
            val dialog = failSync(BackendSyncException(backendError { message = CollectionManager.TR.syncClockOff() }))

            visible(CommonString.dialog_cancel).performClick()
            advanceRobolectricLooper()

            assertFalse(dialog.isShowing)
            assertNull(shadowOf(this).nextStartedActivity)
        }

    @Test
    fun `clock error remains visible when date settings are unavailable`() =
        deckPicker {
            val dialog = failSync(BackendSyncException(backendError { message = CollectionManager.TR.syncClockOff() }))
            val applicationShadow = shadowOf(application)
            applicationShadow.checkActivities(true)
            try {
                visible(CommonString.open_settings).performClick()
                advanceRobolectricLooper()

                assertTrue(dialog.isShowing)
                assertNull(shadowOf(this).nextStartedActivity)

                visible(CommonString.dialog_cancel).performClick()
                advanceRobolectricLooper()
                assertFalse(dialog.isShowing)
            } finally {
                applicationShadow.checkActivities(false)
            }
        }

    @Test
    fun `other sync errors keep their message and OK button`() =
        deckPicker {
            val message = "other sync error"
            val dialog = failSync(BackendSyncException(backendError { this.message = message }))

            compose.onNodeWithText(message).assertIsDisplayed()
            visible(CommonString.dialog_cancel).assertDoesNotExist()
            assertEquals("test", Prefs.hkey)

            compose
                .onNode(
                    hasText(getString(CommonString.dialog_ok)) and hasClickAction() and hasAnyAncestor(hasAnyDescendant(hasText(message))),
                ).performClick()
            advanceRobolectricLooper()

            assertFalse(dialog.isShowing)
            assertNull(shadowOf(this).nextStartedActivity)
        }

    @Test
    fun `collection too large uses database check dialog`() =
        deckPickerEx {
            val message = TR.syncUploadTooLarge("300.14 MB > 300.00 MB")

            val dialog = failSync(BackendSyncException(backendError { this.message = message }))

            visible(CommonString.paper_sync_too_large).assertIsDisplayed()
            visible(CommonString.paper_dialog_show_details).performClick()
            compose.onNodeWithText(message).assertIsDisplayed()
            visible(CommonString.dialog_cancel).assertIsDisplayed()
            compose.onNodeWithText(TR.sentenceCase.checkDatabase).performClick()
            advanceRobolectricLooper()

            assertFalse(dialog.isShowing)
            assertTrue(integrityCheckCalled)
        }

    @Test
    fun `collection too large error is detected from raw byte backend message`() =
        deckPicker {
            val message =
                "Some localized backend message: 123456789 > 314572800"

            failSync(BackendSyncException(backendError { this.message = message }))

            visible(CommonString.paper_sync_too_large).assertIsDisplayed()
        }

    @Test
    fun `collection too large error is detected from localized backend message`() =
        deckPicker {
            val message =
                "AnkiWebକୁ ପଠାଇବା ପାଇଁ ଆପଣଙ୍କ ସଂଗ୍ରହ ଫାଇଲ୍ ବହୁତ ବଡ଼ ଅଟେ। " +
                    "ଆପଣ ଯେକୌଣସି ଅବାଞ୍ଛିତ ଡେକ୍ ଅପସାରଣ କରି ଏହାର ଆକାର ହ୍ରାସ କରିପାରିବେ " +
                    "(ବୈକଳ୍ପିକ ଭାବରେ ସେଗୁଡ଼ିକୁ ପ୍ରଥମେ ରପ୍ତାନି କରନ୍ତୁ), " +
                    "ଏବଂ ତାପରେ ଫାଇଲ୍ ଆକାର ସଙ୍କୁଚିତ କରିବାକୁ ଡାଟାବେସ୍ ଯାଞ୍ଚ ବ୍ୟବହାର କରିପାରିବେ। " +
                    "(300.14 MB > 300.00 MB)"

            failSync(BackendSyncException(backendError { this.message = message }))

            visible(CommonString.paper_sync_too_large).assertIsDisplayed()
            visible(CommonString.paper_dialog_show_details).performClick()
            compose.onNodeWithText(message).assertIsDisplayed()
        }

    @Test
    fun `authentication errors still log out`() =
        deckPicker {
            Prefs.username = "test user"
            val message = "authentication failed"
            failSync(BackendSyncException.BackendSyncAuthFailedException(backendError { this.message = message }))

            assertEquals("", Prefs.hkey)
            assertEquals("", Prefs.username)
            visible(CommonString.paper_sync_sign_in).assertIsDisplayed()
        }

    private fun visible(resource: Int) = compose.onNodeWithText(targetContext.getString(resource))

    private fun DeckPicker.failSync(exception: BackendSyncException): Dialog {
        syncFailure = exception
        signInToSyncServer()
        Prefs.lastSyncTime = 0
        throwOnShowError = false

        handleNewSync(conflict = null, syncMedia = false)
        advanceRobolectricLooperUntil { Prefs.lastSyncTime != 0L }

        return assertNotNull(ShadowDialog.getLatestDialog())
    }
}
