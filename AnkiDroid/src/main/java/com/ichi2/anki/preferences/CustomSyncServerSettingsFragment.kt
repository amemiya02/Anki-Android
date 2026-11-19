// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.R
import com.ichi2.anki.account.AccountFlowFragment
import com.ichi2.anki.account.LoggedInViewModel
import com.ichi2.anki.account.SyncCertificateEditor
import com.ichi2.anki.account.SyncStepPage
import com.ichi2.anki.isSelfHostedSyncAddress
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.utils.openUrl
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.PaperDialog

/** The address and optional certificate are configured before credentials can be entered. */
class CustomSyncServerSettingsFragment : Fragment() {
    private val account by viewModels<LoggedInViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = settingsComposeView(R.id.settings_sync_server) { Address() }

    @Composable
    private fun Address() {
        val flow = requireParentFragment() as AccountFlowFragment
        val server by account.server.collectAsStateWithLifecycle()
        var address by rememberSaveable { mutableStateOf(Prefs.customSyncUri.orEmpty()) }
        var certificate by rememberSaveable { mutableStateOf(Prefs.customSyncCertificate.orEmpty()) }
        var showCertificate by rememberSaveable { mutableStateOf(false) }
        var showImportHelp by rememberSaveable { mutableStateOf(false) }
        var confirmRemove by rememberSaveable { mutableStateOf(false) }
        val foundation = MaterialTheme.foundation
        val keyboard = LocalSoftwareKeyboardController.current
        val valid = isSelfHostedSyncAddress(address.trim())

        fun continueToSignIn() {
            if (!valid) return
            keyboard?.hide()
            account.setServer(requireContext(), address.trim())
            flow.showSignIn()
        }

        SyncStepPage(
            title = stringResource(CommonString.sync_server_title),
            step = 1,
            button = stringResource(CommonString.dialog_continue),
            canContinue = valid,
            bottomCornerClearancePx = flow.bottomCornerClearancePx,
            onBack = requireActivity().onBackPressedDispatcher::onBackPressed,
            onContinue = ::continueToSignIn,
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(CommonString.paper_sync_server_address)) },
                supportingText = {
                    Text(
                        stringResource(
                            if (address.isNotBlank() && !valid) {
                                CommonString.paper_sync_invalid_address
                            } else {
                                CommonString.paper_sync_address_hint
                            },
                        ),
                    )
                },
                isError = address.isNotBlank() && !valid,
                textStyle = foundation.type.body,
                shape = foundation.shapes.button,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { continueToSignIn() }),
                singleLine = true,
            )
            SettingsRow(
                title = stringResource(CommonString.custom_sync_certificate_title),
                summary = stringResource(CommonString.paper_sync_certificate_hint),
                value = if (certificate.isBlank()) stringResource(CommonString.paper_sync_optional) else null,
                icon = R.drawable.symbol_verified_user,
                onClick = { showCertificate = true },
                trailing = { SettingsChevron() },
            )
            SettingsRow(
                title = stringResource(CommonString.paper_sync_server_help),
                icon = R.drawable.symbol_menu_book,
                onClick = { requireContext().openUrl(R.string.link_custom_sync_server_help_learn_more_en) },
                trailing = {
                    Icon(
                        painterResource(R.drawable.symbol_open_in_new),
                        contentDescription = null,
                        tint = foundation.colors.onBackgroundMuted,
                    )
                },
            )
            SettingsRow(
                title = stringResource(CommonString.paper_sync_from_ankiweb),
                icon = R.drawable.symbol_cloud_off,
                onClick = { showImportHelp = true },
                trailing = { SettingsChevron() },
            )
            if (server != null || !Prefs.customSyncUri.isNullOrEmpty()) {
                SettingsRow(
                    title = stringResource(CommonString.paper_sync_remove_server),
                    onClick = { confirmRemove = true },
                )
            }
        }
        if (showCertificate) {
            SyncCertificateEditor(
                initialText = certificate,
                onDismiss = { showCertificate = false },
                onSave = {
                    certificate = it
                    showCertificate = false
                    showSnackbar(CommonString.dialog_updated_custom_certificate)
                },
            )
        }
        if (confirmRemove) {
            ConfirmationDialog(
                title = stringResource(CommonString.paper_sync_remove_server),
                message = stringResource(CommonString.paper_sync_sign_out_hint),
                confirm =
                    DialogAction(
                        stringResource(CommonString.paper_sync_remove_server),
                        {
                            account.setServer(requireContext(), null)
                            address = ""
                            confirmRemove = false
                        },
                        DialogActionStyle.Destructive,
                    ),
                onDismissRequest = { confirmRemove = false },
            )
        }
        if (showImportHelp) {
            PaperDialog(
                title = stringResource(CommonString.paper_sync_from_ankiweb),
                onDismissRequest = { showImportHelp = false },
                actions =
                    listOf(
                        DialogAction(stringResource(CommonString.dialog_cancel), { showImportHelp = false }),
                        DialogAction(
                            stringResource(CommonString.paper_sync_choose_file),
                            {
                                startActivity(
                                    DeckPicker
                                        .getIntent(requireContext())
                                        .putExtra(DeckPicker.EXTRA_IMPORT, true)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                                )
                                flow.finishScreen()
                            },
                            DialogActionStyle.Filled,
                        ),
                    ),
            ) {
                DialogBody(stringResource(CommonString.paper_sync_import_steps))
            }
        }
    }
}
