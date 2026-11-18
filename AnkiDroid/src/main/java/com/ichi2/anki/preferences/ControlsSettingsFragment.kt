// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.XmlRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.children
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.analytics.Analytics
import com.ichi2.anki.common.analytics.AnalyticsEvent
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.preferences.reviewer.ViewerAction
import com.ichi2.anki.preferences.reviewer.WhiteboardAction
import com.ichi2.anki.previewer.PreviewerAction
import com.ichi2.anki.reviewer.MappableAction
import com.ichi2.anki.reviewer.MappableBinding.Companion.toPreferenceString
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.openUrl
import com.ichi2.compose.theme.foundation
import com.ichi2.preferences.ControlPreference
import kotlin.math.roundToInt

class ControlsSettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Analytics.sendAnalyticsScreenView("prefs.controls")
        val context = requireContext()
        val prefs = context.sharedPrefs()
        val screens = ControlPreferenceScreen.entries.associateWith { context.controlPreferences(it, ::controlPreferenceTitle) }
        return settingsComposeView(R.id.settings_controls) {
            ControlsSettings(
                preferences = prefs,
                screens = screens,
                searchTarget = settingsSearchTarget,
                onBack = settingsBackAction,
                onShortcuts = { requireActivity().requestShowKeyboardShortcuts() },
                onUserActionsHelp = { context.openUrl(getString(R.string.link_user_actions_help)) },
                onBindingChanged = { (parentFragment as? PreferencesFragment)?.refreshSearchIndex() },
            )
        }
    }
}

/**
 * Inflate the binding definitions as data for the Compose rows and the binding editor. Keeping
 * the original preference tree lets the editor use the same scoped conflict and persistence rules.
 */
private fun Context.controlPreferences(
    tab: ControlPreferenceScreen,
    controlTitle: (String) -> String?,
): PreferenceScreen {
    val manager = PreferenceManager(this)
    val screen = manager.inflateFromResource(this, tab.xmlRes, null)
    manager.setPreferences(screen)
    val prefs = sharedPrefs()
    val actions = tab.searchableActions().associateBy { it.preferenceKey }
    for (pref in screen.allPreferences().filterIsInstance<ControlPreference>()) {
        controlTitle(pref.key)?.let { pref.title = it }
        if (!prefs.contains(pref.key)) pref.defaultBindingValue = actions.getValue(pref.key).getBindings(prefs).toPreferenceString()
    }
    return screen
}

private val basicControls =
    listOf(
        Triple(R.string.gestures_corner_touch_preference, CommonString.gestures_corner_touch, CommonString.gestures_corner_touch_summary),
        Triple(R.string.pref_swipe_sensitivity_key, CommonString.swipe_sensitivity, 0),
        Triple(R.string.pref_keyboard_shortcuts_key, CommonString.show_keyboard_shortcuts, 0),
    )

internal fun Context.controlsSearchEntries(
    path: String,
    controlTitle: (String) -> String?,
): List<SettingsSearchEntry> =
    buildList {
        for ((key, label, summary) in basicControls) {
            if (key == R.string.pref_keyboard_shortcuts_key && resources.configuration.keyboard != Configuration.KEYBOARD_QWERTY) continue
            add(
                SettingsSearchEntry(
                    getString(label),
                    path,
                    ControlsSettingsFragment::class,
                    getString(key),
                    listOfNotNull(summary.takeIf { it != 0 }?.let(::getString)),
                ),
            )
        }
        for (tab in ControlPreferenceScreen.entries) {
            val tabPath = "$path › ${getString(tab.title)}"
            for (setting in controlPreferences(tab, controlTitle).children) {
                if (setting is PreferenceGroup) {
                    val sectionPath = "$tabPath › ${setting.title}"
                    for (action in setting.children.filterIsInstance<ControlPreference>()) {
                        add(action.searchEntry(sectionPath))
                    }
                } else if (setting is ControlPreference) {
                    add(setting.searchEntry(tabPath))
                }
            }
        }
    }

private fun ControlPreference.searchEntry(path: String) =
    SettingsSearchEntry(
        title.toString(),
        path,
        ControlsSettingsFragment::class,
        key,
        getMappableBindings().map(::displayBinding),
    )

