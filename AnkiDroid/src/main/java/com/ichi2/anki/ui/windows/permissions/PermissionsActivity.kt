// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.permissions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.CommonString
import com.ichi2.anki.InitialActivity
import com.ichi2.anki.R
import com.ichi2.anki.StoragePermissionSet
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.common.utils.ext.getParcelableExtraCompat
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.utils.ext.getParcelableCompat
import timber.log.Timber

/**
 * Screen responsible for getting permissions from the user.
 *
 * Prefer using [PermissionsActivity.getIntent] to get an intent to this activity.
 *
 * Advantages:
 * * Explains why each permission should be granted
 * * Easily reusable
 * * Doesn't need to block any UI elements or background routines that depends on a permission.
 *     Nor needs to add callbacks after the permissions are granted
 *
 * To request optional permissions from the user, prefer [PermissionsBottomSheet].
 */
class PermissionsActivity : AnkiActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) {
            return
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(FragmentContainerView(this).apply { id = R.id.fragment_container })

        // #20881: Activity should not be launchd without extras
        val permissionSet = intent.getParcelableExtraCompat<StoragePermissionSet>(EXTRA_PERMISSIONS_SET)
        if (permissionSet == null) {
            Timber.w("EXTRA_PERMISSIONS_SET not set; finishing")
            showThemedToast(this, CommonString.something_wrong, false)
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, CollectionPermissionsFragment::class.java, intent.extras)
            }
        }
    }

    companion object {
        const val EXTRA_PERMISSIONS_SET = "permissionsSet"

        /**
         * Fragment result request key: all the fragment's permissions are granted.
         *
         * Signals the activity to persist the
         * [storage decision][com.ichi2.anki.common.storage.StorageDecision], and then close.
         */
        const val REQUEST_PERMISSIONS_GRANTED = "result_complete"

        fun getIntent(
            context: Context,
            permissionsSet: StoragePermissionSet,
        ): Intent =
            Intent(context, PermissionsActivity::class.java).apply {
                putExtra(EXTRA_PERMISSIONS_SET, permissionsSet as Parcelable)
            }
    }
}

/** Runs before the collection is open, including during first-run import and after a storage change. */
class CollectionPermissionsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentContainerView(inflater.context).apply { id = R.id.storage_permissions_container }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val permissionSet = arguments?.getParcelableCompat<StoragePermissionSet>(PermissionsActivity.EXTRA_PERMISSIONS_SET)
        if (permissionSet == null) {
            showThemedToast(requireContext(), CommonString.something_wrong, false)
            finishScreen()
            return
        }
        val screen = permissionSet.permissionsFragment
        if (screen == null) {
            decideStorageAndFinish()
            return
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { }
        childFragmentManager.setFragmentResultListener(PermissionsActivity.REQUEST_PERMISSIONS_GRANTED, viewLifecycleOwner) { _, _ ->
            decideStorageAndFinish()
        }
        if (childFragmentManager.findFragmentById(R.id.storage_permissions_container) == null) {
            childFragmentManager.commit { replace(R.id.storage_permissions_container, screen, null) }
        }
    }

    private fun decideStorageAndFinish() {
        InitialActivity.decideStorageIfUndecided(requireContext())
        finishScreen()
    }
}
