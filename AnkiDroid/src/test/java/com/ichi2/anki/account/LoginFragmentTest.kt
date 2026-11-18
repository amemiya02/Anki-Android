// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.account

import android.app.Activity.RESULT_OK
import android.os.Looper
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.backend.BackendError
import anki.backend.backendError
import anki.sync.SyncAuth
import anki.sync.SyncLoginRequest
import anki.sync.syncAuth
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.findAnkiActivity
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.throwOnShowError
import com.ichi2.testutils.BackendEmulatingOpenConflict
import com.ichi2.testutils.TEST_SYNC_SERVER
import com.ichi2.testutils.ext.snackbarText
import com.ichi2.testutils.rules.BackendOverrideRule
import com.ichi2.testutils.setSyncServer
import com.ichi2.testutils.withQueuedCollectionAccess
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import net.ankiweb.rsdroid.Backend
import net.ankiweb.rsdroid.exceptions.BackendSyncException.BackendSyncAuthFailedException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@RunWith(AndroidJUnit4::class)
class LoginFragmentTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @get:Rule
    val backend = BackendOverrideRule()

    @Before
    fun setUpSyncServer() {
        setSyncServer(TEST_SYNC_SERVER)
    }

    /**
     * #21051: the locked-collection guidance is multi-sentence, so it is shown in a dialog
     * rather than truncated in the login screen's snackbar
     */
    @Test
    fun `login with a locked collection shows guidance`() =
        runTest {
            lockCollection()

            startLoginScreen()
            logIn()

            compose.onNodeWithText("Advanced settings", substring = true).assertIsDisplayed()
        }

    @Test
    fun `locked collection guidance survives activity recreation`() =
        runTest {
            lockCollection()
            val login = startLoginScreen()
            logIn()
            compose.onNodeWithText("Advanced settings", substring = true).assertIsDisplayed()

            login.recreate()
            advanceRobolectricLooper()

            // ShadowDialog retains the old dialog, so checking only its text can falsely pass.
            val dialog = assertNotNull(ShadowDialog.getLatestDialog())
            assertSame(login.get(), dialog.context.findAnkiActivity(), "the guidance must be displayed by the recreated activity")
            assertTrue(dialog.isShowing)
            compose.onNodeWithText("Advanced settings", substring = true).assertIsDisplayed()
        }

    @Test
    fun `dismissed login error is not shown again after recreation`() =
        runTest {
            lockCollection()
            val login = startLoginScreen()
            logIn()
            compose.onNodeWithText(targetContext.getString(CommonString.dialog_ok)).performClick()
            advanceRobolectricLooper()
            val dismissed = ShadowDialog.getLatestDialog()

            login.recreate()
            advanceRobolectricLooper()

            assertSame(dismissed, ShadowDialog.getLatestDialog())
        }

    @Test
    fun `login error still being displayed survives recreation`() =
        runTest {
            failAuthentication()
            val login = startLoginScreen()
            enterCredentials()
            signIn.performClick()
            // less than the snackbar's duration
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
            assertEquals(authenticationError, login.get().snackbarText)

            login.recreate()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))

            assertEquals(authenticationError, login.get().snackbarText, "recreation must not discard a login error before it can be read")
        }

    @Test
    fun `retrying after a failed login blocks input until it finishes`() =
        runTest {
            failAuthentication()
            val login = startLoginScreen()
            logIn()
            assertFalse(login.get().isBlockingTouches)

            withQueuedCollectionAccess {
                logIn()
                assertTrue(login.get().isBlockingTouches, "input must be blocked while the retry runs")
                advanceUntilIdle()
            }
            advanceRobolectricLooper()

            assertFalse(login.get().isBlockingTouches)
        }

    @Test
    fun `login in progress blocks input after recreation`() =
        runTest {
            succeedAuthentication()
            val login = startLoginScreen()

            withQueuedCollectionAccess {
                logIn()
                login.recreate()
                assertTrue(login.get().isBlockingTouches, "the recreated screen must block input while logging in")
                advanceUntilIdle()
            }
            advanceRobolectricLooper()

            assertEquals(TEST_HKEY, Prefs.hkey)
            assertTrue(login.get().isFinishing, "sign-in returns to the Sync page")
        }

    @Test
    fun `successful login while stopped returns to the Sync page on resume`() =
        runTest {
            succeedAuthentication()
            val login = startLoginScreen()

            withQueuedCollectionAccess {
                logIn()
                login.pause().stop()
                advanceUntilIdle()
            }
            advanceRobolectricLooper()
            assertEquals(TEST_HKEY, Prefs.hkey)

            login.start().resume()
            advanceRobolectricLooper()

            assertTrue(login.get().isFinishing, "sign-in returns to the Sync page")
        }

    @Test
    fun `repeated keyboard submissions sign in once and return to the caller`() =
        runTest {
            var attempts = 0
            backend.replaceWith {
                object : Backend() {
                    override fun syncLogin(input: SyncLoginRequest): SyncAuth {
                        attempts++
                        return syncAuth { hkey = TEST_HKEY }
                    }
                }
            }
            val login = startLoginScreen(forResult = true)
            enterCredentials()

            withQueuedCollectionAccess {
                repeat(2) { passwordField.performImeAction() }
                advanceUntilIdle()
            }
            advanceRobolectricLooper()

            assertEquals(1, attempts)
            assertEquals(TEST_HKEY, Prefs.hkey)
            assertEquals(RESULT_OK, shadowOf(login.get()).resultCode)
        }

    @Test
    fun `cancelling login interrupts a server that has not responded`() {
        val requestReceived = CountDownLatch(1)
        val releaseResponse = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requestReceived.countDown()
            try {
                check(releaseResponse.await(15, TimeUnit.SECONDS))
                exchange.sendResponseHeaders(500, -1)
            } finally {
                exchange.close()
            }
        }
        server.start()
        // a self-hosted sync server: the real Engine signs in to it
        setSyncServer("http://127.0.0.1:${server.address.port}/")
        val login = startLoginScreen()
        val activity = login.get()
        enterCredentials()
        val backend = CollectionManager.getBackend()
        // the login runs on a real thread, so it waits on the server without blocking the test
        val previousQueue = CollectionManager.setTestDispatcher(Dispatchers.IO.limitedParallelism(1), useReentrantLock = false)
        try {
            signIn.performClick()
            assertTrue(requestReceived.await(5, TimeUnit.SECONDS), "the real backend must reach the local server")
            advanceRobolectricLooperUntil { ShadowDialog.getLatestDialog()?.isShowing == true }

            ShadowDialog.getLatestDialog().cancel()
            val cancelledAt = TimeSource.Monotonic.markNow()
            while (activity.isBlockingTouches && cancelledAt.elapsedNow() < 3.seconds) {
                Thread.sleep(10)
                advanceRobolectricLooper()
            }

            assertFalse(activity.isBlockingTouches, "cancellation must unblock input without waiting for the server response")
            assertEquals("", Prefs.hkey.orEmpty())
        } finally {
            backend.abortSync()
            releaseResponse.countDown()
            advanceRobolectricLooperUntil { !activity.isBlockingTouches }
            CollectionManager.setTestDispatcher(previousQueue)
            server.stop(0)
        }
    }

    @Test
    fun `back works after cancelling a login waiting for the collection`() =
        runTest {
            succeedAuthentication()
            val login = startLoginScreen()
            enterCredentials()
            // never advanced: the login keeps waiting for the collection
            val collectionScheduler = TestCoroutineScheduler()
            val previousQueue = CollectionManager.setTestDispatcher(StandardTestDispatcher(collectionScheduler), useReentrantLock = false)
            try {
                signIn.performClick()
                advanceTimeBy(601)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(601))
                val progress = assertNotNull(ShadowDialog.getLatestDialog())
                assertTrue(progress.isShowing)

                progress.cancel()
                shadowOf(Looper.getMainLooper()).idle()
                runCurrent()
                login.get().onBackPressedDispatcher.onBackPressed()

                assertTrue(login.get().isFinishing, "cancelling must unblock Back without waiting for the collection")
            } finally {
                CollectionManager.setTestDispatcher(previousQueue)
                collectionScheduler.runCurrent()
            }
            assertTrue(Prefs.hkey.isNullOrEmpty(), "a cancelled login must not log in")
        }

    @Test
    fun `successful retry does not wait for the previous error to time out`() =
        runTest {
            var attempts = 0
            backend.replaceWith {
                object : Backend() {
                    override fun syncLogin(input: SyncLoginRequest): SyncAuth {
                        if (++attempts == 1) throw authFailure()
                        return syncAuth { hkey = TEST_HKEY }
                    }
                }
            }
            // Android's 'Time to take action' setting can keep a snackbar visible for minutes
            shadowOf(targetContext.getSystemService(AccessibilityManager::class.java)).apply {
                setInteractiveUiTimeout(120_000)
                setNonInteractiveUiTimeout(120_000)
            }
            val login = startLoginScreen(forResult = true)
            enterCredentials()
            signIn.performClick()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
            assertEquals(authenticationError, login.get().snackbarText)

            signIn.performClick()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))

            assertEquals(2, attempts)
            assertEquals(TEST_HKEY, Prefs.hkey)
            assertTrue(login.get().isFinishing, "a successful retry must not wait for the previous error to time out")
            assertEquals(RESULT_OK, shadowOf(login.get()).resultCode)
        }

    @Test
    @Config(qualifiers = "w480dp-h320dp-land")
    fun `logo is hidden in a compact landscape window`() {
        startLoginScreen()

        compose.onNodeWithText("Hayaki").assertDoesNotExist()
        compose.onNodeWithText("Privacy", substring = true).assertDoesNotExist()
    }

    @Test
    fun `address setup sends sign-in success back to the sync caller`() =
        runTest {
            var endpoint: String? = null
            backend.replaceWith {
                object : Backend() {
                    override fun syncLogin(input: SyncLoginRequest): SyncAuth {
                        endpoint = input.endpoint
                        return syncAuth { hkey = TEST_HKEY }
                    }
                }
            }
            setSyncServer(null)
            val login = startLoginScreen(forResult = true)
            compose
                .onNode(hasSetTextAction() and hasText(targetContext.getString(CommonString.paper_sync_server_address)))
                .performTextReplacement(TEST_SYNC_SERVER)
            compose.onNodeWithText(targetContext.getString(CommonString.dialog_continue)).performClick()
            advanceRobolectricLooper()
            logIn()

            assertEquals(TEST_SYNC_SERVER, endpoint)
            assertEquals(RESULT_OK, shadowOf(login.get()).resultCode)
        }

    private suspend fun lockCollection() {
        throwOnShowError = false
        backend.replaceWith { BackendEmulatingOpenConflict() }
    }

    private suspend fun failAuthentication() =
        backend.replaceWith {
            object : Backend() {
                override fun syncLogin(input: SyncLoginRequest): SyncAuth = throw authFailure()
            }
        }

    private fun authFailure() =
        BackendSyncAuthFailedException(
            backendError {
                kind = BackendError.Kind.SYNC_AUTH_ERROR
                message = "invalid credentials"
            },
        )

    private suspend fun succeedAuthentication() =
        backend.replaceWith {
            object : Backend() {
                override fun syncLogin(input: SyncLoginRequest): SyncAuth = syncAuth { hkey = TEST_HKEY }
            }
        }

    private fun startLoginScreen(forResult: Boolean = false): ActivityController<AccountActivity> =
        startActivityControllerNormallyOpenCollectionWithIntent(
            AccountActivity::class.java,
            AccountActivity.getIntent(targetContext, forResult),
        )

    private val passwordField get() =
        compose.onNode(hasSetTextAction() and hasText(targetContext.getString(CommonString.password)))

    private val signIn get() = compose.onNodeWithText(targetContext.getString(CommonString.paper_sync_sign_in))

    private fun enterCredentials() {
        compose
            .onNode(hasSetTextAction() and hasText(targetContext.getString(CommonString.paper_sync_username)))
            .performTextReplacement("paper")
        passwordField.performTextReplacement("sample-password")
    }

    private fun logIn() {
        enterCredentials()
        signIn.performClick()
        advanceRobolectricLooper()
    }

    private val AccountActivity.isBlockingTouches: Boolean
        get() = window.attributes.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0

    private val authenticationError: String
        get() = targetContext.getString(CommonString.paper_sync_wrong_credentials)

    companion object {
        private const val TEST_HKEY = "test-hkey"
    }
}