@Composable
private fun ControlsSettings(
    preferences: SharedPreferences,
    screens: Map<ControlPreferenceScreen, PreferenceScreen>,
    searchTarget: String?,
    onBack: (() -> Unit)?,
    onShortcuts: () -> Unit,
    onUserActionsHelp: () -> Unit,
    onBindingChanged: () -> Unit,
) {
    val initialTab =
        screens.keys.firstOrNull { tab ->
            searchTarget != null &&
                screens.getValue(tab).findPreference<ControlPreference>(searchTarget) != null
        }
            ?: ControlPreferenceScreen.REVIEWER
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var editorKey by rememberSaveable { mutableStateOf<String?>(null) }
    var revealTarget by rememberSaveable { mutableStateOf(searchTarget) }
    val cornerKey = stringResource(R.string.gestures_corner_touch_preference)
    val swipeKey = stringResource(R.string.pref_swipe_sensitivity_key)
    val shortcutKey = stringResource(R.string.pref_keyboard_shortcuts_key)
    var cornerTouch by remember { mutableStateOf(preferences.getBoolean(cornerKey, false)) }
    var sensitivity by rememberSaveable { mutableIntStateOf(preferences.getInt(swipeKey, 100).coerceIn(20, 180)) }
    var bindingVersion by remember { mutableIntStateOf(0) }
    DisposableEffect(preferences) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == null || key.startsWith("binding_")) bindingVersion++
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val screen = screens.getValue(selectedTab)
    val bindings =
        remember(screen, bindingVersion) {
            screen.allPreferences().filterIsInstance<ControlPreference>().associate { action ->
                action.key to action.getMappableBindings().map(action::displayBinding)
            }
        }

    SettingsScrollPage(stringResource(CommonString.pref_cat_controls), onBack) {
        SettingsDivider()
        SettingsSearchTarget(revealTarget == cornerKey) { modifier, highlighted ->
            SettingsSwitchRow(
                title = stringResource(CommonString.gestures_corner_touch),
                summary = stringResource(CommonString.gestures_corner_touch_summary),
                checked = cornerTouch,
                onChange = {
                    Analytics.send(AnalyticsEvent.SettingTapped(cornerKey))
                    cornerTouch = it
                    preferences.edit { putBoolean(cornerKey, it) }
                },
                modifier = modifier,
                highlighted = highlighted,
            )
        }
        SettingsSearchTarget(revealTarget == swipeKey) { modifier, highlighted ->
            SwipeSensitivityRow(
                sensitivity,
                { sensitivity = it },
                {
                    Analytics.send(AnalyticsEvent.SettingTapped(swipeKey))
                    preferences.edit { putInt(swipeKey, sensitivity) }
                },
                highlighted,
                modifier,
            )
        }

        if (LocalConfiguration.current.keyboard == Configuration.KEYBOARD_QWERTY) {
            SettingsSearchTarget(revealTarget == shortcutKey) { modifier, highlighted ->
                SettingsRow(
                    title = stringResource(CommonString.show_keyboard_shortcuts),
                    modifier = modifier,
                    highlighted = highlighted,
                    onClick = {
                        Analytics.send(AnalyticsEvent.SettingTapped(shortcutKey))
                        onShortcuts()
                    },
                    trailing = { SettingsChevron() },
                )
            }
        }

        ControlTabs(selectedTab) {
            if (selectedTab != it) revealTarget = null
            selectedTab = it
        }
        for (setting in screen.children) {
            if (setting is PreferenceGroup) {
                SettingsSectionTitle(setting.title.toString())
                if (setting is ExtendedPreferenceCategory) {
                    SettingsRow(
                        title = setting.summary.toString(),
                        value = stringResource(CommonString.help),
                        onClick = onUserActionsHelp,
                        trailing = { SettingsChevron() },
                    )
                } else {
                    SettingsDivider()
                }
                for (action in setting.children.filterIsInstance<ControlPreference>()) {
                    ControlActionRow(action, bindings.getValue(action.key), revealTarget) { editorKey = it }
                }
            } else if (setting is ControlPreference) {
                ControlActionRow(setting, bindings.getValue(setting.key), revealTarget) { editorKey = it }
            }
        }
    }

    val edited = editorKey?.let { screen.findPreference<ControlPreference>(it) }
    if (edited != null) {
        ControlBindingEditor(
            edited,
            screen.findPreference(stringResource(R.string.show_answer_command_key)),
            onDismiss = { editorKey = null },
            onChange = onBindingChanged,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeSensitivityRow(
    sensitivity: Int,
    onChange: (Int) -> Unit,
    onSave: () -> Unit,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val foundation = MaterialTheme.foundation
    val title = stringResource(CommonString.swipe_sensitivity)
    Column(
        modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier.border(foundation.shapes.hairline, foundation.colors.emphasis, foundation.shapes.button)
                } else {
                    Modifier
                },
            ).padding(top = 12.dp, bottom = 4.dp),
    ) {
        Text(title, style = foundation.type.rowTitle, color = foundation.colors.onBackground)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val colors = foundation.colors
            val percent = stringResource(CommonString.percentage, "%d".format(sensitivity))
            Slider(
                value = sensitivity.toFloat(),
                onValueChange = { onChange(it.roundToInt()) },
                onValueChangeFinished = onSave,
                modifier =
                    Modifier.weight(1f).height(foundation.spacing.touchTarget).semantics {
                        contentDescription = title
                        stateDescription = percent
                    },
                valueRange = 20f..180f,
                thumb = { Box(Modifier.size(18.dp).background(colors.progressFill, CircleShape)) },
                track = {
                    Box(Modifier.fillMaxWidth().height(3.dp).background(colors.progressTrack)) {
                        Box(
                            Modifier.fillMaxWidth((sensitivity - 20) / 160f).height(3.dp).background(colors.progressFill),
                        )
                    }
                },
            )
            Text(percent, style = foundation.type.body, color = colors.onBackground)
        }
    }
    SettingsDivider()
}

