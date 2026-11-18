// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.Flag
import com.ichi2.anki.R
import com.ichi2.anki.browser.CardBrowserViewModel.ChangeMultiSelectMode.SingleSelectCause
import com.ichi2.anki.browser.CardBrowserViewModel.RowSelection
import com.ichi2.anki.browser.CardBrowserViewModel.SearchState
import com.ichi2.anki.browser.search.SearchFilters
import com.ichi2.anki.browser.search.SearchRequest
import com.ichi2.anki.browser.search.populateChips
import com.ichi2.anki.formatCardCount
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.compose.theme.foundation
import kotlinx.coroutines.delay
import com.ichi2.anki.common.android.R as CommonR

internal enum class BrowserSheet { DECKS, STATES, FLAGS, TAGS, SORT, SELECTED_FLAG }

@Composable
internal fun CardBrowserScreen(
    host: CardBrowserFragment,
    embedded: Boolean,
) {
    val vm = host.activityViewModel
    val search = host.searchViewModel
    val context = LocalContext.current
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    val keyboard = LocalSoftwareKeyboardController.current
    val searchOpen by search.isScreenOpenFlow.collectAsState()
    val query by search.searchTextFlow.collectAsState()
    val request by vm.searchRequestFlow.collectAsState()
    val state by vm.renderedSearchState.collectAsState()
    val results by vm.renderedResults.collectAsState()
    val mode by vm.flowOfCardsOrNotes.collectAsState()
    val selection by vm.flowOfMultiSelectModeChanged.collectAsState()
    var selectionRevision by remember { mutableIntStateOf(0) }
    val selectionMode = selection.resultedInMultiSelect
    val selectedCount = remember(selectionMode, selectionRevision) { vm.selectedRowCount() }
    val paneRow by vm.flowOfPaneRow.collectAsState()
    val columns by vm.flowOfActiveColumns.collectAsState()
    val truncated by vm.flowOfIsTruncated.collectAsState()
    val sort by vm.flowOfCurrentSort.collectAsState()
    val listState = rememberLazyListState()
    var sheet by rememberSaveable { mutableStateOf<BrowserSheet?>(null) }
    LaunchedEffect(host) { host.sheetRequests.collect { sheet = it } }
    var menu by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    val rowCache =
        remember(results, columns, revision) {
            object : LruCache<CardOrNoteId, BrowserResultRow>(256 * 1024) {
                override fun sizeOf(
                    key: CardOrNoteId,
                    value: BrowserResultRow,
                ): Int = (value.title.length + value.details.length) * 2 + 128
            }
        }
    val flagNames by produceState<Map<Flag, String>>(emptyMap(), host.flagNameRevision.collectAsState().value) {
        value = Flag.queryDisplayNames(context)
    }
    val displayRequest by produceState(request, request) {
        value = withCol { request.populateChips() }
    }
    val submittedText by produceState(request.query, request) {
        value = if (request.isEmpty) "" else withCol { request.toSearchString() }.getOrNull()?.value ?: request.query
    }
    val draftRequest by produceState(SearchRequest(query), query, searchOpen) {
        if (searchOpen) {
            delay(100)
            value = withCol { SearchRequest(query).populateChips() }
        }
    }

    fun closeSearch() {
        keyboard?.hide()
        search.closeBrowserSearch(request)
    }

    DisposableEffect(searchOpen, host) {
        host.searchVisibilityChanged(searchOpen)
        onDispose {
            if (searchOpen) host.searchVisibilityChanged(false)
        }
    }
    BackHandler(enabled = searchOpen || (embedded && selectionMode)) {
        if (searchOpen) closeSearch() else vm.endMultiSelectMode(SingleSelectCause.NavigateBack)
    }

    LaunchedEffect(vm) {
        vm.flowOfCardStateChanged.collect { revision++ }
    }
    LaunchedEffect(vm) {
        vm.flowOfSelectedRows.collect { selectionRevision++ }
    }
    LaunchedEffect(vm, listState) {
        vm.flowOfScrollRequest.collect { selection ->
            vm.getPositionOfId(selection.rowId)?.let { listState.scrollToItem(it + 1, -selection.topOffset.coerceAtLeast(0)) }
        }
    }
    LaunchedEffect(request, mode, sort) {
        if (!vm.isInMultiSelectMode && vm.cardIdToBeScrolledTo == null) listState.requestScrollToItem(0)
    }

    val insets =
        when {
            vm.isFragmented ->
                Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                )
            embedded && !searchOpen -> Modifier.windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
            else -> Modifier.windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
        }
    Box(Modifier.fillMaxSize().background(colors.background).then(insets)) {
        if (searchOpen) {
            BrowserSearchPage(
                text = query,
                mode = mode,
                filters = draftRequest.filters,
                flagNames = flagNames,
                search = search,
                onClose = ::closeSearch,
                onChip = { sheet = it },
                onSubmit = {
                    keyboard?.hide()
                    search.submitBrowserSearch(query)
                },
                onSaved = {
                    keyboard?.hide()
                    search.submitBrowserSearch(it.query)
                },
                onRecent = { request ->
                    keyboard?.hide()
                    search.closeBrowserSearch(request)
                    search.submitCurrentSearch(request)
                },
                onManage = host::showSavedSearches,
                onManual = host::openSearchManual,
            )
        } else {
            val selectedSelection: (CardOrNoteId, Int) -> RowSelection = { id, index ->
                RowSelection(
                    id,
                    listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == index }
                        ?.offset ?: 0,
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().nestedScroll(rememberNestedScrollInteropConnection()),
                contentPadding =
                    PaddingValues(
                        start = foundation.spacing.screenMargin,
                        end = foundation.spacing.screenMargin,
                        bottom = 88.dp,
                    ),
            ) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selectionMode) {
                                BrowserIcon(CommonR.drawable.symbol_arrow_back, stringResource(CommonString.dialog_cancel)) {
                                    vm.endMultiSelectMode(SingleSelectCause.NavigateBack)
                                }
                            } else if (!embedded && !vm.isFragmented) {
                                BrowserIcon(
                                    CommonR.drawable.symbol_arrow_back,
                                    stringResource(CommonString.dialog_cancel),
                                    host::browserBack,
                                )
                            }
                            Text(
                                if (selectionMode) selectedCount.toString() else TR.qtMiscBrowse(),
                                modifier = Modifier.weight(1f).semantics { heading() },
                                style = foundation.type.screenTitle,
                                color = colors.onBackground,
                            )
                            if (selectionMode) {
                                BrowserIcon(R.drawable.symbol_select_all, stringResource(CommonString.card_browser_select_all)) {
                                    vm.selectAll()
                                }
                            }
                            Box {
                                BrowserIcon(CommonR.drawable.symbol_more_vert, stringResource(R.string.browse_more_actions)) { menu = true }
                                if (menu) {
                                    BrowserMenu(
                                        expanded = true,
                                        onDismiss = { menu = false },
                                        entries = host.browserMenu(selectionMode, selectedCount, state, request) { sheet = it },
                                    )
                                }
                            }
                        }
                        if (!selectionMode) {
                            BrowserSearchLauncher(submittedText, mode, host::openSearch)
                            BrowserChips(displayRequest.filters, flagNames, { sheet = it })
                            BrowserResultControls(
                                state = state,
                                mode = mode,
                                sort = sort,
                                onSort = { sheet = BrowserSheet.SORT },
                                onMode = { vm.setCardsOrNotes(it) },
                            )
                        }
                        HorizontalDivider(color = colors.outline, thickness = foundation.shapes.hairline)
                    }
                }
                if (state is SearchState.Completed && results.ids.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                            Text(
                                stringResource(if (mode == CardsOrNotes.CARDS) R.string.browse_no_cards else R.string.browse_no_notes),
                                style = foundation.type.rowTitle,
                                color = colors.onBackground,
                            )
                            if (request.query.isNotBlank() || request.filters.activeFilters.isNotEmpty()) {
                                TextButton(onClick = { search.submitCurrentSearch(SearchRequest("")) }) {
                                    Text(stringResource(R.string.browse_clear), style = foundation.type.button)
                                }
                            }
                        }
                    }
                }
                items(
                    count = results.ids.size,
                    key = { "${results.mode}:${results.ids[it].cardOrNoteId}" },
                ) { index ->
                    val id = results.ids[index]
                    val row by produceState(rowCache.get(id), id, rowCache) {
                        value = rowCache.get(id) ?: loadBrowserResultRow(id, results.mode, vm.showMediaFilenames)?.also {
                            rowCache.put(id, it)
                        }
                    }
                    BrowserResult(
                        row = row,
                        flagNames = flagNames,
                        isSelected = remember(id, selectionMode, selectionRevision) { selectionMode && id in vm.selectedRows },
                        isInSelection = selectionMode,
                        isInPane = id == paneRow,
                        truncated = truncated,
                        fontScale = vm.rowFontScale,
                        onClick = { vm.onTap(selectedSelection(id, index + 1)) },
                        onLongClick = { vm.handleRowLongPress(selectedSelection(id, index + 1)) },
                        onRightClick = { vm.handleRightClick(selectedSelection(id, index + 1)) },
                    )
                }
            }
            BrowserScrollbar(listState, results.ids.size, Modifier.align(Alignment.CenterEnd))
            if (!selectionMode) {
                FloatingActionButton(
                    onClick = host::addNote,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(foundation.spacing.addButton),
                    shape = foundation.shapes.addButton,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ) {
                    Icon(painterResource(CommonR.drawable.symbol_add), stringResource(CommonString.menu_add_note))
                }
            }
        }
    }

    sheet?.let { active ->
        val current = if (searchOpen) draftRequest else displayRequest
        BrowserChoiceSheet(
            sheet = active,
            filters = current.filters,
            flagNames = flagNames,
            vm = vm,
            onDismiss = { sheet = null },
            onApply = { filters ->
                sheet = null
                val updated = current.copy(filters = filters)
                if (searchOpen) host.replaceSearchDraft(updated) else search.submitCurrentSearch(updated)
            },
            onFlag = {
                host.updateFlagForSelectedRows(it)
                sheet = null
            },
            onRenameFlags = {
                sheet = null
                host.renameFlags()
            },
        )
    }
}

