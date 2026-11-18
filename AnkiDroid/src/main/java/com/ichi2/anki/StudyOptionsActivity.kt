// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2012 Norbert Nagold <norbert.nagold@gmail.com>

package com.ichi2.anki

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.view.menu.MenuBuilder
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import anki.collection.OpChanges
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.common.destinations.DeferredNavigation
import com.ichi2.anki.common.destinations.ReviewDeckDestination
import com.ichi2.anki.common.destinations.toIntent
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.CustomStudyAction
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.CustomStudyAction.Companion.REQUEST_KEY
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.libanki.undoAvailable
import com.ichi2.anki.libanki.undoLabel
import com.ichi2.anki.multimedia.AttachmentAppBar
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.observability.ChangeManager
import com.ichi2.anki.reviewreminders.ReviewReminderScope
import com.ichi2.anki.reviewreminders.ScheduleRemindersFragment
import androidx.appcompat.R as AppCompatR
import com.ichi2.anki.common.android.R as CommonR

class StudyOptionsActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment = StudyOptionsFlowFragment()
}

/** Standalone deck overview, including its menu, study action and deck-specific reminders. */
class StudyOptionsFlowFragment :
    Fragment(),
    ChangeManager.Subscriber {
    private var undo by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChangeManager.subscribe(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentContainerView(inflater.context).apply { id = R.id.studyoptions_frame }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (childFragmentManager.findFragmentById(R.id.studyoptions_frame) == null) {
            childFragmentManager.commit { replace(R.id.studyoptions_frame, StudyOptionsFragment()) }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            finishScreen(Activity.RESULT_OK)
        }
        childFragmentManager.setFragmentResultListener(REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            when (CustomStudyAction.fromBundle(bundle)) {
                CustomStudyAction.CUSTOM_STUDY_SESSION, CustomStudyAction.EXTEND_STUDY_LIMITS ->
                    (childFragmentManager.findFragmentById(R.id.studyoptions_frame) as? StudyOptionsFragment)?.refreshInterface()
            }
        }
    }

    internal fun startStudy() {
        val activity = requireActivity()
        val reviewer = with(DeferredNavigation) { ReviewDeckDestination.CurrentDeck.toIntent() }
        reviewer.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        startActivity(reviewer)
        if (activity is StudyOptionsActivity) activity.finish()
    }

    internal fun showReminders(deck: DeckId) {
        startActivity(ScheduleRemindersFragment.getIntent(requireContext(), ReviewReminderScope.DeckSpecific(deck)))
    }

    @Composable
    internal fun Toolbar(pane: StudyOptionsFragment) {
        var expanded by remember { mutableStateOf(false) }
        val menu = MenuBuilder(requireContext())
        pane.onCreateMenu(menu, requireActivity().menuInflater)
        pane.onPrepareMenu(menu)
        Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            AttachmentAppBar("", onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }) {
                undo?.let { label ->
                    IconButton(onClick = { launchCatchingViewTask { undoAndShowSnackbar() } }) {
                        Icon(painterResource(R.drawable.symbol_undo), label)
                    }
                }
                val direct =
                    mapOf(
                        R.id.action_schedule_reminders to R.drawable.symbol_notifications,
                        R.id.action_deck_or_study_options to R.drawable.symbol_tune,
                    )
                for ((id, icon) in direct) {
                    val item = menu.findItem(id)
                    if (item?.isVisible == true) {
                        IconButton(onClick = { pane.onMenuItemSelected(item) }) {
                            Icon(painterResource(icon), item.title.toString())
                        }
                    }
                }
                val more = menu.children.filter { it.isVisible && it.itemId !in direct }.toList()
                if (more.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                painterResource(CommonR.drawable.symbol_more_vert),
                                stringResource(AppCompatR.string.abc_action_menu_overflow_description),
                            )
                        }
                        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                            for (item in more) {
                                DropdownMenuItem(text = { Text(item.title.toString()) }, onClick = {
                                    expanded = false
                                    pane.onMenuItemSelected(item)
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUndo()
    }

    override fun opExecuted(
        changes: OpChanges,
        handler: Any?,
    ) {
        if (view != null) refreshUndo()
    }

    private fun refreshUndo() {
        launchCatchingViewTask { undo = withCol { if (undoAvailable()) undoLabel() else null } }
    }
}
