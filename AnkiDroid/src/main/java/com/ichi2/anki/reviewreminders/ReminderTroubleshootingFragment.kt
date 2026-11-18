// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.reviewreminders

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ichi2.anki.CommonString
import com.ichi2.anki.NotificationChannel
import com.ichi2.anki.R
import com.ichi2.anki.preferences.SettingsDivider
import com.ichi2.anki.utils.ext.onWindowFocusChanged
import com.ichi2.anki.utils.ext.requireParcelable
import com.ichi2.anki.utils.ext.setSupportActionBarText
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import com.ichi2.utils.Permissions
import com.ichi2.utils.Permissions.attemptToEnableNotifications
import com.ichi2.utils.Permissions.openAppNotificationsSettingsScreen
import com.ichi2.utils.TruncatedString
import com.ichi2.utils.copyToClipboard
import kotlinx.coroutines.launch
import timber.log.Timber

private data class ResolveCheckAction(
    val label: String,
    val logDescription: String,
    val action: () -> Unit,
)

class ReminderTroubleshootingFragment : Fragment() {
    private val viewModel: ReminderTroubleshootingViewModel by activityViewModels {
        reminderTroubleshootingViewModelFactory(requireContext())
    }

    private val host by lazy { requireArguments().requireParcelable<ScheduleRemindersFragment.FragmentHost>(ARG_HOST) }
    private val hasExternalToolbar get() = host.toolbarType == ScheduleRemindersFragment.ToolbarType.EXTERNAL

    internal val notificationPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refreshChecks() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnkiDroidTheme {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    ReminderPage(
                        onNavigateUp = if (hasExternalToolbar) null else ({ requireActivity().onBackPressedDispatcher.onBackPressed() }),
                    ) {
                        val paper = MaterialTheme.foundation
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
                        ) {
                            if (!hasExternalToolbar) ReminderPageTitle(stringResource(R.string.review_reminder_check_title))
                            CheckSummary(state.summaryStatus)
                            SettingsDivider()
                            state.checks.filter { it.result != CheckResult.Unavailable }.forEach { check ->
                                val action = if (check.result.hasIssue) check.resolveAction() else null
                                TroubleshootingCheckRow(check, action)
                            }
                            Text(
                                stringResource(R.string.review_reminder_check_footer),
                                Modifier.padding(top = 16.dp, bottom = 4.dp),
                                style = paper.type.caption,
                                color = paper.colors.onBackgroundMuted,
                            )
                            TextButton(onClick = ::copyDetails) {
                                Icon(painterResource(R.drawable.symbol_content_copy), contentDescription = null, Modifier.size(18.dp))
                                Text(
                                    stringResource(R.string.review_reminder_check_copy),
                                    Modifier.padding(start = 8.dp),
                                    color = paper.colors.emphasis,
                                    style = paper.type.button,
                                )
                            }
                        }
                    }
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (hasExternalToolbar) {
            setSupportActionBarText(title = getString(R.string.review_reminder_check_title))
            requireActivity().invalidateMenu()
        }
        // A change from the system shade need not pause this fragment.
        onWindowFocusChanged { hasFocus -> if (hasFocus) viewModel.refreshChecks() }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshChecks()
    }

    private fun copyDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            val details = ReminderLogTree.readReminderLog() + "\n\n" + ReviewRemindersDatabase.dumpContentsToString()
            requireContext().copyToClipboard(
                TruncatedString.from(details),
                failureMessageId = CommonString.about_ankidroid_error_copy_debug_info,
            )
        }
    }

    companion object {
        private const val ARG_HOST = "arg_host"

        fun newInstance(host: ScheduleRemindersFragment.FragmentHost): ReminderTroubleshootingFragment =
            ReminderTroubleshootingFragment().apply {
                arguments = Bundle().apply { putParcelable(ARG_HOST, host) }
            }
    }
}

/** Both reminder pages share this activity-scoped model. */
internal fun reminderTroubleshootingViewModelFactory(context: Context): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { ReminderTroubleshootingViewModel(ReminderTroubleshootingRepository(context)) }
    }

@Composable
private fun CheckSummary(status: SummaryStatus) {
    val paper = MaterialTheme.foundation
    val (icon, title) =
        when (status) {
            SummaryStatus.Ok -> R.drawable.symbol_check_circle to CommonString.reminder_troubleshooting_summary_ok
            SummaryStatus.Warning -> R.drawable.symbol_error to R.string.review_reminder_check_warning
            SummaryStatus.Error -> R.drawable.symbol_cancel to CommonString.reminder_troubleshooting_summary_error
        }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(56.dp).border(paper.shapes.hairline, paper.colors.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = if (status == SummaryStatus.Ok) paper.colors.answerGood else paper.colors.emphasis,
            )
        }
        Text(stringResource(title), style = paper.type.rowTitle, color = paper.colors.onBackground)
    }
}

