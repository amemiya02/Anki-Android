// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.account

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.NoSyncServerException
import com.ichi2.anki.cancelSync
import com.ichi2.anki.progress.HasProgress
import com.ichi2.anki.progress.ProgressManager
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.sync.SyncAuth
import com.ichi2.anki.syncCertificate
import com.ichi2.anki.syncServerAddress
import com.ichi2.anki.usesAnkiWeb
import com.ichi2.anki.utils.MessageQueue
import com.ichi2.anki.utils.MessageQueue.MessageId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import net.ankiweb.rsdroid.exceptions.BackendSyncException
import timber.log.Timber

/** The operation [LoginViewModel] is running, shown as a progress message by the UI. */
enum class LoginProgress {
    LOGGING_IN,
}

internal class InvalidSyncCertificateException : Exception("Invalid custom sync certificate")

/** The outcome of a login attempt, which the UI shows and then acknowledges. */
sealed interface LoginMessage {
    data object LoggedIn : LoginMessage

    /**
     * The exception from the login attempt itself i.e. [BackendSyncException.BackendSyncAuthFailedException]
     *
     * This may contain PII as it comes from the backend
     */
    data class Failed(
        val exception: Exception,
    ) : LoginMessage
}

/**
 * ViewModel that manages the state for user login. It handles the login process,
 * validates input fields, and provides results for login operations.
 */
class LoginViewModel :
    ViewModel(),
    HasProgress<LoginProgress> {
    override val progressManager = ProgressManager<LoginProgress>()

    val loginButtonEnabled: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val userNameError: StateFlow<LoginError?>
        field = MutableStateFlow<LoginError?>(null)

    val passwordError: StateFlow<LoginError?>
        field = MutableStateFlow<LoginError?>(null)

    private val messageQueue = MessageQueue<LoginMessage>()

    /** Login outcomes in display order. Collecting does not consume them; acknowledge with [messageShown]. */
    val pendingMessages = messageQueue.messages

    fun messageShown(id: MessageId) = messageQueue.acknowledge(id)

    /** Whether a login is running or has succeeded. Further attempts are ignored. */
    private var loginStarted = false

    fun onUserNameFocusChange(
        hasFocus: Boolean,
        userName: String,
    ) {
        userNameError.value = if (!hasFocus && userName.isEmpty()) LoginError.EMPTY_USERNAME else null
    }

    fun onPasswordFocusChange(
        hasFocus: Boolean,
        password: String,
    ) {
        passwordError.value = if (!hasFocus && password.isEmpty()) LoginError.EMPTY_PASSWORD else null
    }

    fun onTextChanged(
        userName: String,
        password: String,
    ) {
        loginButtonEnabled.value = userName.isNotEmpty() && password.isNotEmpty()
    }

    /**
     * Attempts to authenticate the user with the provided credentials,
     * adding the outcome to [pendingMessages].
     *
     * Ignored if a credential is missing, or a login is running or has succeeded.
     * A malformed custom server configuration requires correction before sign-in.
     *
     * @param username The username entered by the user.
     * @param password The password entered by the user.
     */
    fun login(
        username: String,
        password: String,
    ) {
        if (username.isEmpty() || password.isEmpty()) {
            Timber.i("Login cancelled - username/password missing")
            return
        }
        if (loginStarted) {
            Timber.i("Login cancelled - already logging in")
            return
        }
        val endpoint = syncServerAddress()
        val session = Prefs.syncSession
        if (endpoint == null && !usesAnkiWeb()) {
            Timber.i("Login cancelled - no sync server")
            onLoginFailed(NoSyncServerException())
            return
        }
        // set before launching: the login may complete before `launch` returns
        loginStarted = true
        Timber.i("Logging in")
        // a new attempt makes earlier failures obsolete, so its outcome doesn't wait for them to be dismissed
        pendingMessages.value
            .filter { it.message is LoginMessage.Failed }
            .forEach { messageQueue.acknowledge(it.id) }
        viewModelScope.launch {
            // Not a child of this coroutine, which would then wait for it when cancelled:
            // waiting for collection access can't be interrupted, so the progress would remain
            // until the collection is available
            val auth = viewModelScope.async { syncLogin(username, password, endpoint, session) }
            val login = coroutineContext.job
            try {
                progressManager.withProgress(
                    message = LoginProgress.LOGGING_IN,
                    onCancel = {
                        login.cancel()
                        // neither cancelling nor setWantsAbort interrupts a request waiting on the server
                        cancelSync(CollectionManager.getBackend())
                    },
                ) {
                    val key = auth.await().hkey
                    check(Prefs.syncSession == session) { "Sync account changed during sign-in" }
                    updateSyncAccount(AnkiDroidApp.instance, username, key)
                    Timber.i("Login success")
                }
                messageQueue.enqueue(LoginMessage.LoggedIn)
            } catch (exc: CancellationException) {
                Timber.i("Login cancelled")
                // if it's still waiting for the collection, the credentials are never sent
                auth.cancel()
                loginStarted = false
                throw exc
            } catch (exc: BackendSyncException.BackendSyncAuthFailedException) {
                Timber.i("Login auth failed")
                if (Prefs.syncSession == session) updateSyncAccount(AnkiDroidApp.instance)
                onLoginFailed(exc)
            } catch (exc: Exception) {
                // do not log the error, can contain PII
                Timber.w("Login error")
                onLoginFailed(exc)
            }
        }
    }

    private fun onLoginFailed(exception: Exception) {
        loginStarted = false
        messageQueue.enqueue(LoginMessage.Failed(exception))
    }

    private suspend fun syncLogin(
        username: String,
        password: String,
        endpoint: String?,
        session: String,
    ): SyncAuth =
        withCol {
            check(Prefs.syncSession == session) { "Sync account changed before sign-in" }
            if (!CollectionManager.updateCustomCertificate(syncCertificate())) throw InvalidSyncCertificateException()
            check(Prefs.syncSession == session) { "Sync account changed before sign-in" }
            SyncAuth(syncLogin(username, password, endpoint))
        }
}

/**
 * Enum representing the possible error states for the login fields.
 * It is used to track errors related to the username and password fields.
 */
enum class LoginError(
    @StringRes private val messageResId: Int,
) {
    EMPTY_USERNAME(CommonString.paper_sync_username_required),
    EMPTY_PASSWORD(CommonString.password_empty),
    ;

    fun toHumanReadableString(context: Context): String = context.getString(this.messageResId)
}
