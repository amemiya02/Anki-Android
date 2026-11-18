// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.content.res.Configuration
import androidx.annotation.XmlRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.children
import com.google.android.material.tabs.TabLayout
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.preferences.reviewer.ViewerAction
import com.ichi2.anki.preferences.reviewer.WhiteboardAction
import com.ichi2.anki.previewer.PreviewerAction
import com.ichi2.anki.reviewer.CardSide
import com.ichi2.anki.reviewer.MappableAction
import com.ichi2.anki.reviewer.MappableBinding.Companion.toPreferenceString
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.sharedPrefs
import com.ichi2.preferences.ControlPreference
import com.ichi2.preferences.ReviewerControlPreference
import com.ichi2.utils.negativeButton
import com.ichi2.utils.positiveButton
import com.ichi2.utils.show
import timber.log.Timber

class ControlsSettingsFragment :
    SettingsFragment(),
    TabLayout.OnTabSelectedListener {
    override val preferenceResource: Int
        get() = R.xml.preferences_controls
    override val analyticsScreenNameConstant: String
        get() = "prefs.controls"

    override fun initSubscreen() {
        requirePreference<ControlsTabPreference>(R.string.pref_controls_tab_layout_key).setOnTabSelectedListener(this)
        val initialScreen = ControlPreferenceScreen.entries.first()
        addPreferencesFromResource(initialScreen.xmlRes)
        // TODO replace the preference with something dismissible. This is meant only to improve
        //  the discoverability of the system shortcut for the shortcuts dialog.
        requirePreference<Preference>(R.string.pref_keyboard_shortcuts_key).apply {
            isVisible = resources.configuration.keyboard == Configuration.KEYBOARD_QWERTY
            setOnPreferenceClickListener {
                requireActivity().requestShowKeyboardShortcuts()
                true
            }
        }
        setControlPreferencesDefaultValues(initialScreen)
        setDynamicTitle()
        setupAnswerCommands()
    }

    /**
     * Selects the appropriate tab based on a preference key from search results.
     * This allows search navigation to automatically switch to the correct tab.
     */
    fun selectTabForPreference(key: String) {
        val targetTabIndex = actionToScreenMap[key]?.ordinal
        if (targetTabIndex == null) {
            Timber.w("Could not find the preference with %s key", key)
            return
        }

        requirePreference<ControlsTabPreference>(
            R.string.pref_controls_tab_layout_key,
        ).selectTab(targetTabIndex)
    }

    override fun revealSearchTarget(key: String) {
        if (key in actionToScreenMap) selectTabForPreference(key)
        listView.post { highlightSearchPreference(key) }
    }

    private fun setControlPreferencesDefaultValues(screen: ControlPreferenceScreen) {
        val commands = screen.getActions().associateBy { it.preferenceKey }
        val prefs = sharedPrefs()
        preferenceScreen
            .allPreferences()
            .filterIsInstance<ControlPreference>()
            .filter { pref -> pref.value == null }
            .forEach { pref -> commands[pref.key]?.getBindings(prefs)?.toPreferenceString()?.let { pref.value = it } }
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        val screen = ControlPreferenceScreen.entries[tab.position]
        Timber.v("Selected tab %d - %s", tab.position, screen.name)
        addPreferencesFromResource(screen.xmlRes)
        setControlPreferencesDefaultValues(screen)
        setDynamicTitle()
        setupAnswerCommands()
    }

    @NeedsTest("Only the tab elements are removed")
    override fun onTabUnselected(tab: TabLayout.Tab?) {
        val preferences = preferenceScreen.children.toList()
        val tabsPrefIndex = preferences.indexOfFirst { it is ControlsTabPreference }
        for (pref in preferences.subList(tabsPrefIndex + 1, preferences.size)) {
            preferenceScreen.removePreference(pref)
        }
    }

    override fun onTabReselected(tab: TabLayout.Tab?) = Unit

    private fun setDynamicTitle() {
        val dueDateKey = getString(R.string.reschedule_command_key)
        preferenceScreen.allPreferences().filterIsInstance<ControlPreference>().forEach { preference ->
            val title = controlPreferenceTitle(preference.key) ?: return@forEach
            preference.title = title
            if (preference.key == dueDateKey) preference.dialogTitle = title
        }
    }

    private fun setupAnswerCommands() {
        val showAnswerPref = (findPreference<ControlPreference>(R.string.show_answer_command_key) as? ReviewerControlPreference)

        val answerCommandKeys =
            listOf(
                ViewerAction.ANSWER_AGAIN.preferenceKey,
                ViewerAction.ANSWER_HARD.preferenceKey,
                ViewerAction.ANSWER_GOOD.preferenceKey,
                ViewerAction.ANSWER_EASY.preferenceKey,
            )
        for (key in answerCommandKeys) {
            (findPreference<Preference>(key) as? ReviewerControlPreference)?.let { answerPref ->
                answerPref.setOnBindingSelectedListener { binding ->
                    val isAlreadyAssigned =
                        showAnswerPref?.getMappableBindings()?.any {
                            it.binding == binding &&
                                (it.side == CardSide.QUESTION || it.side == CardSide.BOTH)
                        } == true
                    if (isAlreadyAssigned) {
                        answerPref.addBinding(binding, CardSide.ANSWER)
                        return@setOnBindingSelectedListener true
                    }

                    AlertDialog.Builder(requireContext()).show {
                        setTitle(answerPref.title)
                        setIcon(answerPref.icon)
                        setMessage(getString(CommonString.also_assign_binding_to_show_answer, getString(CommonString.show_answer)))

                        positiveButton(text = getString(CommonString.dialog_assign)) {
                            answerPref.addBinding(binding, CardSide.ANSWER)
                            showAnswerPref?.addBinding(binding, CardSide.QUESTION)
                        }
                        negativeButton(text = getString(CommonString.dialog_skip)) {
                            answerPref.addBinding(binding, CardSide.ANSWER)
                        }
                    }
                    true
                }
            }
        }
    }

    companion object {
        private val actionToScreenMap: Map<String, ControlPreferenceScreen> by lazy {
            ControlPreferenceScreen.entries
                .flatMap { screen ->
                    screen.searchableActions().map { action -> action.preferenceKey to screen }
                }.toMap()
        }
    }
}

