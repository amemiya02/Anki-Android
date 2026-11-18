// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.filtered

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.preferences.SettingsChevron
import com.ichi2.anki.preferences.SettingsPage
import com.ichi2.anki.preferences.SettingsRow
import com.ichi2.anki.preferences.SettingsSectionTitle
import com.ichi2.anki.preferences.SettingsSwitchRow
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogMark
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import java.util.Locale
import com.ichi2.anki.common.android.R as CommonR

@Composable
internal fun FilteredDeckOptionsScreen(
    state: FilteredDeckOptionsState,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onName: (String) -> Unit,
    onSearch: (FilterIndex, String) -> Unit,
    onSearchInBrowser: (FilterIndex) -> Unit,
    onLimit: (FilterIndex, String) -> Unit,
    onOrder: (FilterIndex, Int) -> Unit,
    onSecondFilter: (Boolean) -> Unit,
    onReschedule: (Boolean) -> Unit,
    onDelay: (RescheduleDelay, String) -> Unit,
    onAllowEmpty: (Boolean) -> Unit,
    onExcludedCards: () -> Unit,
    onBuild: () -> Unit,
    onErrorClosed: () -> Unit,
    onExit: () -> Unit,
) {
    val spacing = MaterialTheme.foundation.spacing
    SettingsPage((state as? FilteredDeckOptions)?.title ?: TR.actionsOptions(), onBack) {
        when (state) {
            is Initializing -> {
                if (state.throwable == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
            DeckBuilt -> Unit
            is FilteredDeckOptions -> {
                var orderFilter by rememberSaveable { mutableStateOf<FilterIndex?>(null) }
                val orderLabel = TR.decksCardsSelectedBy().replaceFirstChar { it.titlecase(Locale.getDefault()) }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val nameError =
                        when (state.nameInputError) {
                            FilteredNameInputError.Empty -> stringResource(CommonString.empty_cram_label)
                            FilteredNameInputError.AlreadyExists -> stringResource(CommonString.error_name_exists)
                            null -> null
                        }
                    FilteredDeckTextField(state.name, onName, TR.deckConfigNamePrompt(), error = nameError)

                    SettingsSectionTitle(TR.actionsFilter())
                    SearchTermFields(
                        term = state.filter1State,
                        orderLabel = orderLabel,
                        order = state.cardOptions.getOrNull(state.filter1State.index)?.label,
                        onSearch = { onSearch(FilterIndex.First, it) },
                        onSearchInBrowser = { onSearchInBrowser(FilterIndex.First) },
                        onLimit = { onLimit(FilterIndex.First, it) },
                        onChooseOrder = { orderFilter = FilterIndex.First },
                    )
                    SettingsSwitchRow(TR.decksEnableSecondFilter(), state.isSecondFilterEnabled, onSecondFilter)
                    if (state.isSecondFilterEnabled) {
                        state.filter2State?.let { second ->
                            SearchTermFields(
                                term = second,
                                orderLabel = orderLabel,
                                order = state.cardOptions.getOrNull(second.index)?.label,
                                onSearch = { onSearch(FilterIndex.Second, it) },
                                onSearchInBrowser = { onSearchInBrowser(FilterIndex.Second) },
                                onLimit = { onLimit(FilterIndex.Second, it) },
                                onChooseOrder = { orderFilter = FilterIndex.Second },
                            )
                        }
                    }
                    SettingsSectionTitle(TR.actionsOptions())
                    SettingsSwitchRow(TR.decksRescheduleCardsBasedOnMyAnswers(), state.shouldReschedule, onReschedule)
                    if (!state.shouldReschedule) {
                        PreviewDelay(state.delayAgain, CommonString.filtered_option_delay_again) { onDelay(RescheduleDelay.Again, it) }
                        PreviewDelay(state.delayHard, CommonString.filtered_option_delay_hard) { onDelay(RescheduleDelay.Hard, it) }
                        PreviewDelay(state.delayGood, CommonString.filtered_option_delay_good) { onDelay(RescheduleDelay.Good, it) }
                        Text(TR.decksZeroMinutesHint(), style = MaterialTheme.foundation.type.caption)
                    }
                    SettingsSwitchRow(TR.decksCreateEvenIfEmpty(), state.allowEmpty, onAllowEmpty)
                    SettingsRow(TR.decksUnmovableCards(), onClick = onExcludedCards, trailing = { SettingsChevron() })
                    TextButton(onClick = onHelp, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                        Text(stringResource(CommonString.help))
                    }
                }
                if (state.isBuildingBrowserSearch) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                Button(
                    onClick = onBuild,
                    enabled = state.isBuildingAllowed,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = spacing.touchTarget),
                ) {
                    Text(if (state.id == null) TR.decksBuild() else TR.actionsRebuild())
                }
                orderFilter?.let { index ->
                    SingleChoiceDialog(
                        title = orderLabel,
                        choices = state.cardOptions.map { DialogChoice(it.label) },
                        selectedIndex = if (index == FilterIndex.First) state.filter1State.index else state.filter2State?.index,
                        onSelect = {
                            onOrder(index, it)
                            orderFilter = null
                        },
                        onDismissRequest = { orderFilter = null },
                    )
                }
            }
        }
    }
    when (state) {
        is Initializing ->
            state.throwable?.let {
                PaperDialog(
                    title = stringResource(CommonString.import_title_error),
                    onDismissRequest = onExit,
                    mark = DialogMark(R.drawable.symbol_warning, warning = true),
                    actions = listOf(DialogAction(stringResource(CommonString.dialog_exit), onExit, DialogActionStyle.Filled)),
                    properties =
                        DialogProperties(
                            dismissOnBackPress = false,
                            dismissOnClickOutside = false,
                            usePlatformDefaultWidth = false,
                        ),
                ) {
                    DialogBody(it.toString())
                }
            }
        is FilteredDeckOptions ->
            state.throwable?.let {
                ErrorDialog(
                    title = stringResource(CommonString.import_title_error),
                    message = it.message.orEmpty(),
                    onDismissRequest = onErrorClosed,
                    dismissText = null,
                    primaryAction = DialogAction(stringResource(CommonString.dialog_ok), onErrorClosed),
                )
            }
        DeckBuilt -> Unit
    }
}