@Composable
private fun ControlTabs(
    selected: ControlPreferenceScreen,
    onSelect: (ControlPreferenceScreen) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        val compact = maxWidth < 310.dp || LocalDensity.current.fontScale > 1.3f
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = foundation.shapes.hairline,
            color = foundation.colors.outline,
        )
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 24.dp),
        ) {
            for (tab in ControlPreferenceScreen.entries) {
                val active = tab == selected
                val width = if (compact) Modifier.weight(1f) else Modifier.width(IntrinsicSize.Max)
                Column(
                    width.selectable(active, role = Role.Tab, onClick = { onSelect(tab) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(tab.title),
                        Modifier.heightIn(min = foundation.spacing.touchTarget).padding(vertical = 12.dp),
                        style = foundation.type.body.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (active) foundation.colors.onBackground else foundation.colors.onBackgroundMuted,
                    )
                    HorizontalDivider(
                        thickness = 2.dp,
                        color = if (active) foundation.colors.emphasis else Color.Transparent,
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlActionRow(
    action: ControlPreference,
    bindings: List<String>,
    searchTarget: String?,
    openEditor: (String) -> Unit,
) {
    val edit = {
        Analytics.send(AnalyticsEvent.SettingTapped(action.key))
        openEditor(action.key)
    }
    SettingsSearchTarget(searchTarget == action.key) { modifier, highlighted ->
        BoxWithConstraints {
            // At large font scales the bindings need the width of the full row.
            val stack = LocalDensity.current.fontScale > 1.3f || maxWidth < 310.dp
            val chipWidth = maxWidth * 0.48f
            val chips: (@Composable () -> Unit)? =
                if (bindings.isEmpty()) null else ({ BindingChips(bindings, Modifier.widthIn(max = chipWidth)) })
            if (stack && bindings.isNotEmpty()) {
                val foundation = MaterialTheme.foundation
                Column(
                    modifier =
                        modifier
                            .fillMaxWidth()
                            .heightIn(min = foundation.spacing.settingsRow)
                            .clickable(role = Role.Button, onClick = edit)
                            .then(
                                if (highlighted) {
                                    Modifier.border(foundation.shapes.hairline, foundation.colors.emphasis, foundation.shapes.button)
                                } else {
                                    Modifier
                                },
                            ).padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(action.title.toString(), style = foundation.type.rowTitle, color = foundation.colors.onBackground)
                    BindingChips(bindings)
                }
                SettingsDivider()
            } else {
                SettingsRow(action.title.toString(), onClick = edit, highlighted = highlighted, modifier = modifier, trailing = chips)
            }
        }
    }
}

@Composable
private fun BindingChips(
    bindings: List<String>,
    modifier: Modifier = Modifier,
) {
    val foundation = MaterialTheme.foundation
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (binding in bindings) {
            Text(
                binding,
                modifier =
                    Modifier
                        .border(foundation.shapes.hairline, foundation.colors.outline, foundation.shapes.button)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                style = foundation.type.caption,
                color = foundation.colors.onBackground,
            )
        }
    }
}

enum class ControlPreferenceScreen(
    @XmlRes val xmlRes: Int,
    val title: Int,
) {
    REVIEWER(R.xml.preferences_reviewer_controls, R.string.settings_study_screen),
    PREVIEWER(R.xml.preferences_previewer_controls, R.string.settings_previewer),
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
