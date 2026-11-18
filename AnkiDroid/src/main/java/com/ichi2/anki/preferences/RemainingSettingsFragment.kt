// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.XmlRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.analytics.Analytics
import com.ichi2.anki.common.analytics.AnalyticsEvent
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.preferences.DialogPreference
import com.ichi2.preferences.HtmlHelpPreference
import com.ichi2.preferences.Preference
import com.ichi2.preferences.PreferenceCategory
import com.ichi2.preferences.PreferenceGroup
import com.ichi2.preferences.PreferenceScreen
import com.ichi2.preferences.loadSettings
import com.ichi2.utils.FragmentFactoryUtils

/** Hosts the Compose Settings page and its editor in the existing Settings navigation. */
abstract class RemainingSettingsFragment(
    @IdRes private val composeViewId: Int,
) : Fragment(),
    PreferenceXmlSource {
    private lateinit var screen: PreferenceScreen
    private var activePreferenceKey by mutableStateOf<String?>(null)
    private var dialogContent by mutableStateOf<(@Composable (() -> Unit) -> Unit)?>(null)

    protected abstract val analyticsScreenNameConstant: String

    protected abstract fun initSubscreen()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.sendAnalyticsScreenView(analyticsScreenNameConstant)
        activePreferenceKey = savedInstanceState?.getString(ACTIVE_PREFERENCE)
        screen = requireContext().loadSettings(preferenceResource)
        screen.onSettingTapped = { Analytics.send(AnalyticsEvent.SettingTapped(it.key)) }
        screen.openEditor = { activePreferenceKey = it.key }
        screen.openSubpage = {
            val parent = parentFragment as? PreferencesFragment
            parent?.openSubpage(FragmentFactoryUtils.instantiate<Fragment>(requireActivity(), it))
        }
        screen.applySettingsMetadata(preferenceResource)
        initSubscreen()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = settingsComposeView(composeViewId) { SettingsBody() }

    override fun onDestroyView() {
        dialogContent = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ACTIVE_PREFERENCE, activePreferenceKey)
    }

    fun <T : Preference> findPreference(key: String): T? = screen.findPreference(key)

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
        get() = screen.title.toString()

    @Composable
    private fun SettingsBody() {
        SettingsScrollPage(settingsPageTitle, settingsBackAction) {
            PreferenceGroupContent(screen)
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
    private fun PreferenceGroupContent(group: PreferenceGroup) {
        for (preference in group.children) {
            if (!preference.isVisible) continue
            key(preference) {
                PreferenceContent(preference)
                if (preference is PreferenceGroup) PreferenceGroupContent(preference)
            }
        }
    }

    @Composable
    private fun PreferenceContent(preference: Preference) {
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
                SettingsSearchTarget(preference.key == settingsSearchTarget) { modifier, highlighted ->
                    PreferenceRow(preference, modifier, highlighted)
                }
        }
    }
}

private const val ACTIVE_PREFERENCE = "active_preference"

interface PreferenceXmlSource {
    @get:XmlRes
    val preferenceResource: Int
}
