// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Eric Li <ericli3690@gmail.com>

package com.ichi2.anki.reviewreminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.R
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.canUserAccessDeck
import com.ichi2.anki.launchCatchingViewTask
import com.ichi2.anki.migrateLegacyNotifications
import com.ichi2.anki.preferences.PreferencesFragment
import com.ichi2.anki.reviewreminders.AddEditReminderDialog.Companion.registerAddEditReminderHandler
import com.ichi2.anki.utils.ConfigAwareSingleFragmentActivity
import com.ichi2.anki.utils.ext.getParcelableCompat
import com.ichi2.anki.utils.ext.setSupportActionBarText
import com.ichi2.anki.utils.showDialogFragment
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.utils.Permissions.openAppNotificationsSettingsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.parcelize.Parcelize

class ScheduleRemindersFragment : Fragment() {
    @Parcelize
    enum class ToolbarType : Parcelable {
        EXTERNAL,
        INTERNAL_COLLAPSIBLE,
        INTERNAL_NON_COLLAPSIBLE,
    }

    @Parcelize
    enum class FragmentHost(
        @IdRes val containerId: Int,
        val toolbarType: ToolbarType,
    ) : Parcelable {
        SETTINGS(R.id.settings_container, ToolbarType.INTERNAL_COLLAPSIBLE),
        STUDY_OPTIONS_FRAGMENT(R.id.studyoptions_fragment, ToolbarType.INTERNAL_NON_COLLAPSIBLE),
        STUDY_OPTIONS_FRAME(R.id.studyoptions_frame, ToolbarType.EXTERNAL),
        STANDALONE_ACTIVITY(R.id.fragment_container, ToolbarType.INTERNAL_NON_COLLAPSIBLE),
    }

    private val scope by lazy { arguments?.getParcelableCompat<ReviewReminderScope>(ARG_SCOPE) ?: ReviewReminderScope.Global }
    private val host by lazy { arguments?.getParcelableCompat<FragmentHost>(ARG_HOST) ?: FragmentHost.SETTINGS }
    private val hasExternalToolbar get() =
        host.toolbarType == ToolbarType.EXTERNAL &&
            parentFragment !is com.ichi2.anki.StudyOptionsFlowFragment

    private val troubleshootingViewModel: ReminderTroubleshootingViewModel by activityViewModels {
        reminderTroubleshootingViewModelFactory(requireContext())
    }
    private val reminders = MutableStateFlow<List<ReminderRow>>(emptyList())
    private val subtitle = MutableStateFlow<String?>(null)
    private val snackbar = SnackbarHostState()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnkiDroidTheme {
                    val rows by reminders.collectAsStateWithLifecycle()
                    val deckName by subtitle.collectAsStateWithLifecycle()
                    val checks by troubleshootingViewModel.state.collectAsStateWithLifecycle()
                    ReviewRemindersScreen(
                        rows = rows,
                        deckName = deckName.takeUnless { hasExternalToolbar },
                        onNavigateUp = if (hasExternalToolbar) null else ::navigateUp,
                        showWarning = checks.summaryStatus == SummaryStatus.Error && rows.any { it.reminder.enabled },
                        snackbar = snackbar,
                        onAdd = { openEditor(AddEditReminderDialog.DialogMode.Add(scope)) },
                        onEdit = { openEditor(AddEditReminderDialog.DialogMode.Edit(it)) },
                        onToggle = ::setReminderEnabled,
                        onCheck = ::openTroubleshootingScreen,
                        onSettings = { openAppNotificationsSettingsScreen() },
                    )
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        launchCatchingViewTask {
            migrateLegacyNotifications(requireContext())
            subtitle.value = (scope as? ReviewReminderScope.DeckSpecific)?.getDeckName()
            if (hasExternalToolbar) {
                setSupportActionBarText(title = getString(CommonString.review_reminders_title), subtitle = subtitle.value)
                requireActivity().invalidateMenu()
            }
            refreshReminders()
        }
        registerAddEditReminderHandler(::saveEditorResult)
    }

    override fun onResume() {
        super.onResume()
        troubleshootingViewModel.refreshChecks()
    }

