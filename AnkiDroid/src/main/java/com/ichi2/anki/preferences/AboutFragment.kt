// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.BuildConfig
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.destinations.ChangelogDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.scheduling.Fsrs
import com.ichi2.anki.servicelayer.DebugInfoService
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.openUrl
import com.ichi2.utils.IntentUtil
import com.ichi2.utils.TruncatedString
import com.ichi2.utils.VersionUtils.pkgVersionName
import com.ichi2.utils.copyToClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import net.ankiweb.rsdroid.BuildConfig as BackendBuildConfig

class AboutFragment : Fragment() {
    private var developerClicks by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        developerClicks = savedInstanceState?.getInt(DEVELOPER_CLICKS) ?: 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(DEVELOPER_CLICKS, developerClicks)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val locale = resources.configuration.locales[0] ?: Locale.getDefault()
        val buildDate =
            SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "d MMM yyyy"), locale)
                .format(Date(BuildConfig.BUILD_TIME))
        val backendVersion = "(anki ${BackendBuildConfig.ANKI_DESKTOP_VERSION} / ${BackendBuildConfig.ANKI_COMMIT_HASH.take(8)})"
        val info =
            AboutScreenState(
                pkgVersionName,
                buildDate,
                backendVersion,
                Fsrs.displayVersion?.let { "($it)" },
                TR.sentenceCase.copyDebugInfo,
                BuildConfig.SHOW_DONATE_LINKS,
            )
        return settingsComposeView(R.id.settings_about) {
            AboutScreen(
                state = info,
                searchTarget = settingsSearchTarget,
                onBack = settingsBackAction,
                onLogo = {
                    if (!Prefs.isDeveloperOptionsEnabled && developerClicks < CLICK_LIMIT) developerClicks++
                },
                onLink = { requireContext().openUrl(it) },
                onRate = { IntentUtil.tryOpenIntent(requireActivity(), AnkiDroidApp.getMarketIntent(requireContext())) },
                onChangelog = { navigate(ChangelogDestination) },
                onCopyDebugInfo = ::copyDebugInfo,
            )
            if (developerClicks == CLICK_LIMIT && !Prefs.isDeveloperOptionsEnabled) {
                EnableDeveloperOptionsDialog(
                    onCancel = { developerClicks = 0 },
                    onEnable = {
                        developerClicks = 0
                        Prefs.isDeveloperOptionsEnabled = true
                        requireActivity().recreate()
                        showThemedToast(requireContext(), R.string.dev_options_enabled_msg, shortLength = true)
                    },
                )
            }
        }
    }

    private fun copyDebugInfo() {
        launchCatchingTask {
            val debugInfo = withContext(Dispatchers.IO) { DebugInfoService.getDebugInfo(requireContext()) }
            requireContext().copyToClipboard(
                TruncatedString.from(debugInfo),
                failureMessageId = CommonString.about_ankidroid_error_copy_debug_info,
            )
        }
    }

    companion object {
        private const val DEVELOPER_CLICKS = "developer_clicks"
        private const val CLICK_LIMIT = 6
    }
}