@Composable
private fun TroubleshootingCheckRow(
    check: TroubleshootingCheck,
    action: ResolveCheckAction?,
) {
    val paper = MaterialTheme.foundation
    val context = LocalContext.current
    val title = with(context) { check.title() }
    val status = with(context) { check.statusName() }
    val explanation = with(context) { check.explanation() }
    val iconColor =
        when {
            check.result.hasIssue -> paper.colors.emphasis
            check.result == CheckResult.Passed -> paper.colors.answerGood
            else -> paper.colors.onBackgroundMuted
        }
    Row(
        Modifier.fillMaxWidth().heightIn(min = paper.spacing.settingsRow).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(painterResource(check.result.iconRes()), contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = paper.type.rowTitle, color = paper.colors.onBackground)
            explanation?.let {
                Text(it, Modifier.padding(top = 4.dp), style = paper.type.caption, color = paper.colors.onBackgroundMuted)
            }
            if (action != null) {
                TextButton(onClick = {
                    Timber.i("Launching fix for '%s': %s", title, action.logDescription)
                    action.action()
                }) {
                    Text(action.label, style = paper.type.button, color = paper.colors.emphasis)
                }
            }
        }
        status?.let {
            Text(
                it,
                Modifier.widthIn(max = 116.dp),
                style = paper.type.body,
                color = if (check.result.hasIssue) paper.colors.emphasis else paper.colors.onBackgroundMuted,
                textAlign = TextAlign.End,
            )
        }
    }
    SettingsDivider()
}

context(context: Context)
private fun TroubleshootingCheck.title(): String =
    context.getString(
        when (this) {
            is TroubleshootingCheck.NotificationPermission -> R.string.review_reminder_check_notifications
            is TroubleshootingCheck.NotificationChannelEnabled -> R.string.review_reminder_check_channel
            is TroubleshootingCheck.DoNotDisturbOff -> R.string.review_reminder_check_dnd
            is TroubleshootingCheck.UnrestrictedOptimizationEnabled -> R.string.review_reminder_check_battery
            is TroubleshootingCheck.PowerSavingModeOff -> R.string.review_reminder_check_battery_saver
            is TroubleshootingCheck.ExactAlarmPermission -> R.string.review_reminder_check_alarms
        },
    )

context(context: Context)
private fun TroubleshootingCheck.statusName(): String? {
    if (result == CheckResult.Loading) return context.getString(R.string.review_reminder_checking)
    if (result is CheckResult.Error) return context.getString(R.string.review_reminder_check_failed)
    val passed = result == CheckResult.Passed
    val statusRes =
        when (this) {
            is TroubleshootingCheck.NotificationPermission,
            is TroubleshootingCheck.ExactAlarmPermission,
            ->
                if (passed) {
                    R.string.review_reminder_check_allowed
                } else {
                    R.string.review_reminder_check_denied
                }
            is TroubleshootingCheck.NotificationChannelEnabled ->
                if (passed) CommonString.reminder_troubleshooting_status_on else CommonString.reminder_troubleshooting_status_off
            is TroubleshootingCheck.DoNotDisturbOff,
            is TroubleshootingCheck.PowerSavingModeOff,
            ->
                if (passed) {
                    CommonString.reminder_troubleshooting_status_off
                } else {
                    CommonString.reminder_troubleshooting_status_on
                }
            is TroubleshootingCheck.UnrestrictedOptimizationEnabled ->
                when (result) {
                    is CheckResult.Passed -> CommonString.reminder_troubleshooting_status_unrestricted
                    is CheckResult.Warning -> CommonString.reminder_troubleshooting_status_optimized
                    is CheckResult.Failed -> CommonString.reminder_troubleshooting_status_restricted
                    else -> null
                }
        }
    return statusRes?.let { context.getString(it) }
}

