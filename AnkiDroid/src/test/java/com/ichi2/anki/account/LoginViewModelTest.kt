// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.account

import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.backend.BackendError
import anki.backend.backendError
import anki.sync.SyncAuth
import anki.sync.SyncLoginRequest
import anki.sync.syncAuth
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.progress.ViewModelProgress
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.worker.toSyncAuth
import com.ichi2.anki.worker.toWorkData
import com.ichi2.testutils.TEST_SYNC_SERVER
import com.ichi2.testutils.rules.BackendOverrideRule
import com.ichi2.testutils.setSyncServer
import com.ichi2.testutils.withQueuedCollectionAccess
import kotlinx.coroutines.test.advanceUntilIdle
import net.ankiweb.rsdroid.Backend
import net.ankiweb.rsdroid.exceptions.BackendSyncException.BackendSyncAuthFailedException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.ichi2.anki.syncAuth as appSyncAuth

@RunWith(AndroidJUnit4::class)
class LoginViewModelTest : RobolectricTest() {
    @get:Rule
    val backend = BackendOverrideRule()

    private var loginAttempts = 0

    @Before
    fun setUpSyncServer() {
        setSyncServer(TEST_SYNC_SERVER)
    }

    @Test
    fun `login signs in to the sync server`() =
        runTest {
            var endpoint: String? = null
            emulateLogin {
                endpoint = it.endpoint.takeIf { _ -> it.hasEndpoint() }
                syncAuth { hkey = TEST_HKEY }
            }
            val viewModel = LoginViewModel()

            viewModel.login(USERNAME, PASSWORD)

            assertEquals(TEST_SYNC_SERVER, endpoint)
            assertEquals(listOf<LoginMessage>(LoginMessage.LoggedIn), viewModel.messages)
        }

    @Test
    fun `AnkiWeb login lets the upstream engine choose its endpoint`() =
        runTest {
            setSyncServer(null)
            emulateLogin {
                assertTrue(!it.hasEndpoint())
                syncAuth { hkey = TEST_HKEY }
            }
            val viewModel = LoginViewModel()

            viewModel.login(USERNAME, PASSWORD)

            assertEquals(1, loginAttempts)
            assertEquals(TEST_HKEY, Prefs.hkey)
            assertEquals(listOf<LoginMessage>(LoginMessage.LoggedIn), viewModel.messages)
        }

    @Test
    fun `successful login saves the account`() =
        runTest {
            emulateLogin { syncAuth { hkey = TEST_HKEY } }
            val viewModel = LoginViewModel()

            viewModel.login(USERNAME, PASSWORD)

            assertEquals(USERNAME, Prefs.username)
            assertEquals(TEST_HKEY, Prefs.hkey)
            assertEquals(listOf<LoginMessage>(LoginMessage.LoggedIn), viewModel.messages)
        }

    @Test
    fun `successful login invalidates queued credentials for the previous account`() =
        runTest {
            Prefs.hkey = "previous-hkey"
            val queued = assertNotNull(appSyncAuth()).toWorkData()
            val certificate =
                targetContext.assets
                    .open("USERTrust_RSA.crt")
                    .bufferedReader()
                    .use { it.readText() }
            Prefs.sharedPrefs.edit { putString(Prefs.key(R.string.custom_sync_certificate_key), certificate) }
            emulateLogin { syncAuth { hkey = TEST_HKEY } }

            LoginViewModel().login(USERNAME, PASSWORD)

            assertNull(queued.toSyncAuth())
            assertEquals(TEST_HKEY, appSyncAuth()?.hkey)
            assertEquals(TEST_SYNC_SERVER, appSyncAuth()?.endpoint)
            assertEquals(certificate, Prefs.customSyncCertificate)
        }

    @Test
    fun `login does not send credentials when the backend rejects the configured certificate`() =
        runTest {
            Prefs.sharedPrefs.edit { putString(Prefs.key(R.string.custom_sync_certificate_key), "invalid certificate") }
            emulateLogin { syncAuth { hkey = TEST_HKEY } }
            val viewModel = LoginViewModel()

            viewModel.login(USERNAME, PASSWORD)

            assertEquals(0, loginAttempts)
            assertNull(Prefs.hkey)
            val failure = assertIs<LoginMessage.Failed>(viewModel.messages.single())
            assertIs<InvalidSyncCertificateException>(failure.exception)
        }