@Composable
private fun SearchTermFields(
    term: SearchTermState,
    orderLabel: String,
    order: String?,
    onSearch: (String) -> Unit,
    onSearchInBrowser: () -> Unit,
    onLimit: (String) -> Unit,
    onChooseOrder: () -> Unit,
) {
    FilteredDeckTextField(term.search, onSearch, TR.actionsSearch(), trailingIcon = {
        IconButton(onClick = onSearchInBrowser) {
            Icon(painterResource(CommonR.drawable.symbol_search), stringResource(CommonString.card_browser_cram_search))
        }
    })
    val numberError =
        when (term.error) {
            SearchInputError.Empty -> stringResource(CommonString.empty_cram_label)
            SearchInputError.NotANumber -> TR.errorsInvalidInputEmpty()
            null -> null
        }
    FilteredDeckTextField(
        term.limit,
        { onLimit(it.take(5)) },
        TR.decksLimitTo(),
        numeric = true,
        error = numberError,
    )
    SettingsRow(orderLabel, value = order, onClick = onChooseOrder, trailing = { SettingsChevron() })
}

@Composable
private fun PreviewDelay(
    value: String,
    label: Int,
    onValue: (String) -> Unit,
) {
    FilteredDeckTextField(
        value,
        onValue,
        stringResource(label),
        numeric = true,
        suffix = TR.schedulingSeconds(),
        error = if (value.toIntOrNull() == null) TR.errorsInvalidInputEmpty() else null,
    )
}

@Composable
private fun FilteredDeckTextField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    numeric: Boolean = false,
    error: String? = null,
    suffix: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        trailingIcon = trailingIcon,
    )
}
