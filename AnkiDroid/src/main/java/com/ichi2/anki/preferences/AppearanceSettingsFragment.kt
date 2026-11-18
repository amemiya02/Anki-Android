// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.common.utils.android.systemIsInNightMode
import com.ichi2.anki.deckpicker.BackgroundImage
import com.ichi2.anki.deckpicker.BackgroundImage.FileSizeResult
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.settings.enums.AppTheme
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.themes.Themes
import com.ichi2.themes.Themes.updateCurrentTheme
import com.ichi2.utils.negativeButton
import com.ichi2.utils.positiveButton
import com.ichi2.utils.show
import com.ichi2.utils.title
import timber.log.Timber
import com.ichi2.anki.common.android.R as CommonR

class AppearanceSettingsFragment : SettingsFragment() {
    private var backgroundImage: Preference? = null
    private var removeBackgroundPref: Preference? = null
    override val preferenceResource: Int
        get() = R.xml.preferences_appearance
    override val analyticsScreenNameConstant: String
        get() = "prefs.appearance"

    override fun initSubscreen() {
        preferenceScreen.title = TR.preferencesAppearance()

        // Configure background
        backgroundImage = requirePreference<Preference>("deckPickerBackground")
        backgroundImage!!.title = TR.sentenceCase.selectImage
        removeBackgroundPref = requirePreference<Preference>("removeWallPaper")
        backgroundImage!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                try {
                    backgroundImageResultLauncher.launch("image/*")
                } catch (ex: ActivityNotFoundException) {
                    Timber.w(ex, "No app found to handle background preference change request")
                    activity?.showSnackbar(CommonString.activity_start_failed)
                }
                true
            }
        removeBackgroundPref?.setOnPreferenceClickListener {
            showRemoveBackgroundImageDialog()
            true
        }

        // Initially update visibility based on whether a background exists
        updateRemoveBackgroundVisibility()

        setupThemePreferences()
    }

    private fun updateRemoveBackgroundVisibility() {
        removeBackgroundPref?.isVisible = BackgroundImage.shouldBeShown(requireContext())
    }

    private fun showRemoveBackgroundImageDialog() {
        AlertDialog.Builder(requireContext()).show {
            title(CommonString.remove_background_image)
            positiveButton(CommonString.dialog_remove) {
                if (BackgroundImage.remove(requireContext())) {
                    showSnackbar(CommonString.background_image_removed)
                    updateRemoveBackgroundVisibility()
                } else {
                    showSnackbar(CommonString.error_deleting_image)
                }
            }
            negativeButton(CommonString.dialog_keep)
        }
    }

    private fun setupThemePreferences() {
        val appTheme = Prefs.appTheme
        val appThemePref = requirePreference<ListPreference>(R.string.app_theme_key)
        val nightThemePref = requirePreference<ListPreference>(R.string.night_theme_key)

        nightThemePref.isEnabled = appTheme != AppTheme.DAY

        // Remove follow system options in android versions which do not have system dark mode
        // When minSdk reaches 29, the only necessary change is to remove this if-block
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Drop "Follow system" option (the first one)
            appThemePref.entries = resources.getStringArray(R.array.app_theme_labels).drop(1).toTypedArray()
            appThemePref.entryValues = resources.getStringArray(CommonR.array.app_theme_values).drop(1).toTypedArray()
            if (appTheme == AppTheme.FOLLOW_SYSTEM) {
                appThemePref.value = getString(Themes.currentTheme.entryResId)
            }
        }

        appThemePref.setOnPreferenceChangeListener { newValue ->
            if (newValue != appThemePref.value) {
                val previousThemeId = Themes.currentTheme.styleResId
                appThemePref.value = newValue
                updateCurrentTheme(requireContext())

                if (previousThemeId != Themes.currentTheme.styleResId) {
                    ActivityCompat.recreate(requireActivity())
                } else {
                    nightThemePref.isEnabled = newValue != getString(AppTheme.DAY.entryResId)
                }
            }
        }

        nightThemePref.setOnPreferenceChangeListener { newValue ->
            if (
                newValue != nightThemePref.value &&
                (appTheme == AppTheme.NIGHT || (appTheme == AppTheme.FOLLOW_SYSTEM && systemIsInNightMode(requireContext())))
            ) {
                ActivityCompat.recreate(requireActivity())
            }
        }
    }

    private val backgroundImageResultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { selectedImage ->
            if (selectedImage == null) {
                if (BackgroundImage.shouldBeShown(requireContext())) {
                    showRemoveBackgroundImageDialog()
                } else {
                    showSnackbar(CommonString.no_image_selected)
                }
                return@registerForActivityResult
            }
            // handling file may result in exception
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
                        updateRemoveBackgroundVisibility()
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
}
