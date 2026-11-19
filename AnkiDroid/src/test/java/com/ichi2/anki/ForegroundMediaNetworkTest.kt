// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.app.Service
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import anki.sync.FullUploadOrDownloadRequest
import anki.sync.SyncCollectionResponse
import anki.sync.SyncStatusResponse
import anki.sync.syncCollectionResponse
import anki.sync.syncStatusResponse
import com.ichi2.anki.dialogs.SyncErrorDialog
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.settings.enums.ShouldFetchMedia
import com.ichi2.anki.utils.ext.DIALOG_FRAGMENT_TAG
import com.ichi2.anki.worker.SyncMediaWorker
import com.ichi2.anki.worker.UniqueWorkNames
import com.ichi2.anki.worker.toWorkData
import com.ichi2.testutils.BackupManagerTestUtilities
import com.ichi2.testutils.signInToSyncServer
import com.ichi2.utils.NetworkUtils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import net.ankiweb.rsdroid.Backend
import net.ankiweb.rsdroid.BackendFactory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import java.util.UUID
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class ForegroundMediaNetworkTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private var metered = false
    private var syncRequired = SyncCollectionResponse.ChangesRequired.NO_CHANGES
    private var fullSyncRequest: FullUploadOrDownloadRequest? = null

    override fun getCollectionStorageMode() = CollectionStorageMode.ON_DISK

    override fun setupTestDispatcher(dispatcher: TestDispatcher) {
        super.setupTestDispatcher(dispatcher)
        // Keep UI continuations on Robolectric's main looper after real background work.
        Dispatchers.resetMain()
    }

    @Before
    fun prepareSync() {
        BackupManagerTestUtilities.setupSpaceForBackup(targetContext)
        signInToSyncServer()
        Prefs.lastSyncTime = 0
        Prefs.allowSyncOnMeteredConnections = false
        Prefs.shouldFetchMedia = ShouldFetchMedia.ALWAYS
        mockkObject(NetworkUtils)
        every { NetworkUtils.isActiveNetworkMetered() } answers { metered }
        runBlocking { CollectionManager.discardBackend() }
        BackendFactory.setOverride {
            object : Backend() {
                override fun syncCollection(
                    auth: anki.sync.SyncAuth,
                    syncMedia: Boolean,
                ): SyncCollectionResponse {
                    // The network changes between the collection sync and the media handoff.
                    metered = true
                    return syncCollectionResponse { required = syncRequired }
                }

                override fun fullUploadOrDownload(input: FullUploadOrDownloadRequest) {
                    fullSyncRequest = input
                }
            }
        }
    }

    @After
    fun resetSync() {
        BackupManagerTestUtilities.reset()
        BackendFactory.setOverride(null)
        unmockkObject(NetworkUtils)
    }

    @Test
    fun `reopening respects notification cancellation until a manual sync`() =
        checkStartupAfterMediaCancellation(SyncStatusResponse.Required.NO_CHANGES)

    @Test
    fun `reopening after cancelling media still syncs collection changes`() =
        checkStartupAfterMediaCancellation(SyncStatusResponse.Required.NORMAL_SYNC)

    private fun checkStartupAfterMediaCancellation(required: SyncStatusResponse.Required) {
        val auth = requireNotNull(syncAuth())
        val manager = WorkManager.getInstance(targetContext)
        runBlocking { SyncMediaWorker.start(targetContext, auth, NetworkType.UNMETERED) }
        val cancelled = manager.getWorkInfosForUniqueWork(UniqueWorkNames.SYNC_MEDIA).get().single()
        runBlocking { tapMediaNotificationCancel(cancelled.id) }
        advanceRobolectricLooperUntil { manager.getWorkInfoById(cancelled.id).get()?.state == WorkInfo.State.CANCELLED }

        every { NetworkUtils.isOnline } returns true
        Prefs.isAutoSyncEnabled = true
        val backend = CollectionManager.getColUnsafe().backend
        mockkObject(backend)
        every { backend.syncStatus(any<anki.sync.SyncAuth>()) } returns syncStatusResponse { this.required = required }
        // Startup checks the collection on Dispatchers.IO. Wait for the activity's sync decision.
        val startupTasks = mutableListOf<Job>()
        mockkStatic("com.ichi2.anki.CoroutineHelpersKt")
        every { any<DeckPicker>().launchCatchingTask(any(), any(), any()) } answers { callOriginal().also(startupTasks::add) }
        try {
            deckPicker {
                advanceRobolectricLooperUntil { startupTasks.isNotEmpty() && startupTasks.all { it.isCompleted } }
                verify(atLeast = 1) { backend.syncStatus(any<anki.sync.SyncAuth>()) }
                verify(exactly = if (required == SyncStatusResponse.Required.NORMAL_SYNC) 1 else 0) {
                    backend.syncCollection(any<anki.sync.SyncAuth>(), any())
                }
                val afterStartup = manager.getWorkInfosForUniqueWork(UniqueWorkNames.SYNC_MEDIA).get()
                assertThat(afterStartup.filterNot { it.state.isFinished }, equalTo(emptyList()))

                metered = false
                Prefs.lastSyncTime = 0
                val manual = syncAndGetMediaWork()
                assertThat(manual.state, equalTo(WorkInfo.State.ENQUEUED))
            }
        } finally {
            unmockkStatic("com.ichi2.anki.CoroutineHelpersKt")
            unmockkObject(backend)
        }
    }

    private suspend fun tapMediaNotificationCancel(id: UUID) {
        val worker = TestListenableWorkerBuilder<SyncMediaWorker>(targetContext, requireNotNull(syncAuth()).toWorkData()).setId(id).build()
        val cancel =
            worker
                .getForegroundInfo()
                .notification.actions
                .single()
                .actionIntent
        cancel.send()
        // Robolectric records service starts. Deliver the notification's command to its actual service.
        val intent = shadowOf(cancel).savedIntent
        val component = Class.forName(requireNotNull(intent.component).className)
        if (Service::class.java.isAssignableFrom(component)) {
            Robolectric
                .buildService(component.asSubclass(Service::class.java), intent)
                .create()
                .startCommand(0, 1)
                .destroy()
        }
    }

    @Test
    fun `starting on wifi does not authorize queued media on a metered network`() =
        deckPicker {
            val request = syncAndGetMediaWork()

            assertThat(metered, equalTo(true))
            assertThat(request.constraints.requiredNetworkType, equalTo(NetworkType.UNMETERED))
        }

    @Test
    fun `one-time metered confirmation permits the queued media sync`() =
        deckPicker {
            metered = true

            val request = syncAndGetMediaWork(confirmWarning = true)

            assertThat("one-time confirmation must not change the preference", Prefs.allowSyncOnMeteredConnections, equalTo(false))
            assertThat(request.constraints.requiredNetworkType, equalTo(NetworkType.CONNECTED))
        }

    @Test
    fun `one-time metered confirmation releases media already waiting for wifi`() =
        deckPicker {
            val waiting = syncAndGetMediaWork()
            assertThat(waiting.state, equalTo(WorkInfo.State.ENQUEUED))
            assertThat(waiting.constraints.requiredNetworkType, equalTo(NetworkType.UNMETERED))
            Prefs.lastSyncTime = 0

            val approved = syncAndGetMediaWork(confirmWarning = true)

            assertThat(Prefs.allowSyncOnMeteredConnections, equalTo(false))
            assertThat(approved.id, equalTo(waiting.id))
            assertThat(approved.constraints.requiredNetworkType, equalTo(NetworkType.CONNECTED))
        }

    @Test
    fun `media-only restriction survives a network change during collection sync`() =
        deckPicker {
            Prefs.allowSyncOnMeteredConnections = true
            Prefs.shouldFetchMedia = ShouldFetchMedia.ONLY_UNMETERED

            val request = syncAndGetMediaWork()

            assertThat(request.constraints.requiredNetworkType, equalTo(NetworkType.UNMETERED))
        }

    @Test
    fun `conflict resolution preserves the original unmetered restriction`() =
        deckPicker {
            syncRequired = SyncCollectionResponse.ChangesRequired.FULL_SYNC

            val request = syncAndGetMediaWork(resolveConflict = ConflictResolution.FULL_UPLOAD)

            assertThat(request.constraints.requiredNetworkType, equalTo(NetworkType.UNMETERED))
            assertFullSyncDoesNotStartMedia(upload = true)
        }

    @Test
    fun `conflict resolution preserves one-time metered confirmation`() =
        deckPicker {
            metered = true
            syncRequired = SyncCollectionResponse.ChangesRequired.FULL_SYNC

            val request = syncAndGetMediaWork(confirmWarning = true, resolveConflict = ConflictResolution.FULL_UPLOAD)

            assertThat(Prefs.allowSyncOnMeteredConnections, equalTo(false))
            assertThat(request.constraints.requiredNetworkType, equalTo(NetworkType.CONNECTED))
            assertFullSyncDoesNotStartMedia(upload = true)
        }

    @Test
    fun `conflict download leaves media startup to the constrained worker`() =
        deckPicker {
            syncRequired = SyncCollectionResponse.ChangesRequired.FULL_SYNC

            val request = syncAndGetMediaWork(resolveConflict = ConflictResolution.FULL_DOWNLOAD)

            assertThat(request.constraints.requiredNetworkType, equalTo(NetworkType.UNMETERED))
            assertFullSyncDoesNotStartMedia(upload = false)
        }

    @Test
    fun `direct full upload leaves media startup to the constrained worker`() =
        deckPicker {
            syncRequired = SyncCollectionResponse.ChangesRequired.FULL_UPLOAD

            val request = syncAndGetMediaWork()

            assertThat(request.constraints.requiredNetworkType, equalTo(NetworkType.UNMETERED))
            assertFullSyncDoesNotStartMedia(upload = true)
        }

    @Test
    fun `direct full download leaves media startup to the constrained worker`() =
        deckPicker {
            syncRequired = SyncCollectionResponse.ChangesRequired.FULL_DOWNLOAD

            val request = syncAndGetMediaWork()

            assertThat(request.constraints.requiredNetworkType, equalTo(NetworkType.UNMETERED))
            assertFullSyncDoesNotStartMedia(upload = false)
        }

    private fun assertFullSyncDoesNotStartMedia(upload: Boolean) {
        val request = requireNotNull(fullSyncRequest)
        assertThat(request.upload, equalTo(upload))
        // Supplying serverUsn makes the Rust backend start media before WorkManager can gate it.
        assertThat("the full sync must not start native media sync", request.hasServerUsn(), equalTo(false))
    }

    private fun DeckPicker.syncAndGetMediaWork(
        confirmWarning: Boolean = false,
        resolveConflict: ConflictResolution? = null,
    ): WorkInfo {
        val manager = WorkManager.getInstance(this)
        sync()
        if (confirmWarning) {
            compose.onNodeWithText(getString(CommonString.dialog_continue)).performClick()
        }
        advanceRobolectricLooperUntil { Prefs.lastSyncTime != 0L }
        if (resolveConflict != null) {
            Prefs.lastSyncTime = 0
            val button =
                when (resolveConflict) {
                    ConflictResolution.FULL_UPLOAD -> CommonString.paper_sync_keep_phone
                    ConflictResolution.FULL_DOWNLOAD -> CommonString.paper_sync_keep_server
                }
            clickConflictAction(button)
            advanceRobolectricLooper()
            clickConflictAction(CommonString.paper_dialog_replace_collection)
            advanceRobolectricLooperUntil { Prefs.lastSyncTime != 0L }
        }
        return manager.getWorkInfosForUniqueWork(UniqueWorkNames.SYNC_MEDIA).get().single { !it.state.isFinished }
    }

    private fun DeckPicker.clickConflictAction(label: Int) {
        // A later deck-refresh progress dialog can replace ShadowDialog's latest dialog.
        val fragment = assertNotNull(supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) as? SyncErrorDialog)
        val dialog = assertNotNull(fragment.dialog)
        assertThat("isShowing", dialog.isShowing, equalTo(true))
        compose.onNode(hasText(getString(label)) and hasClickAction()).performClick()
    }
}