    @Test
    fun `failed authentication clears the account`() =
        runTest {
            Prefs.username = "previous@example.com"
            Prefs.hkey = "previous-hkey"
            val queued = assertNotNull(appSyncAuth()).toWorkData()
            Prefs.currentSyncUri = TEST_SYNC_SERVER
            emulateLogin { throw authFailure() }
            val viewModel = LoginViewModel()

            viewModel.login(USERNAME, PASSWORD)

            assertTrue(Prefs.username.isNullOrEmpty())
            assertTrue(Prefs.hkey.isNullOrEmpty())
            assertNull(Prefs.currentSyncUri)
            assertNull(queued.toSyncAuth())
            val failure = assertIs<LoginMessage.Failed>(viewModel.messages.single())
            assertIs<BackendSyncAuthFailedException>(failure.exception)
        }

    @Test
    fun `login can be retried after a failure`() =
        runTest {
            emulateLogin { throw authFailure() }
            val viewModel = LoginViewModel()

            viewModel.login(USERNAME, PASSWORD)
            viewModel.login(USERNAME, PASSWORD)

            assertEquals(2, loginAttempts)
            assertEquals(1, viewModel.messages.size, "a new attempt replaces the earlier failure")
        }

    @Test
    fun `login is ignored while one is in progress`() =
        runTest {
            emulateLogin { syncAuth { hkey = TEST_HKEY } }
            val viewModel = LoginViewModel()

            withQueuedCollectionAccess {
                viewModel.login(USERNAME, PASSWORD)
                val progress = assertIs<ViewModelProgress.Active<*>>(viewModel.progressManager.progress.value)
                assertEquals(LoginProgress.LOGGING_IN, progress.message)
                assertTrue(progress.cancellable)

                viewModel.login(USERNAME, PASSWORD)
                advanceUntilIdle()
            }

            assertEquals(1, loginAttempts)
            assertEquals(ViewModelProgress.Idle, viewModel.progressManager.progress.value)
            assertEquals(listOf<LoginMessage>(LoginMessage.LoggedIn), viewModel.messages)
        }

    @Test
    fun `cancelling a login waiting for the collection does not authenticate`() =
        runTest {
            emulateLogin { syncAuth { hkey = TEST_HKEY } }
            val viewModel = LoginViewModel()

            withQueuedCollectionAccess {
                viewModel.login(USERNAME, PASSWORD)
                viewModel.progressManager.requestCancel()
                assertEquals(ViewModelProgress.Idle, viewModel.progressManager.progress.value, "progress must end without the collection")
                advanceUntilIdle()
            }

            assertEquals(0, loginAttempts, "a cancelled login must not send credentials once the collection is available")
            assertTrue(viewModel.messages.isEmpty())

            viewModel.login(USERNAME, PASSWORD)
            assertEquals(1, loginAttempts, "a cancelled login can be retried")
        }

    @Test
    fun `login is ignored without credentials`() =
        runTest {
            emulateLogin { syncAuth { hkey = TEST_HKEY } }
            val viewModel = LoginViewModel()

            viewModel.login(username = "", password = PASSWORD)
            viewModel.login(username = USERNAME, password = "")

            assertEquals(0, loginAttempts)
            assertTrue(viewModel.messages.isEmpty())
        }

    private suspend fun emulateLogin(login: (SyncLoginRequest) -> SyncAuth) =
        backend.replaceWith {
            object : Backend() {
                override fun syncLogin(input: SyncLoginRequest): SyncAuth {
                    loginAttempts++
                    return login(input)
                }
            }
        }

    private fun authFailure() =
        BackendSyncAuthFailedException(
            backendError {
                kind = BackendError.Kind.SYNC_AUTH_ERROR
                message = "invalid credentials"
            },
        )

    private val LoginViewModel.messages: List<LoginMessage>
        get() = pendingMessages.value.map { it.message }

    companion object {
        private const val USERNAME = "user@example.com"
        private const val PASSWORD = "hunter2"
        private const val TEST_HKEY = "test-hkey"
    }
}
