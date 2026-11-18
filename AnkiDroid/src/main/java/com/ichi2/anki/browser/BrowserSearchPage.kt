// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.Flag
import com.ichi2.anki.R
import com.ichi2.anki.browser.search.CardBrowserSearchViewModel
import com.ichi2.anki.browser.search.SavedSearch
import com.ichi2.anki.browser.search.SearchFilters
import com.ichi2.anki.browser.search.SearchRequest
import com.ichi2.anki.browser.search.from
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.compose.theme.foundation
import com.ichi2.anki.common.android.R as CommonR

@Composable
private fun searchHint(mode: CardsOrNotes): String =
    stringResource(if (mode == CardsOrNotes.CARDS) R.string.browse_search_cards else R.string.browse_search_notes)

@Composable
internal fun BrowserSearchLauncher(
    text: String,
    mode: CardsOrNotes,
    open: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    val hint = searchHint(mode)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = foundation.spacing.touchTarget)
                .clip(foundation.shapes.button)
                .border(1.dp, colors.onBackgroundMuted, foundation.shapes.button)
                .semantics { contentDescription = hint }
                .clickable(role = Role.Button, onClick = open)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(CommonR.drawable.symbol_search), contentDescription = null, tint = colors.onBackgroundMuted)
        Text(
            text.ifBlank { hint },
            style = foundation.type.lead,
            color = if (text.isBlank()) colors.onBackgroundMuted else colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun BrowserSearchPage(
    text: String,
    mode: CardsOrNotes,
    filters: SearchFilters,
    flagNames: Map<Flag, String>,
    search: CardBrowserSearchViewModel,
    onClose: () -> Unit,
    onChip: (BrowserSheet) -> Unit,
    onSubmit: () -> Unit,
    onSaved: (SavedSearch) -> Unit,
    onRecent: (SearchRequest) -> Unit,
    onManage: () -> Unit,
    onManual: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val history by search.displayedSearchHistoryFlow.collectAsState()
    val canExpandHistory by search.showHistoryToggleFlow.collectAsState(initial = false)
    val historyExpanded by search.isHistoryExpandedFlow.collectAsState()
    val savedSearches by search.savedSearchesFlow.collectAsState()
    val hint = searchHint(mode)
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = foundation.spacing.screenMargin)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BrowserIcon(CommonR.drawable.symbol_arrow_back, stringResource(CommonString.dialog_cancel), onClose)
            OutlinedTextField(
                value = text,
                onValueChange = search::updateBrowserDraft,
                modifier = Modifier.weight(1f).focusRequester(focus).semantics { contentDescription = hint },
                placeholder = { Text(hint, style = foundation.type.lead) },
                textStyle = foundation.type.lead,
                singleLine = true,
                shape = foundation.shapes.button,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.emphasis,
                        unfocusedBorderColor = colors.onBackgroundMuted,
                        cursorColor = colors.emphasis,
                    ),
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        BrowserIcon(CommonR.drawable.symbol_close, stringResource(R.string.browse_clear)) { search.updateBrowserDraft("") }
                    }
                },
            )
            BrowserIcon(CommonR.drawable.symbol_search, stringResource(R.string.browse_hide_keyboard), onSubmit)
        }
        BrowserChips(filters, flagNames) {
            keyboard?.hide()
            onChip(it)
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            if (history.isNotEmpty()) {
                item {
                    BrowserSearchSection(
                        label = stringResource(R.string.browse_recent_searches),
                        action =
                            if (canExpandHistory) {
                                stringResource(
                                    if (historyExpanded) CommonString.card_browser_see_less else CommonString.card_browser_see_more,
                                )
                            } else {
                                null
                            },
                        onAction = search::toggleHistoryExpanded,
                    )
                }
                items(history.entryToSearchString) { (entry, rendered) ->
                    val query = rendered?.value ?: entry.query
                    val request by androidx.compose.runtime.produceState<SearchRequest?>(null, entry) {
                        value = SearchFilters.from(entry)?.let { SearchRequest(entry.query, it) }
                    }
                    BrowserSearchItem(
                        icon = R.drawable.symbol_history,
                        title = query,
                        onClick = { request?.let(onRecent) },
                        accessory = {
                            BrowserIcon(R.drawable.symbol_arrow_insert, stringResource(R.string.browse_history_edit)) {
                                search.updateBrowserDraft(query)
                                focus.requestFocus()
                                keyboard?.show()
                            }
                        },
                    )
                }
            }
            if (savedSearches.isNotEmpty()) {
                item {
                    BrowserSearchSection(
                        stringResource(R.string.browse_saved_searches),
                        stringResource(R.string.browse_manage_searches),
                        onManage,
                    )
                }
                items(savedSearches, key = { it.name }) { saved ->
                    BrowserSearchItem(
                        icon = R.drawable.symbol_star,
                        title = saved.name,
                        summary = saved.query,
                        onClick = { onSaved(saved) },
                    )
                }
            }
            item {
                Column(Modifier.padding(top = 20.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = colors.outline)
                    BrowserSearchItem(
                        icon = CommonR.drawable.symbol_help,
                        title = stringResource(R.string.browse_search_manual),
                        summary = stringResource(R.string.browse_search_manual_description),
                        onClick = onManual,
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserSearchSection(
    label: String,
    action: String?,
    onAction: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Column(Modifier.padding(top = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, Modifier.weight(1f), color = foundation.colors.onBackgroundMuted, style = foundation.type.caption)
            if (action != null) {
                TextButton(onAction) { Text(action, style = foundation.type.caption, color = foundation.colors.emphasis) }
            }
        }
        HorizontalDivider(color = foundation.colors.outline)
    }
}

@Composable
private fun BrowserSearchItem(
    icon: Int,
    title: String,
    onClick: () -> Unit,
    summary: String? = null,
    accessory: @Composable (() -> Unit)? = null,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onClick)
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = colors.onBackgroundMuted, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = foundation.type.rowTitle, color = colors.onBackground)
                if (summary != null) {
                    Text(summary, style = foundation.type.caption, color = colors.onBackgroundMuted)
                }
            }
        }
        accessory?.invoke()
    }
    HorizontalDivider(color = colors.outline)
}
