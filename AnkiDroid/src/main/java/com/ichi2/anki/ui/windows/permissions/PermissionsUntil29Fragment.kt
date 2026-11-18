// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.permissions

import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.utils.Permissions
import com.ichi2.utils.Permissions.showToastAndOpenAppSettingsScreenForPermission

/** Requests read and write access for a collection stored in a public directory. */
class PermissionsUntil29Fragment : PermissionsFragment() {
    private val storageLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { requestedPermissions ->
            refreshPermissions()
            if (!requestedPermissions.all { it.value }) {
                showToastAndOpenAppSettingsScreenForPermission(
                    requestedPermissions.keys.singleOrNull(),
                    CommonString.startup_no_storage_permission,
                )
            }
        }

    internal override fun permissionRequests() =
        listOf(
            PermissionRequest(
                CommonString.storage_access_title,
                CommonString.storage_access_summary,
                R.drawable.symbol_save,
                Permissions.legacyStorageAccessStartupPermissions,
            ) { granted ->
                if (!granted) {
                    // Android 13 and later cannot grant WRITE_EXTERNAL_STORAGE to this app.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        storageLauncher.launch(Permissions.legacyStorageAccessStartupPermissions.toTypedArray())
                    } else {
                        AndroidPermanentlyRevokedPermissionsDialog.show(requireActivity() as AnkiActivity)
                    }
                }
            },
        )
}
