// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import anki.backend.backendError
import anki.sync.syncAuth
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.sync.SyncAuth
import com.ichi2.anki.worker.SyncMediaWorker
import com.ichi2.anki.worker.UniqueWorkNames
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import net.ankiweb.rsdroid.exceptions.BackendNetworkException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowDialog
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class MediaSyncLogTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    override fun setupTestDispatcher(dispatcher: TestDispatcher) {
        super.setupTestDispatcher(dispatcher)
        // WorkManager observes its database asynchronously and resumes the dialog on the main looper.
        Dispatchers.resetMain()
    }

    @Test
    fun `open media log lets the worker retry and can cancel it during backoff`() =
        deckPicker {
            val auth = SyncAuth(syncAuth { hkey = "test" })
            val manager = WorkManager.getInstance(this)
            SyncMediaWorker.start(this, auth, NetworkType.UNMETERED)
            val request = manager.getWorkInfosForUniqueWork(UniqueWorkNames.SYNC_MEDIA).get().single()
            val backend = CollectionManager.getColUnsafe().backend

            mockkObject(backend) {
                every { backend.syncMedia(auth.toProto()) } returns Unit
                every { backend.mediaSyncStatus() } throws BackendNetworkException(backendError { message = "connection lost" })

                val previousDialog = ShadowDialog.getLatestDialog()
                val log = launchCatchingTask { monitorMediaSync(this@deckPicker) }
                try {
                    advanceRobolectricLooperUntil { ShadowDialog.getLatestDialog() !== previousDialog }
                    val dialog = assertNotNull(ShadowDialog.getLatestDialog())
                    compose
                        .onNode(hasText(getString(CommonString.sync_media_queued)) and hasAnyAncestor(isDialog()))
                        .assertIsDisplayed()
                    verify(exactly = 0) { backend.mediaSyncStatus() }

                    requireNotNull(WorkManagerTestInitHelper.getTestDriver(this)).setAllConstraintsMet(request.id)
                    advanceRobolectricLooperUntil {
                        val work = manager.getWorkInfoById(request.id).get()
                        work?.state == WorkInfo.State.ENQUEUED && work.runAttemptCount == 1
                    }
                    assertTrue(dialog.isShowing)
                    compose.onNodeWithText(TR.syncAbortButton()).performClick()
                    advanceRobolectricLooperUntil {
                        !dialog.isShowing && manager.getWorkInfoById(request.id).get()?.state == WorkInfo.State.CANCELLED
                    }
                    assertTrue(Prefs.mediaSyncPaused)
                } finally {
                    log.cancel()
                    advanceRobolectricLooper()
                }
            }
        }
}
