// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.instanteditor

import android.content.Intent
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.instantnoteeditor.InstantNoteEditorActivity
import com.ichi2.anki.instantnoteeditor.InstantNoteEditorFragment
import com.ichi2.anki.setIntroductionSlidesShown
import com.ichi2.testutils.BackupManagerTestUtilities
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

context(test: RobolectricTest)
internal fun openSharedInstantNote(
    intent: Intent,
    compose: ComposeTestRule,
): DeckPicker {
    test.setIntroductionSlidesShown(true)
    test.ensureCollectionLoadIsSynchronous()
    BackupManagerTestUtilities.setupSpaceForBackup(test.targetContext)
    val entry = Robolectric.buildActivity(InstantNoteEditorActivity::class.java, intent).use { controller ->
        checkNotNull(shadowOf(controller.create().get()).nextStartedActivity)
    }
    return test.startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, entry).also { deckPicker ->
        compose.waitUntil(10_000) { deckPicker.currentScreenFragment is InstantNoteEditorFragment }
    }
}