context(context: Context)
private fun TroubleshootingCheck.explanation(): String? {
    val explanationRes =
        when (this) {
            // no need for an explanation: the 'grant permission' action should be sufficient
            is TroubleshootingCheck.NotificationPermission -> null
            is TroubleshootingCheck.NotificationChannelEnabled ->
                if (result.hasIssue) CommonString.reminder_troubleshooting_explanation_notification_channel else null
            is TroubleshootingCheck.DoNotDisturbOff ->
                if (result.hasIssue) CommonString.reminder_troubleshooting_explanation_do_not_disturb else null
            is TroubleshootingCheck.UnrestrictedOptimizationEnabled ->
                when (result) {
                    is CheckResult.Warning -> R.string.review_reminder_check_battery_explanation
                    is CheckResult.Failed -> CommonString.reminder_troubleshooting_explanation_battery_restricted
                    else -> null
                }
            is TroubleshootingCheck.PowerSavingModeOff ->
                if (result.hasIssue) CommonString.reminder_troubleshooting_explanation_power_saving_mode else null
            is TroubleshootingCheck.ExactAlarmPermission ->
                if (result.hasIssue) CommonString.reminder_troubleshooting_explanation_exact_alarm else null
        }
    return explanationRes?.let { context.getString(it) }
}

private fun CheckResult.iconRes(): Int =
    when (this) {
        is CheckResult.Loading -> R.drawable.symbol_sync
        is CheckResult.Passed -> R.drawable.symbol_check_circle
        is CheckResult.Failed -> R.drawable.symbol_cancel
        is CheckResult.Warning -> R.drawable.symbol_warning
        is CheckResult.Unavailable -> R.drawable.symbol_error
        is CheckResult.Error -> R.drawable.symbol_error
    }

/**
 * Returns a [ResolveCheckAction] for the check.
 */
context(fragment: ReminderTroubleshootingFragment)
private fun TroubleshootingCheck.resolveAction(): ResolveCheckAction? {
    val context = fragment.requireContext()

    fun requestNotificationPermission(): ResolveCheckAction? =
        ResolveCheckAction(
            label = context.getString(CommonString.reminder_troubleshooting_action_grant_permission),
            logDescription = "requesting POST_NOTIFICATIONS via system dialog or app settings",
        ) {
            fragment.attemptToEnableNotifications(fragment.notificationPermissionLauncher)
        }

    fun requestReminderNotifChannelPermission(): ResolveCheckAction? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return ResolveCheckAction(
            label = context.getString(CommonString.reminder_troubleshooting_action_enable_notification_channel),
            logDescription = "opening app notification settings screen",
        ) {
            fragment.openAppNotificationsSettingsScreen(highlightedChannel = NotificationChannel.REVIEW_REMINDERS)
        }
    }

    fun requestUnrestrictedBackgroundUsage(): ResolveCheckAction {
        fun openBatteryOptimizationList() = context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

        return if (Permissions.canRequestIgnoreBatteryOptimizations(context)) {
            ResolveCheckAction(
                label = context.getString(R.string.review_reminder_check_allow_background),
                logDescription = "opening ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            ) {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                } catch (e: ActivityNotFoundException) {
                    // not all devices can request an exemption
                    Timber.w(e, "cannot request a battery optimization exemption; opening the list")
                    openBatteryOptimizationList()
                }
            }
        } else {
            ResolveCheckAction(
                label = context.getString(CommonString.reminder_troubleshooting_action_open_battery_settings),
                logDescription = "opening ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS",
            ) {
                openBatteryOptimizationList()
            }
        }
    }

    fun openBatterySaverSettings() =
        ResolveCheckAction(
            label = context.getString(CommonString.reminder_troubleshooting_action_open_battery_settings),
            logDescription = "opening ACTION_BATTERY_SAVER_SETTINGS",
        ) {
            context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        }

    fun openExactAlarmSettings(): ResolveCheckAction? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return ResolveCheckAction(
            label = context.getString(CommonString.reminder_troubleshooting_action_grant_permission),
            logDescription = "opening ACTION_REQUEST_SCHEDULE_EXACT_ALARM",
        ) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                },
            )
        }
    }

    return when (this) {
        is TroubleshootingCheck.NotificationPermission -> requestNotificationPermission()
        is TroubleshootingCheck.NotificationChannelEnabled -> requestReminderNotifChannelPermission()
        is TroubleshootingCheck.DoNotDisturbOff -> null
        is TroubleshootingCheck.UnrestrictedOptimizationEnabled -> requestUnrestrictedBackgroundUsage()
        is TroubleshootingCheck.PowerSavingModeOff -> openBatterySaverSettings()
        is TroubleshootingCheck.ExactAlarmPermission -> openExactAlarmSettings()
    }
}
