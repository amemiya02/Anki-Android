// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.analytics.Analytics
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.deckpicker.BackgroundImage
import com.ichi2.anki.deckpicker.BackgroundImage.FileSizeResult
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.settings.enums.AppTheme
import com.ichi2.anki.settings.enums.NightTheme
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.themes.Themes
import timber.log.Timber

class AppearanceSettingsFragment : Fragment() {
    private var state by mutableStateOf(AppearanceSettingsState(AppearanceTheme.SYSTEM, NightTheme.DARK, false))
    private var removeDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        removeDialog = savedInstanceState?.getBoolean(REMOVE_DIALOG) ?: false
        updateAppearance()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(REMOVE_DIALOG, removeDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Analytics.sendAnalyticsScreenView("prefs.appearance")
        return settingsComposeView(R.id.settings_appearance) {
            AppearanceSettingsScreen(
                state,
                settingsSearchTarget,
                settingsBackAction,
                onThemeChange = ::changeTheme,
                onNightThemeChange = ::changeNightTheme,
                onChooseBackground = ::chooseBackground,
                onRemoveBackground = { removeDialog = true },
            )
            if (removeDialog) {
                ConfirmationDialog(
                    title = stringResource(R.string.settings_remove_background),
                    message = stringResource(CommonString.remove_background_image),
                    confirm =
                        DialogAction(
                            stringResource(CommonString.dialog_remove),
                            style = DialogActionStyle.Destructive,
                            onClick = ::removeBackground,
                        ),
                    onDismissRequest = { removeDialog = false },
                    dismissText = stringResource(CommonString.dialog_keep),
                )
            }
        }
    }

    private fun updateAppearance() {
        val theme =
            when (Prefs.appTheme) {
                AppTheme.FOLLOW_SYSTEM -> AppearanceTheme.SYSTEM
                AppTheme.DAY -> AppearanceTheme.LIGHT
                AppTheme.NIGHT -> if (Prefs.nightTheme == NightTheme.BLACK) AppearanceTheme.BLACK else AppearanceTheme.DARK
            }
        state = AppearanceSettingsState(theme, Prefs.nightTheme, BackgroundImage.shouldBeShown(requireContext()))
        (parentFragment as? PreferencesFragment)?.refreshSearchIndex()
    }

    private fun changeTheme(theme: AppearanceTheme) {
        if (state.theme == theme) return
        val mode =
            when (theme) {
                AppearanceTheme.SYSTEM -> AppTheme.FOLLOW_SYSTEM
                AppearanceTheme.LIGHT -> AppTheme.DAY
                AppearanceTheme.DARK, AppearanceTheme.BLACK -> AppTheme.NIGHT
            }
        // The two existing preference values already encode all four choices. Store them together
        // so that restoring an activity cannot observe a partially applied dark or black choice.
        Prefs.sharedPrefs.edit {
            putString(getString(R.string.app_theme_key), getString(mode.entryResId))
            when (theme) {
                AppearanceTheme.DARK -> putString(getString(R.string.night_theme_key), getString(NightTheme.DARK.entryResId))
                AppearanceTheme.BLACK -> putString(getString(R.string.night_theme_key), getString(NightTheme.BLACK.entryResId))
                else -> Unit
            }
        }
        applyTheme()
    }

    private fun changeNightTheme(theme: NightTheme) {
        if (state.nightTheme == theme) return
        Prefs.nightTheme = theme
        applyTheme()
    }

    private fun applyTheme() {
        val previous = Themes.currentTheme
        Themes.updateCurrentTheme(requireContext())
        updateAppearance()
        if (previous != Themes.currentTheme) ActivityCompat.recreate(requireActivity())
    }

    private fun chooseBackground() {
        try {
            backgroundImageResultLauncher.launch("image/*")
        } catch (exception: ActivityNotFoundException) {
            Timber.w(exception, "No app found to select a background image")
            showSnackbar(CommonString.activity_start_failed)
        }
    }

    private fun removeBackground() {
        removeDialog = false
        val removed = BackgroundImage.remove(requireContext())
        updateAppearance()
        showSnackbar(if (removed) CommonString.background_image_removed else CommonString.error_deleting_image)
    }

    private val backgroundImageResultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { selectedImage ->
            if (selectedImage == null) {
                if (state.hasBackground) removeDialog = true else showSnackbar(CommonString.no_image_selected)
                return@registerForActivityResult
            }
            try {
                when (val sizeResult = BackgroundImage.validateBackgroundImageFileSize(this, selectedImage)) {
                    is FileSizeResult.FileTooLarge -> {
                        showThemedToast(requireContext(), getString(CommonString.image_max_size_allowed, sizeResult.maxMB), false)
                    }
                    is FileSizeResult.UncompressedBitmapTooLarge -> {
                        showThemedToast(
                            requireContext(),
                            getString(CommonString.image_dimensions_too_large, sizeResult.width, sizeResult.height),
                            false,
                        )
                    }
                    is FileSizeResult.OK -> {
                        BackgroundImage.import(this, selectedImage)
                        updateAppearance()
                    }
                }
            } catch (e: OutOfMemoryError) {
                Timber.w(e)
                showSnackbar(getString(CommonString.error_selecting_image, e.localizedMessage))
            } catch (e: Exception) {
                Timber.w(e)
                showSnackbar(getString(CommonString.error_selecting_image, e.localizedMessage))
            }
        }

    companion object {
        private const val REMOVE_DIALOG = "remove_background_dialog"
    }
}