    internal fun navigateUp() {
        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
        } else {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private suspend fun refreshReminders() {
        val group =
            when (scope) {
                ReviewReminderScope.Global -> ReviewRemindersDatabase.getAllReminders()
                is ReviewReminderScope.DeckSpecific -> ReviewRemindersDatabase.getRemindersForScope(scope)
            }
        val deckNames = mutableMapOf<Long, String?>()
        reminders.value =
            group
                .getRemindersList()
                .map { reminder ->
                    val deck = reminder.scope as? ReviewReminderScope.DeckSpecific
                    val deckName =
                        deck?.let {
                            if (it.did !in deckNames) {
                                deckNames[it.did] = if (canUserAccessDeck(it.did)) it.getDeckName() else null
                            }
                            deckNames[it.did]
                        }
                    ReminderRow(reminder, deckName)
                }.sortedWith(compareBy({ it.deletedDeck }, { it.reminder.time.toSecondsFromMidnight() }))
        ReviewRemindersDatabase.checkDeserializationErrors(requireContext())
    }

    private fun saveEditorResult(
        updated: ReviewReminder?,
        mode: AddEditReminderDialog.DialogMode,
    ) = launchCatchingViewTask {
        val original = (mode as? AddEditReminderDialog.DialogMode.Edit)?.reminderToBeEdited
        ReviewRemindersDatabase.replaceReminder(original, updated)
        val context = requireContext()
        original?.let { ReviewReminderAlarmManager.unscheduleReviewReminderNotifications(context, it) }
        if (updated?.enabled == true) {
            ReviewReminderAlarmManager.scheduleReviewReminderNotification(context, updated, attemptImmediateNotification = false)
        }
        refreshReminders()
        if (troubleshootingViewModel.state.value.summaryStatus != SummaryStatus.Error) {
            val message =
                when {
                    original == null -> R.string.review_reminder_added
                    updated == null -> R.string.review_reminder_deleted
                    else -> R.string.review_reminder_saved
                }
            snackbar.showSnackbar(getString(message))
        }
    }

    private fun setReminderEnabled(
        reminder: ReviewReminder,
        enabled: Boolean,
    ) {
        launchCatchingViewTask {
            val updated = ReviewRemindersDatabase.setReminderEnabled(reminder, enabled) ?: return@launchCatchingViewTask
            val context = requireContext()
            if (enabled) {
                ReviewReminderAlarmManager.scheduleReviewReminderNotification(context, updated, attemptImmediateNotification = false)
            } else {
                ReviewReminderAlarmManager.unscheduleReviewReminderNotifications(context, updated)
            }
            refreshReminders()
        }
    }

    private fun openTroubleshootingScreen() {
        val settings = parentFragment as? PreferencesFragment
        if (settings != null) {
            settings.openSubpage(ReminderTroubleshootingFragment.newInstance(FragmentHost.SETTINGS))
        } else if (activity is DeckPicker) {
            val page = ReminderTroubleshootingFragment.newInstance(FragmentHost.STANDALONE_ACTIVITY)
            startActivity(SingleFragmentActivity.getIntent(requireContext(), ReminderTroubleshootingFragment::class, page.arguments))
        } else {
            parentFragmentManager.commit {
                replace(host.containerId, ReminderTroubleshootingFragment.newInstance(host))
                addToBackStack(null)
            }
        }
    }

    private fun openEditor(mode: AddEditReminderDialog.DialogMode) {
        childFragmentManager.showDialogFragment(AddEditReminderDialog.getInstance(mode))
    }

    companion object {
        private const val ARG_SCOPE = "arg_scope"
        private const val ARG_HOST = "arg_host"

        fun getIntent(
            context: Context,
            scope: ReviewReminderScope,
        ): Intent =
            ConfigAwareSingleFragmentActivity.getIntent(
                context,
                ScheduleRemindersFragment::class,
                reminderArguments(scope, FragmentHost.STANDALONE_ACTIVITY),
            )

        fun newInstance(
            scope: ReviewReminderScope,
            host: FragmentHost,
        ): ScheduleRemindersFragment = ScheduleRemindersFragment().apply { arguments = reminderArguments(scope, host) }

        private fun reminderArguments(
            scope: ReviewReminderScope,
            host: FragmentHost,
        ): Bundle =
            Bundle().apply {
                putParcelable(ARG_SCOPE, scope)
                putParcelable(ARG_HOST, host)
            }
    }
}
