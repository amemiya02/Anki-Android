// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.worker

import android.app.PendingIntent
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.testing.TestListenableWorkerBuilder
import anki.backend.backendError
import anki.sync.mediaSyncProgress
import anki.sync.mediaSyncStatusResponse
import anki.sync.syncAuth
import com.google.common.util.concurrent.Futures
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.NOTIFICATION_MIN_DELAY
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.sync.SyncAuth
import com.ichi2.utils.TruncatedString
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import net.ankiweb.rsdroid.exceptions.BackendNetworkException
import net.ankiweb.rsdroid.exceptions.BackendSyncException
import net.ankiweb.rsdroid.exceptions.BackendSyncException.BackendSyncAuthFailedException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class SyncMediaWorkerTest : RobolectricTest() {
    private val auth = SyncAuth(syncAuth { hkey = "test" })
    private val worker by lazy {
        TestListenableWorkerBuilder<SyncMediaWorker>(targetContext, auth.toWorkData()).build()
    }

    @Test
    fun `network loss during transfer asks WorkManager to retry`() =
        runTest {
            val backend = CollectionManager.getColUnsafe().backend
            mockkObject(backend) {
                every { backend.syncMedia(auth.toProto()) } returns Unit
                every { backend.mediaSyncStatus() } throws BackendNetworkException(backendError { message = "connection lost" })

                assertThat(worker.doWork(), equalTo(Result.retry()))
            }
        }

    @Test
    fun `connection lost while reading media response asks WorkManager to retry`() =
        runTest {
            val backend = CollectionManager.getColUnsafe().backend
            mockkObject(backend) {
                every { backend.syncMedia(auth.toProto()) } returns Unit
                every { backend.mediaSyncStatus() } throws
                    BackendSyncException(backendError { message = """HttpError { code: 303, context: "reading stream" }""" })

                assertThat(worker.doWork(), equalTo(Result.retry()))
            }
        }

    @Test
    fun `an unrelated sync error fails the media work immediately`() =
        runTest {
            val message = """HttpError { code: 303, context: "starting media sync" }"""
            val backend = CollectionManager.getColUnsafe().backend
            mockkObject(backend) {
                every { backend.syncMedia(auth.toProto()) } returns Unit
                every { backend.mediaSyncStatus() } throws BackendSyncException(backendError { this.message = message })

                val result = worker.doWork()
                assertThat(result is Result.Failure, equalTo(true))
                assertThat(result.outputData.mediaSyncErrorMessage, equalTo(message))
            }
        }

    @Test
    fun `an invalid saved certificate stops media sync before contacting the server`() =
        runTest {
            val endpoint = "https://anki.example.test/"
            val invalidPem = "invalid PEM"
            Prefs.isCustomSyncEnabled = true
            Prefs.customSyncUri = endpoint
            Prefs.sharedPrefs.edit { putString(Prefs.key(R.string.custom_sync_certificate_key), invalidPem) }
            val customAuth =
                SyncAuth(
                    syncAuth {
                        hkey = "test"
                        this.endpoint = endpoint
                    },
                )
            val worker = TestListenableWorkerBuilder<SyncMediaWorker>(targetContext, customAuth.toWorkData()).build()
            val backend = CollectionManager.getColUnsafe().backend
            mockkObject(backend) {
                every { backend.setCustomCertificate(invalidPem) } returns false
                every { backend.syncMedia(customAuth.toProto()) } returns Unit
                every { backend.mediaSyncStatus() } returns mediaSyncStatusResponse { active = false }

                assertThat(worker.doWork(), equalTo(Result.failure()))
                verify(exactly = 0) { backend.syncMedia(customAuth.toProto()) }
            }
        }

    @Test
    fun `rejected media sync credentials fail the work`() =
        runTest {
            val backend = CollectionManager.getColUnsafe().backend
            mockkObject(backend) {
                every { backend.syncMedia(auth.toProto()) } throws
                    BackendSyncAuthFailedException(backendError { message = "invalid credentials" })

                val result = worker.doWork()
                assertThat(result is Result.Failure, equalTo(true))
                assertThat(result.outputData.mediaSyncErrorMessage, equalTo("invalid credentials"))
            }
        }

    @Test
    fun `progress updates are published only when the transfer changes`() =
        runTest {
            val start =
                mediaSyncProgress {
                    added = "Added 1"
                    removed = "Removed 0"
                    checked = "Checked 2"
                }
            val next =
                mediaSyncProgress {
                    added = "Added 2"
                    removed = "Removed 0"
                    checked = "Checked 2"
                }
            val updates = mutableListOf<Data>()
            val worker =
                TestListenableWorkerBuilder<SyncMediaWorker>(targetContext, auth.toWorkData())
                    .setProgressUpdater { _, _, progress ->
                        updates.add(progress)
                        Futures.immediateVoidFuture()
                    }.build()
            val backend = CollectionManager.getColUnsafe().backend
            mockkObject(backend) {
                every { backend.syncMedia(auth.toProto()) } returns Unit
                every { backend.mediaSyncStatus() } returnsMany
                    listOf(
                        mediaSyncStatusResponse {
                            active = true
                            progress = start
                        },
                        mediaSyncStatusResponse {
                            active = true
                            progress = start
                        },
                        mediaSyncStatusResponse {
                            active = true
                            progress = next
                        },
                        mediaSyncStatusResponse { active = false },
                    )

                assertThat(worker.doWork(), equalTo(Result.success()))
                assertThat(
                    updates.map { it.mediaSyncProgressText },
                    equalTo(listOf("Added 1\nRemoved 0\nChecked 2", "Added 2\nRemoved 0\nChecked 2")),
                )
            }
        }

    @Test
    @Suppress("SimplifyBooleanWithConstants")
    fun `notification update delay is not lower than min delay`() {
        assert(SyncMediaWorker.NOTIFICATION_UPDATE_RATE >= NOTIFICATION_MIN_DELAY)
    }

    // Issue 18937: an unset protobuf string must not become an explicitly empty URL in work data.
    @Test
    fun `default AnkiWeb endpoint remains absent in work data`() {
        val input = SyncMediaWorker.getWorkRequest(auth, NetworkType.CONNECTED).workSpec.input
        val restoredAuth = input.toSyncAuth()!!

        assertThat(restoredAuth.endpoint, nullValue())
        assertThat(restoredAuth.hkey, equalTo("test"))
    }

    // https://github.com/ankidroid/Anki-Android/issues/20826
    @Test
    fun `copy to clipboard intent is immutable`() {
        val pendingIntent = worker.getCopyToClipboardIntent(TruncatedString.from("error text"))

        assertThat(
            "an intent attached to a notification must not be modifiable by other apps",
            shadowOf(pendingIntent).flags and PendingIntent.FLAG_IMMUTABLE,
            not(equalTo(0)),
        )
    }
}
