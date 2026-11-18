// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.introduction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.IntroductionActivity
import com.ichi2.anki.R
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.utils.bottomCornerClearance
import com.ichi2.compose.theme.AnkiDroidTheme

/** First-run destination shared by the application host and the existing welcome entry point. */
class WelcomeFragment :
    Fragment(),
    CollectionPermissionScreenLauncher {
    interface Host {
        fun onWelcomeCompleted(nextAction: String?)
    }

    override val permissionScreenLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if ((requireActivity() as AnkiActivity).hasCollectionStoragePermissions()) complete(DeckPicker.EXTRA_IMPORT)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(inflater.context).apply {
            id = R.id.welcome_content
            var cornerClearance by mutableIntStateOf(0)
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                cornerClearance = insets.bottomCornerClearance(view)
                insets
            }
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnkiDroidTheme {
                    Welcome(
                        bottomClearance = cornerClearance,
                        onStartEmpty = { complete(null) },
                        onImport = {
                            if (!(requireActivity() as AnkiActivity).collectionPermissionScreenWasOpened()) {
                                complete(
                                    DeckPicker.EXTRA_IMPORT,
                                )
                            }
                        },
                        onSetupSync = { complete(DeckPicker.EXTRA_SYNC_SETUP) },
                    )
                }
            }
        }

    private fun complete(nextAction: String?) {
        val activity = requireActivity()
        if (activity.isFinishing) return
        activity.sharedPrefs().edit { putBoolean(IntroductionActivity.PREF_INTRODUCTION_SLIDES_SHOWN, true) }
        (activity as Host).onWelcomeCompleted(nextAction)
    }
}
