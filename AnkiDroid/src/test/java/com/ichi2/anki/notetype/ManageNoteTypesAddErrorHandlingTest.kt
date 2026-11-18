// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.backend.backendError
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.backend.DatabaseCorruption
import com.ichi2.anki.common.crashreporting.CrashReportService
import com.ichi2.anki.common.crashreporting.CrashReporter
import com.ichi2.anki.dialogs.DatabaseErrorDialog
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.addNotetypeLegacy
import com.ichi2.anki.throwOnShowError
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import net.ankiweb.rsdroid.BackendException.BackendDbException.BackendDbCorruptException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ManageNoteTypesAddErrorHandlingTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var originalCrashReporter: CrashReporter
    private val crashReporter = mock<CrashReporter>()

    @Before
    fun captureCrashReports() {
        throwOnShowError = false
        originalCrashReporter = CrashReportService.getReporter()
        CrashReportService.setReporter(crashReporter)
    }

    @After
    fun restoreCrashReporter() {
        CrashReportService.setReporter(originalCrashReporter)
    }

    @Test
    fun `collection open failure while adding shows an error without escaping the coroutine`() =
        runTest {
            openAddDialog()
            compose.fillName("New note type")

            val previousFailure = CollectionManager.emulatedOpenFailure
            CollectionManager.emulatedOpenFailure = CollectionManager.CollectionOpenFailure.LOCKED
            try {
                compose.confirmAdd()
                advanceRobolectricLooper()

                compose.onNodeWithText("Advanced settings", substring = true).assertIsDisplayed()
            } finally {
                CollectionManager.emulatedOpenFailure = previousFailure
            }
        }

    @Test
    fun `corruption during add opens database recovery`() =
        runTest {
            val activity = openAddDialog()
            compose.fillName("New note type")

            val previousCorruption = DatabaseCorruption.isDetected
            DatabaseCorruption.isDetected = false
            mockkStatic(Collection::addNotetypeLegacy)
            try {
                every { any<Collection>().addNotetypeLegacy(any()) } throws BackendDbCorruptException(backendError {})

                compose.confirmAdd()
                advanceRobolectricLooper()

                assertTrue(DatabaseCorruption.isDetected, "Corruption must be marked for recovery")
                assertTrue(
                    activity.supportFragmentManager.fragments.any { it is DatabaseErrorDialog },
                    "Corruption must open the database recovery dialog",
                )
            } finally {
                unmockkStatic(Collection::addNotetypeLegacy)
                DatabaseCorruption.isDetected = previousCorruption
            }
        }

    @Test
    fun `cloning an invalid legacy template shows validation without a crash report`() =
        runTest {
            val sourceName = "Invalid legacy note type"
            val cloneName = "Invalid template clone"
            // Legacy collections can contain templates that fail the current backend's validation.
            addStandardNoteType(sourceName, arrayOf("front", "back"), "", "")
            openAddDialog()
            compose.chooseNoteType(sourceName)
            compose.fillName(cloneName)
            compose.confirmAdd()
            compose.onNodeWithText("problem", substring = true).assertIsDisplayed()
            assertNull(col.notetypes.byName(cloneName), "The invalid clone must not be created")
            compose.onNodeWithText("Close", ignoreCase = true).performClick()
            verify(crashReporter, never()).sendExceptionReport(any<Throwable>(), anyOrNull(), anyOrNull(), any())
        }

    private fun openAddDialog(): ManageNotetypes {
        ensureCollectionLoadIsSynchronous()
        val activity = startRegularActivity<ManageNotetypes>()
        compose.openAddType()
        return activity
    }
}