@Composable
private fun BrowserResultControls(
    state: SearchState,
    mode: CardsOrNotes,
    sort: SortChangeNotification?,
    onSort: () -> Unit,
    onMode: (CardsOrNotes) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    val context = LocalContext.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (state is SearchState.Completed) {
            Text(state.formatCardCount(context.resources), color = colors.onBackgroundMuted, style = foundation.type.caption)
        } else if (state == SearchState.Searching || state == SearchState.Initializing) {
            LinearProgressIndicator(
                modifier = Modifier.size(width = 54.dp, height = 2.dp),
                color = colors.progressFill,
                trackColor = colors.progressTrack,
            )
        } else {
            Spacer(Modifier.height(1.dp))
        }
        val sortLabel = stringResource(R.string.browse_sort)
        TextButton(onClick = onSort, modifier = Modifier.semantics { contentDescription = sortLabel }) {
            val ordering = sort as? SortChangeNotification.CollectionOrdering
            val icon =
                when (ordering?.reverse) {
                    true -> R.drawable.symbol_arrow_downward_alt
                    false -> R.drawable.symbol_arrow_upward_alt
                    null -> R.drawable.symbol_sort
                }
            Icon(painterResource(icon), null, Modifier.size(20.dp), tint = colors.onBackground)
            Text(
                ordering?.columnLabel ?: sortLabel,
                color = colors.onBackground,
                style = foundation.type.caption,
            )
        }
        Row(Modifier.clip(foundation.shapes.button).border(1.dp, colors.onBackgroundMuted, foundation.shapes.button)) {
            for ((option, label) in listOf(
                CardsOrNotes.CARDS to R.string.browse_mode_cards,
                CardsOrNotes.NOTES to R.string.browse_mode_notes,
            )) {
                val active = mode == option
                Text(
                    text = stringResource(label),
                    style = foundation.type.caption,
                    color = if (active) colors.onBackground else colors.onBackgroundMuted,
                    modifier =
                        Modifier
                            .background(if (active) colors.progressTrack else colors.background)
                            .heightIn(min = foundation.spacing.touchTarget)
                            .semantics { selected = active }
                            .clickable(role = Role.Tab) { onMode(option) }
                            .padding(horizontal = 11.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
internal fun BrowserChips(
    filters: SearchFilters,
    flagNames: Map<Flag, String>,
    onClick: (BrowserSheet) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors

    @Composable
    fun chip(
        value: List<String>,
        fallback: String,
        sheet: BrowserSheet,
    ) {
        val checked = value.isNotEmpty()
        val text = value.firstOrNull()?.let { first -> if (value.size == 1) first else "$first +${value.size - 1}" } ?: fallback
        Row(
            modifier =
                Modifier
                    .heightIn(min = foundation.spacing.touchTarget)
                    .clip(foundation.shapes.button)
                    .background(if (checked) colors.progressTrack else colors.background)
                    .border(1.dp, colors.outline, foundation.shapes.button)
                    .semantics {
                        contentDescription = fallback
                        selected = checked
                    }.clickable(role = Role.Button) { onClick(sheet) }
                    .padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (checked) {
                Icon(
                    painterResource(R.drawable.symbol_check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.onBackground,
                )
            }
            Text(text, style = foundation.type.body, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onBackground)
            Icon(
                painterResource(R.drawable.symbol_arrow_drop_down),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.onBackgroundMuted,
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 5.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chip(
            filters.decks.map { it.name.replace("::", " › ") },
            with(LocalContext.current) { TR.sentenceCase.allDecks },
            BrowserSheet.DECKS,
        )
        chip(filters.cardStates.sortedBy { it.code }.map { it.label }, stringResource(R.string.browse_card_state), BrowserSheet.STATES)
        chip(filters.flags.sortedBy { it.code }.mapNotNull(flagNames::get), TR.browsingSidebarFlags(), BrowserSheet.FLAGS)
        chip(filters.tags, stringResource(CommonString.card_details_tags), BrowserSheet.TAGS)
    }
}

@Composable
internal fun BrowserIcon(
    icon: Int,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(modifier = Modifier.size(MaterialTheme.foundation.spacing.touchTarget), onClick = onClick) {
        Icon(painterResource(icon), description, tint = MaterialTheme.foundation.colors.onBackground)
    }
}

internal data class BrowserMenuEntry(
    val label: String,
    val action: () -> Unit,
)

@Composable
private fun BrowserMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    entries: List<BrowserMenuEntry>,
) {
    val foundation = MaterialTheme.foundation
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = foundation.shapes.button,
        shadowElevation = 0.dp,
        containerColor = foundation.colors.background,
    ) {
        entries.forEach { item ->
            DropdownMenuItem(
                text = { Text(item.label, style = foundation.type.body, color = foundation.colors.onBackground) },
                onClick = {
                    onDismiss()
                    item.action()
                },
            )
        }
    }
}
