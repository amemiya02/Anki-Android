// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.permissions

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.permissions.MANAGE_EXTERNAL_STORAGE
import com.ichi2.anki.common.permissions.hasAllPermissions
import com.ichi2.anki.preferences.settingsComposeView
import com.ichi2.utils.Permissions.openAppSettingsScreenForPermission
import com.ichi2.utils.Permissions.showToastAndOpenAppSettingsScreenForPermission
import timber.log.Timber

internal data class PermissionRequest(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @DrawableRes val icon: Int,
    val permissions: List<String>,
    val optional: Boolean = false,
    val onClick: (Boolean) -> Unit,
)

internal data class PermissionRowState(
    val request: PermissionRequest,
    val granted: Boolean,
)

/** Keeps the runtime permission results and their Android settings routes in the host. */
abstract class PermissionsFragment : Fragment() {
    internal abstract fun permissionRequests(): List<PermissionRequest>

    private var rows by mutableStateOf<List<PermissionRowState>>(emptyList())

    internal open val isExplanation = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        refreshPermissions()
        return settingsComposeView(R.id.permissions_screen) {
            PermissionsScreen(
                title =
                    stringResource(
                        if (isExplanation) CommonString.permissions_screen_optional_headline else CommonString.permissions_screen_headline,
                    ),
                rows = rows,
                showGroupLabels = isExplanation,
                onBack = if (isExplanation) ({ requireActivity().onBackPressedDispatcher.onBackPressed() }) else null,
                onContinue = if (isExplanation) null else ::completeStoragePermissionRequest,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    protected fun refreshPermissions() {
        rows =
            permissionRequests().map { request ->
                PermissionRowState(request, hasAllPermissions(requireContext(), request.permissions))
            }
    }

    protected fun completeStoragePermissionRequest() {
        if (permissionRequests().all { hasAllPermissions(requireContext(), it.permissions) }) {
            setFragmentResult(PermissionsActivity.REQUEST_PERMISSIONS_GRANTED, Bundle())
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    internal fun externalStorageRequest(launcher: ActivityResultLauncher<Intent>): PermissionRequest =
        PermissionRequest(
            CommonString.all_files_access_title,
            CommonString.storage_access_summary,
            R.drawable.symbol_save,
            listOf(MANAGE_EXTERNAL_STORAGE),
        ) { granted ->
            if (!granted) launcher.showManageAllFilesScreen()
        }

    /** Opens the Android all-files page if the device provides it. */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun ActivityResultLauncher<Intent>.showManageAllFilesScreen() {
        val intent =
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.fromParts("package", requireActivity().packageName, null),
            )
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            Timber.i("launching ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION")
            launch(intent)
        } else {
            openAppSettingsScreenForPermission(MANAGE_EXTERNAL_STORAGE)
        }
    }

    protected fun requestOrOpenSettings(
        permission: String,
        granted: Boolean,
        request: () -> Unit,
    ) {
        if (granted) {
            showToastAndOpenAppSettingsScreenForPermission(permission, CommonString.revoke_permissions)
        } else {
            request()
        }
    }
}
