// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.permissions

import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi

/** Requests all-files access so the collection can live in a public directory. */
@RequiresApi(Build.VERSION_CODES.R)
class PermissionsStartingAt30Fragment : PermissionsFragment() {
    private val accessAllFilesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshPermissions()
            completeStoragePermissionRequest()
        }

    internal override fun permissionRequests() = listOf(externalStorageRequest(accessAllFilesLauncher))
}
