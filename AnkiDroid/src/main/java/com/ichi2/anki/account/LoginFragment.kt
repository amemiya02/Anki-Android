// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ichi2.anki.CommonString
import com.ichi2.anki.CrashReportData.Companion.toCrashReportData
import com.ichi2.anki.NoSyncServerException
import com.ichi2.anki.R
import com.ichi2.anki.exception.CollectionLockedException
import com.ichi2.anki.preferences.SettingsChevron
import com.ichi2.anki.preferences.SettingsRow
import com.ichi2.anki.preferences.settingsComposeView
import com.ichi2.anki.progress.observeProgress
import com.ichi2.anki.showErrorAndAwaitDismissal
import com.ichi2.anki.syncServerAddress
import com.ichi2.anki.syncServerDisplayName
import com.ichi2.anki.utils.observeMessages
import com.ichi2.anki.utils.showSnackbarAndAwaitDismissal
import com.ichi2.compose.theme.foundation
import com.ichi2.utils.LanguageUtil.withUnicodeIsolation
import net.ankiweb.rsdroid.exceptions.BackendNetworkException
import net.ankiweb.rsdroid.exceptions.BackendSyncException.BackendSyncAuthFailedException

class LoginFragment : Fragment() {
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = settingsComposeView(R.id.settings_sync_sign_in) { SignIn() }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        observeProgress(viewModel) { getString(CommonString.sign_in) }
        observeMessages(viewModel.pendingMessages, viewModel::messageShown) { message ->
            when (message) {
                LoginMessage.LoggedIn -> (requireParentFragment() as AccountFlowFragment).completeSignIn()
                is LoginMessage.Failed -> showLoginError(message.exception)
            }
        }
    }

    @Composable
    private fun SignIn() {
        val flow = requireParentFragment() as AccountFlowFragment
        val server = syncServerAddress()
        var username by rememberSaveable(server) { mutableStateOf("") }
        // Keep credentials out of the activity's saved state.
        var password by remember(server) { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var usernameWasFocused by remember { mutableStateOf(false) }
        var passwordWasFocused by remember { mutableStateOf(false) }
        val canSignIn by viewModel.loginButtonEnabled.collectAsStateWithLifecycle()
        val usernameError by viewModel.userNameError.collectAsStateWithLifecycle()
        val passwordError by viewModel.passwordError.collectAsStateWithLifecycle()
        val foundation = MaterialTheme.foundation
        val keyboard = LocalSoftwareKeyboardController.current

        LaunchedEffect(username, password) { viewModel.onTextChanged(username.trim(), password) }

        fun signIn() {
            if (server == null || !canSignIn) return
            keyboard?.hide()
            viewModel.login(username.trim(), password)
        }

        SyncStepPage(
            title = stringResource(CommonString.paper_sync_sign_in_title),
            step = 2,
            button = stringResource(CommonString.paper_sync_sign_in),
            canContinue = server != null && canSignIn,
            bottomCornerClearancePx = flow.bottomCornerClearancePx,
            onBack = requireActivity().onBackPressedDispatcher::onBackPressed,
            onContinue = ::signIn,
        ) {
            SettingsRow(
                title = stringResource(CommonString.paper_sync_change_server),
                summary = server?.let(::withUnicodeIsolation),
                icon = R.drawable.symbol_dns,
                onClick = flow::changeServer,
                trailing = { SettingsChevron() },
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Username }
                        .onFocusChanged {
                            if (usernameWasFocused || it.isFocused) {
                                viewModel.onUserNameFocusChange(it.isFocused, username.trim())
                            }
                            usernameWasFocused = usernameWasFocused || it.isFocused
                        },
                label = { Text(stringResource(CommonString.paper_sync_username)) },
                textStyle = foundation.type.body,
                isError = usernameError != null,
                supportingText = usernameError?.let { error -> { Text(error.toHumanReadableString(activity)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                shape = foundation.shapes.button,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Password }
                        .onFocusChanged {
                            if (passwordWasFocused || it.isFocused) viewModel.onPasswordFocusChange(it.isFocused, password)
                            passwordWasFocused = passwordWasFocused || it.isFocused
                        }.onPreviewKeyEvent {
                            if (it.key != Key.Enter && it.key != Key.NumPadEnter && it.key != Key.DirectionCenter) {
                                return@onPreviewKeyEvent false
                            }
                            if (it.type == KeyEventType.KeyDown) signIn()
                            true
                        },
                label = { Text(stringResource(CommonString.password)) },
                textStyle = foundation.type.body,
                isError = passwordError != null,
                supportingText = {
                    Text(
                        passwordError?.toHumanReadableString(activity) ?: stringResource(CommonString.paper_sync_password_note),
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painterResource(if (passwordVisible) R.drawable.symbol_visibility_off else R.drawable.symbol_visibility),
                            contentDescription =
                                stringResource(
                                    if (passwordVisible) CommonString.paper_sync_hide_password else CommonString.paper_sync_show_password,
                                ),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { signIn() }),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = foundation.shapes.button,
            )
        }
    }

    private suspend fun showLoginError(exception: Exception) {
        when (exception) {
            is CollectionLockedException ->
                requireContext().showErrorAndAwaitDismissal(
                    exception.localizedMessage!!,
                    exception.toCrashReportData(requireContext(), reportException = false),
                )
            is NoSyncServerException -> showSnackbarAndAwaitDismissal(getString(CommonString.sync_server_required))
            is BackendNetworkException ->
                showSnackbarAndAwaitDismissal(
                    getString(
                        CommonString.paper_sync_unreachable_body,
                        syncServerDisplayName() ?: getString(CommonString.sync_server_title),
                    ),
                )
            is BackendSyncAuthFailedException -> showSnackbarAndAwaitDismissal(getString(CommonString.paper_sync_wrong_credentials))
            else -> showSnackbarAndAwaitDismissal(getString(CommonString.paper_sync_sign_in_failed))
        }
    }
}
