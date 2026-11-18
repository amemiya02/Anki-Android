// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.dialogs.DialogHandler
import com.ichi2.testutils.BackupManagerTestUtilities
import com.ichi2.utils.ImportResolution
import com.ichi2.utils.ImportResult
import com.ichi2.utils.ImportUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Import handling tests for [IntentHandler]. */
@RunWith(AndroidJUnit4::class)
class IntentHandlerImportTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun prepareImport() {
        setIntroductionSlidesShown(true)
        mockkObject(ImportUtils)
    }

    @After
    fun resetImport() {
        unmockkObject(ImportUtils)
    }

    @Test
    fun `import failure stays visible until dismissed`() =
        runTest {
            val failure = ImportResult.Failure("The selected file is no longer available")
            every { ImportUtils.prepareImport(any(), any()) } returns failure

            val activity = launchImport()

            compose.onNodeWithText(failure.humanReadableMessage).assertIsDisplayed()
            assertFalse(activity.isFinishing)

            compose.onNodeWithText(activity.getString(CommonString.dialog_ok)).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(activity.isFinishing)
        }

    @Test
    fun `successful import finishes the intent handler`() =
        runTest {
            every { ImportUtils.prepareImport(any(), any()) } returns
                ImportResult.Success(
                    ImportResolution.DeckPackage(ImportResolution.Source("content://import/deck.apkg".toUri(), "deck.apkg")),
                    "/cache/deck.apkg",
                )

            assertTrue(launchImport().isFinishing)
        }

    @Test
    fun `finishing before preparation completes suppresses import results`() =
        runTest {
            val source = ImportResolution.Source("content://import/deck.apkg".toUri(), "deck.apkg")
            val results =
                listOf(
                    ImportResult.Success(ImportResolution.DeckPackage(source), "/cache/deck.apkg"),
                    ImportResult.Success(ImportResolution.Text(source), "/cache/notes.csv"),
                    ImportResult.Failure("The selected file is no longer available"),
                )
            val dialogHandler = DialogHandler(mockk<AnkiActivity>())
            for (result in results) {
                val ioScheduler = TestCoroutineScheduler()
                ioDispatcher = StandardTestDispatcher(ioScheduler)
                every { ImportUtils.prepareImport(any(), any()) } returns result
                val activity = launchImport()
                verify(exactly = 0) { ImportUtils.prepareImport(activity, any()) }

                activity.finish()
                ioScheduler.runCurrent()

                verify(exactly = 1) { ImportUtils.prepareImport(activity, any()) }
                assertFalse(activity.isDestroyed, "Exercise the window before onDestroy cancels the import scope")
                assertNull(shadowOf(activity).nextStartedActivity, "A dismissed import must not launch an activity")
                assertNull(dialogHandler.popMessage(), "A dismissed import must not leave a pending confirmation")
                assertNull(ShadowDialog.getLatestDialog(), "A dismissed import must not show an error")
            }
        }

    @Test
    fun `package import failure from a text intent stays visible until dismissed`() =
        runTest {
            val failure = ImportResult.Failure("The selected file is no longer available")
            every { ImportUtils.prepareImport(any(), any()) } returns failure

            val activity = launchImport("text/plain")

            compose.onNodeWithText(failure.humanReadableMessage).assertIsDisplayed()
            assertFalse(activity.isFinishing)

            compose.onNodeWithText(activity.getString(CommonString.dialog_ok)).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(activity.isFinishing)
        }

    @Test
    fun `an external package survives Welcome and is prepared once after setup`() {
        setIntroductionSlidesShown(false)
        ensureCollectionLoadIsSynchronous()
        BackupManagerTestUtilities.setupSpaceForBackup(targetContext)
        val failure = ImportResult.Failure("The selected file is no longer available")
        every { ImportUtils.prepareImport(any(), any()) } returns failure

        val handler = launchImport()
        assertTrue(handler.isFinishing)
        val entry = assertNotNull(shadowOf(handler).nextStartedActivity)
        assertEquals(DeckPicker::class.java.name, entry.component?.className)
        assertEquals("content://import/deck.apkg".toUri(), entry.clipData?.getItemAt(0)?.uri)
        assertTrue(entry.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)

        val controller = startActivityControllerNormallyOpenCollectionWithIntent(DeckPicker::class.java, entry)
        val welcome = compose.onNodeWithText(getResourceString(CommonString.paper_welcome_start_empty))
        welcome.assertIsDisplayed()
        verify(exactly = 0) { ImportUtils.prepareImport(any(), any()) }
        controller.recreate()
        advanceRobolectricLooper()
        welcome.performClick()
        compose.onNodeWithText(failure.humanReadableMessage).assertIsDisplayed()
        verify(exactly = 1) { ImportUtils.prepareImport(any(), any()) }
    }

    private fun launchImport(mimeType: String? = null): IntentHandler {
        val intent =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType("content://import/deck.apkg".toUri(), mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val controller = Robolectric.buildActivity(IntentHandler::class.java, intent)
        saveControllerForCleanup(controller)
        return controller.create().get()
    }
}
