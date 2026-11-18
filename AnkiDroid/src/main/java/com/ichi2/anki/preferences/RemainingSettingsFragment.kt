// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroupAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.analytics.Analytics
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.preferences.HtmlHelpPreference

/** Uses the existing typed preference model and callbacks with a Compose-only presentation. */
abstract class RemainingSettingsFragment(
    @IdRes private val composeViewId: Int,
) : SettingsFragment() {
    private lateinit var modelChanges: PreferenceGroupAdapter
    private var revision by mutableIntStateOf(0)
    private var activePreferenceKey by mutableStateOf<String?>(null)
    private var dialogContent by mutableStateOf<(@Composable (() -> Unit) -> Unit)?>(null)
    private val updates =
        object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                revision++
            }

            override fun onItemRangeChanged(
                positionStart: Int,
                itemCount: Int,
                payload: Any?,
            ) {
                revision++
            }
        }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        Analytics.sendAnalyticsScreenView(analyticsScreenNameConstant)
        // Install only the model, bypassing PreferenceFragmentCompat's RecyclerView binding.
        val screen = preferenceManager.inflateFromResource(requireContext(), preferenceResource, null)
        preferenceManager.setPreferences(screen)
        screen.applySettingsMetadata(preferenceResource)
        initSubscreen()
        activePreferenceKey = savedInstanceState?.getString(ACTIVE_PREFERENCE)
        modelChanges = PreferenceGroupAdapter(screen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        preferenceScreen.onAttached()
        modelChanges.registerAdapterDataObserver(updates)
        return settingsComposeView(composeViewId) { SettingsBody(revision) }
    }

    override fun onDestroyView() {
        modelChanges.unregisterAdapterDataObserver(updates)
        preferenceScreen.onDetached()
        dialogContent = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ACTIVE_PREFERENCE, activePreferenceKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        activePreferenceKey = preference.key
    }

    protected fun showSettingsDialog(content: @Composable (dismiss: () -> Unit) -> Unit) {
        dialogContent = content
    }

    protected fun confirmSettingChange(
        title: String,
        message: String = "",
        confirmText: String = getString(CommonString.dialog_ok),
        destructive: Boolean = false,
        onConfirm: () -> Unit,
    ) = showSettingsDialog { dismiss ->
        ConfirmationDialog(
            title = title,
            message = message,
            confirm =
                DialogAction(
                    confirmText,
                    {
                        dismiss()
                        onConfirm()
                    },
                    if (destructive) DialogActionStyle.Destructive else DialogActionStyle.Filled,
                ),
            onDismissRequest = dismiss,
        )
    }

    protected open val settingsPageTitle: String
        get() = preferenceScreen.title.toString()

    @Composable
    private fun SettingsBody(revision: Int) {
        SettingsScrollPage(settingsPageTitle, settingsBackAction) {
            for (index in 0 until modelChanges.itemCount) {
                val preference = checkNotNull(modelChanges.getItem(index))
                key(preference) {
                    PreferenceContent(preference, revision)
                }
            }
        }
        val preference = activePreferenceKey?.let { findPreference<DialogPreference>(it) }
        if (preference != null) {
            key(preference) {
                SettingEditor(preference, onDismiss = { activePreferenceKey = null }, showDialog = ::showSettingsDialog)
            }
        }
        dialogContent?.invoke { dialogContent = null }
    }

    @Composable
    private fun PreferenceContent(
        preference: Preference,
        revision: Int,
    ) {
        when (preference) {
            is PreferenceCategory -> {
                preference.title?.let { SettingsSectionTitle(it.toString()) }
                preference.summary?.let {
                    Text(
                        it.toString(),
                        modifier = Modifier.padding(bottom = 12.dp),
                        style = MaterialTheme.foundation.type.caption,
                        color = MaterialTheme.foundation.colors.onBackgroundMuted,
                    )
                }
            }
            is HtmlHelpPreference -> {
                val foundation = MaterialTheme.foundation
                Text(
                    AnnotatedString.fromHtml(
                        preference.htmlSummary,
                        linkStyles =
                            TextLinkStyles(
                                SpanStyle(color = foundation.colors.emphasis, textDecoration = TextDecoration.Underline),
                            ),
                    ),
                    modifier = Modifier.padding(vertical = 12.dp),
                    style = foundation.type.body,
                    color = foundation.colors.onBackground,
                )
            }
            else ->
                SettingsSearchTarget(preference.key != null && preference.key == settingsSearchTarget) { modifier, highlighted ->
                    PreferenceRow(preference, modifier, highlighted, revision)
                }
        }
    }
}

private const val ACTIVE_PREFERENCE = "active_preference"
