// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Eric Li <ericli3690@gmail.com>

package com.ichi2.anki.ui.windows.permissions

import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.permissions.LEGACY_POST_NOTIFICATIONS
import com.ichi2.anki.settings.Prefs
import com.ichi2.utils.Permissions
import com.ichi2.utils.Permissions.attemptToEnableNotifications
import com.ichi2.utils.Permissions.notificationsPermission
import com.ichi2.utils.Permissions.requestPermissionThroughDialogOrSettings
import timber.log.Timber
import com.ichi2.anki.common.android.R as CommonR

/** Explains the permissions shown on the Android privacy dashboard and app settings. */
@RequiresApi(Build.VERSION_CODES.S)
class AllPermissionsExplanationFragment : PermissionsFragment() {
    internal override val isExplanation = true

    private val permissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Timber.i("Permission result: $isGranted")
            refreshPermissions()
        }

    private val accessAllFilesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refreshPermissions() }

    internal override fun permissionRequests(): List<PermissionRequest> =
        buildList {
            if (Permissions.canManageExternalStorage(requireContext())) {
                add(externalStorageRequest(accessAllFilesLauncher))
            }
            val notifications =
                if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.TIRAMISU
                ) {
                    notificationsPermission
                } else {
                    LEGACY_POST_NOTIFICATIONS
                }
            add(
                PermissionRequest(
                    CommonString.notification_pref,
                    CommonString.notifications_permission_explanation,
                    R.drawable.symbol_notifications,
                    listOf(notifications),
                    optional = true,
                ) { granted ->
                    requestOrOpenSettings(notifications, granted) { attemptToEnableNotifications(permissionRequestLauncher) }
                },
            )
            add(
                PermissionRequest(
                    CommonString.microphone,
                    CommonString.microphone_permission_explanation,
                    CommonR.drawable.symbol_mic,
                    listOf(Permissions.RECORD_AUDIO_PERMISSION),
                    optional = true,
                ) { granted ->
                    requestOrOpenSettings(Permissions.RECORD_AUDIO_PERMISSION, granted) {
                        requestPermissionThroughDialogOrSettings(
                            activity = requireActivity(),
                            permission = Permissions.RECORD_AUDIO_PERMISSION,
                            permissionRequestedFlag = Prefs::recordAudioPermissionRequested,
                            permissionRequestLauncher = permissionRequestLauncher,
                        )
                    }
                },
            )
        }
}
