// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import anki.search.BrowserColumns.Sorting
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.Flag
import com.ichi2.anki.R
import com.ichi2.anki.browser.search.CardState
import com.ichi2.anki.browser.search.SearchFilters
import com.ichi2.anki.libanki.DeckNameId
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.anki.model.SortType
import com.ichi2.compose.theme.foundation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowserChoiceSheet(
    sheet: BrowserSheet,
    filters: SearchFilters,
    flagNames: Map<Flag, String>,
    vm: CardBrowserViewModel,
    onApply: (SearchFilters) -> Unit,
    onDismiss: () -> Unit,
    onFlag: (Flag) -> Unit,
    onRenameFlags: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    // Resolve dynamic choices before the sheet measures its expanded anchor.
    val decks by produceState<List<DeckNameId>?>(null, sheet) {
        if (sheet == BrowserSheet.DECKS) value = withCol { decks.allNamesAndIds() }
    }
    val tags by produceState<List<String>?>(null, sheet) {
        if (sheet == BrowserSheet.TAGS) value = withCol { tags.all() }
    }
    if ((sheet == BrowserSheet.DECKS && decks == null) || (sheet == BrowserSheet.TAGS && tags == null)) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = foundation.shapes.button.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        containerColor = colors.background,
        contentColor = colors.onBackground,
        tonalElevation = 0.dp,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = foundation.spacing.screenMargin, vertical = 10.dp)) {
            when (sheet) {
                BrowserSheet.DECKS -> {
                    BrowserFilterChoices(
                        title = TR.actionsDecks(),
                        labels = decks.orEmpty().associateWith { it.name.replace("::", " › ") },
                        current = filters.decks,
                        onDone = { onApply(filters.copy(decks = it)) },
                    )
                }
                BrowserSheet.STATES ->
                    BrowserFilterChoices(
                        title = stringResource(R.string.browse_card_state),
                        labels = CardState.entries.associateWith { it.label },
                        current = filters.cardStates,
                        onDone = { onApply(filters.copy(cardStates = it)) },
                    )
                BrowserSheet.TAGS -> {
                    BrowserFilterChoices(
                        title = stringResource(CommonString.card_details_tags),
                        labels = (tags.orEmpty() + filters.tags).associateWith { it },
                        current = filters.tags,
                        onDone = { onApply(filters.copy(tags = it)) },
                    )
                }
                BrowserSheet.FLAGS ->
                    BrowserFilterChoices(
                        title = TR.browsingSidebarFlags(),
                        labels = Flag.entries.mapNotNull { flag -> flagNames[flag]?.let { flag to it } }.toMap(),
                        current = filters.flags,
                        onDone = { onApply(filters.copy(flags = it)) },
                        mark = { FlagMark(it) },
                        footer = {
                            TextButton(onClick = onRenameFlags) {
                                Text(stringResource(CommonString.rename_flag), style = foundation.type.button, color = colors.emphasis)
                            }
                        },
                    )
                BrowserSheet.SORT -> BrowserSortChoices(vm, onDismiss)
                BrowserSheet.SELECTED_FLAG -> {
                    Text(TR.browsingSidebarFlags(), style = foundation.type.pageTitle)
                    LazyColumn {
                        items(Flag.entries.toList()) { flag ->
                            flagNames[flag]?.let { label ->
                                TextButton(onClick = { onFlag(flag) }) {
                                    FlagMark(flag)
                                    Text(label, Modifier.padding(start = 12.dp), style = foundation.type.body)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagMark(flag: Flag) {
    Icon(
        painterResource(if (flag == Flag.NONE) R.drawable.symbol_flag else R.drawable.symbol_flag_red),
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.foundation.colors.flag(flag),
    )
}

@Composable
private fun <T> BrowserFilterChoices(
    title: String,
    labels: Map<T, String>,
    current: List<T>,
    onDone: (List<T>) -> Unit,
    mark: @Composable (T) -> Unit = {},
    footer: @Composable () -> Unit = {},
) {
    var selected by rememberSaveable(current) { mutableStateOf(current.map { it.toString() }) }
    BrowserSheetHeading(title, onDone = { onDone(labels.keys.filter { it.toString() in selected }) })
    LazyColumn(Modifier.weightInSheet()) {
        items(labels.entries.toList()) { (item, label) ->
            BrowserCheckChoice(
                label,
                item.toString() in selected,
                onToggle = { checked -> selected = if (checked) selected + item.toString() else selected - item.toString() },
                mark = { mark(item) },
            )
        }
    }
    footer()
}

@Composable
private fun BrowserSortChoices(
    vm: CardBrowserViewModel,
    onDismiss: () -> Unit,
) {
    val mode = vm.cardsOrNotes
    val active by produceState<SortType?>(null, mode) { value = SortType.build(mode) }
    var chosen by remember(active) { mutableStateOf(active) }
    val sortable =
        vm.allColumns
            .filter {
                val sorting = if (mode == CardsOrNotes.CARDS) it.sortingCards else it.sortingNotes
                sorting != Sorting.SORTING_NONE
            }.sortedBy { it.getLabel(mode) }
            .sortedBy { column ->
                vm.activeColumns.indexOfFirst { it.ankiColumnKey == column.key }.takeIf { it >= 0 } ?: Int.MAX_VALUE
            }.map { BrowserColumnKey.from(it) to it.getLabel(mode) }
    BrowserSheetHeading(
        stringResource(R.string.browse_sort),
        onDone = {
            chosen?.let { vm.setSortType(it) }
            onDismiss()
        },
    )
    LazyColumn(Modifier.weightInSheet().selectableGroup()) {
        item {
            BrowserRadioChoice(stringResource(CommonString.card_browser_order_no_sorting_title), chosen == SortType.NoOrdering) {
                chosen = SortType.NoOrdering
            }
        }
        items(sortable) { (key, label) ->
            val ordering = chosen as? SortType.CollectionOrdering
            BrowserRadioChoice(label, ordering?.key == key) {
                chosen = SortType.CollectionOrdering(key, ordering?.reverse ?: false)
            }
        }
        val ordering = chosen as? SortType.CollectionOrdering
        if (ordering != null) {
            item {
                Text(stringResource(R.string.browse_sort_direction), style = MaterialTheme.foundation.type.rowTitle)
                BrowserRadioChoice(stringResource(CommonString.sort_pill_a11y_ascending), !ordering.reverse) {
                    chosen = ordering.copy(reverse = false)
                }
                BrowserRadioChoice(stringResource(CommonString.sort_pill_a11y_descending), ordering.reverse) {
                    chosen = ordering.copy(reverse = true)
                }
            }
        }
    }
}

@Composable
private fun BrowserSheetHeading(
    title: String,
    onDone: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = foundation.type.pageTitle, color = foundation.colors.onBackground)
        TextButton(onDone) {
            Text(stringResource(R.string.browse_done), style = foundation.type.button, color = foundation.colors.emphasis)
        }
    }
    HorizontalDivider(color = foundation.colors.outline)
}

@Composable
private fun BrowserCheckChoice(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    mark: @Composable () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(
                min = 48.dp,
            ).toggleable(checked, role = Role.Checkbox, onValueChange = onToggle)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = foundation.colors.emphasis, checkmarkColor = foundation.colors.background),
        )
        mark()
        Text(label, style = foundation.type.body)
    }
    HorizontalDivider(color = foundation.colors.outline)
}

@Composable
private fun BrowserRadioChoice(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(
                min = 48.dp,
            ).selectable(checked, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(checked, onClick = null)
        Text(label, style = foundation.type.body)
    }
    HorizontalDivider(color = foundation.colors.outline)
}

private fun Modifier.weightInSheet(): Modifier = heightIn(max = 420.dp).fillMaxWidth()