enum class ControlPreferenceScreen(
    @XmlRes val xmlRes: Int,
) {
    REVIEWER(R.xml.preferences_reviewer_controls),
    PREVIEWER(R.xml.preferences_previewer_controls),
    ;

    fun getActions(): List<MappableAction<*>> =
        when (this) {
            REVIEWER -> ViewerAction.entries
            PREVIEWER -> PreviewerAction.entries
        }
}

internal fun ControlPreferenceScreen.searchableActions(): List<MappableAction<*>> =
    if (this == ControlPreferenceScreen.REVIEWER) getActions() + WhiteboardAction.entries else getActions()

/** The same localized action label is used in its row and in Settings search. */
internal fun Fragment.controlPreferenceTitle(key: String): String? =
    when (key) {
        getString(R.string.browse_command_key) -> TR.qtMiscBrowse()
        getString(R.string.statistics_command_key) -> TR.statisticsTitle()
        getString(R.string.reschedule_command_key) -> TR.sentenceCase.setDueDate
        getString(R.string.bury_card_command_key) -> TR.sentenceCase.buryCard
        getString(R.string.suspend_card_command_key) -> TR.sentenceCase.suspendCard
        getString(R.string.card_info_command_key) -> TR.sentenceCase.cardInfo
        getString(R.string.bury_note_command_key) -> TR.sentenceCase.buryNote
        getString(R.string.suspend_note_command_key) -> TR.sentenceCase.suspendNote
        getString(R.string.mark_command_key), getString(R.string.previewer_mark_key) -> TR.sentenceCase.markNote
        getString(R.string.previous_card_info_command_key) -> TR.sentenceCase.previousCardInfo
        getString(R.string.delete_command_key) -> TR.sentenceCase.deleteNote
        getString(R.string.answer_again_command_key) -> TR.sentenceCase.answerAgain
        getString(R.string.answer_hard_command_key) -> TR.sentenceCase.answerHard
        getString(R.string.answer_good_command_key) -> TR.sentenceCase.answerGood
        getString(R.string.toggle_whiteboard_command_key) ->
            TR.sentenceCase.gestureToggleWhiteboard(getString(CommonString.gesture_toggle_whiteboard))
        getString(R.string.flag_red_command_key) -> TR.sentenceCase.gestureFlagRed(getString(CommonString.gesture_flag_red))
        getString(R.string.flag_orange_command_key) -> TR.sentenceCase.gestureFlagOrange(getString(CommonString.gesture_flag_orange))
        getString(R.string.flag_green_command_key) -> TR.sentenceCase.gestureFlagGreen(getString(CommonString.gesture_flag_green))
        getString(R.string.flag_blue_command_key) -> TR.sentenceCase.gestureFlagBlue(getString(CommonString.gesture_flag_blue))
        getString(R.string.flag_pink_command_key) -> TR.sentenceCase.gestureFlagPink(getString(CommonString.gesture_flag_pink))
        getString(R.string.flag_turquoise_command_key) ->
            TR.sentenceCase.gestureFlagTurquoise(getString(CommonString.gesture_flag_turquoise))
        getString(R.string.flag_purple_command_key) -> TR.sentenceCase.gestureFlagPurple(getString(CommonString.gesture_flag_purple))
        getString(R.string.remove_flag_command_key) -> TR.sentenceCase.gestureFlagRemove(getString(CommonString.gesture_flag_remove))
        else -> null
    }
