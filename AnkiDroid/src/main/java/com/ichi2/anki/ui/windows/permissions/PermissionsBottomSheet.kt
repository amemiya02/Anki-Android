// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Eric Li <ericli3690@gmail.com>

package com.ichi2.anki.ui.windows.permissions

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.ichi2.anki.CommonString
import com.ichi2.anki.OptionalPermissionSet
import com.ichi2.anki.R
import com.ichi2.anki.common.permissions.LEGACY_POST_NOTIFICATIONS
import com.ichi2.anki.common.permissions.canPostNotifications
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.utils.ext.requireParcelable
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.utils.Permissions.notificationsPermission
import com.ichi2.utils.Permissions.requestPermissionThroughDialogOrSettings
import com.ichi2.utils.Permissions.showToastAndOpenAppSettingsScreenForPermission

/**
 * Requests optional notification access when a feature needs it. Required permissions are handled
 * separately by [PermissionsActivity].
 */
class PermissionsBottomSheet : DialogFragment() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) dismiss()
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val permissionSet = requireArguments().requireParcelable<OptionalPermissionSet>(ARG_PERMISSION_SET)
        val canRequestPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                permissionSet == OptionalPermissionSet.NOTIFICATIONS
        val actionLabel =
            if (canRequestPermission) CommonString.reminder_troubleshooting_action_grant_permission else CommonString.open_settings
        return createPaperDialog(requireContext()) {
            ConfirmationDialog(
                title = getString(CommonString.permissions_screen_optional_headline),
                message = getString(CommonString.notifications_permission_explanation),
                confirm =
                    DialogAction(
                        label =
                            getString(
                                if (canRequestPermission) CommonString.reminder_troubleshooting_action_grant_permission else CommonString.open_settings,
                            ),
                        onClick = {
                            if (canRequestPermission) {
                                requestPermissionThroughDialogOrSettings(
                                    activity = requireActivity(),
                                    permission = notificationsPermission,
                                    permissionRequestedFlag = Prefs::notificationsPermissionRequested,
                                    permissionRequestLauncher = permissionLauncher,
                                )
                            } else {
                                showToastAndOpenAppSettingsScreenForPermission(
                                    LEGACY_POST_NOTIFICATIONS,
                                    CommonString.manually_grant_permissions,
                                )
                            }
                        },
                    ),
                markIcon = R.drawable.symbol_notifications,
                onDismissRequest = ::dismiss,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (canPostNotifications(requireContext())) dismiss()
    }

    companion object {
        private const val FRAGMENT_TAG = "notifications_bottom_sheet"
        private const val ARG_PERMISSION_SET = "arg_permission_set"

        fun launch(
            fragmentManager: FragmentManager,
            permissionsSet: OptionalPermissionSet,
        ) {
            PermissionsBottomSheet()
                .apply {
                    arguments = Bundle().apply { putParcelable(ARG_PERMISSION_SET, permissionsSet) }
                }.show(fragmentManager, FRAGMENT_TAG)
        }
    }
}
