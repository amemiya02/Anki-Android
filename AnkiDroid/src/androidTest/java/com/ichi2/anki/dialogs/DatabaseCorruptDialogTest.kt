// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Alex Odorico <alex.odorico03@gmail.com>

package com.ichi2.anki.dialogs

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.R
import com.ichi2.anki.backend.DatabaseCorruption
import com.ichi2.anki.common.storage.CollectionHelper.getCurrentAnkiDroidDirectory
import com.ichi2.anki.tests.InstrumentedTest
import com.ichi2.anki.tests.Shared
import com.ichi2.anki.testutil.awaitDisplayed
import com.ichi2.anki.testutil.disableIntroductionSlide
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.File

@RunWith(AndroidJUnit4::class)
class DatabaseCorruptDialogTest : InstrumentedTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var targetColFile: File

    @Before
    fun setup() {
        disableIntroductionSlide()
        val dbDir = getCurrentAnkiDroidDirectory(testContext)
        targetColFile = File(dbDir, "collection.anki2")
        val corruptColFile = Shared.getTestFile(testContext, "initial_version_2_12_1_corrupt_regular.anki2")
        corruptColFile.copyTo(targetColFile, overwrite = true)
        Timber.i("Copied %d bytes to target", targetColFile.length())
    }

    @Test
    fun testCorruptDialogFlagSet() {
        ActivityScenario.launch(DeckPicker::class.java).use {
            compose.awaitDisplayed { onNodeWithText(testContext.getString(CommonString.open_collection_failed_title)) }
            Assert.assertTrue(DatabaseCorruption.isDetected)
        }
    }

    @Test
    fun testOpenCollectionFailedDialog() {
        ActivityScenario.launch(DeckPicker::class.java).use {
            compose.awaitDisplayed { onNodeWithText(testContext.getString(CommonString.open_collection_failed_title)) }
        }
    }

    @Test
    fun testCorruptCollectionDialog() {
        val corruptMsg =
            testContext.getString(
                CommonString.corrupt_db_message,
                testContext.getString(R.string.repair_deck),
            )
        ActivityScenario.launch(DeckPicker::class.java).use {
            compose.awaitDisplayed { onNodeWithText(corruptMsg) }
        }
    }

    @After
    fun cleanupCorruptColFile() {
        CollectionManager.closeCollectionBlocking()
        if (!targetColFile.delete()) {
            Timber.e("Cleanup: Failed to delete %s", targetColFile.path)
        }
    }
}
