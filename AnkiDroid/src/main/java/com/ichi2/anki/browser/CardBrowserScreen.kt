// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    var showSelectionActions by rememberSaveable { mutableStateOf(false) }
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

    fun endSelection() = vm.endMultiSelectMode(SingleSelectCause.NavigateBack)

    val takesOverBottomNavigation = searchOpen || selectionMode
    DisposableEffect(takesOverBottomNavigation, host) {
        host.browserChromeChanged(takesOverBottomNavigation)
        onDispose {
            if (takesOverBottomNavigation) host.browserChromeChanged(false)
        }
    }
    LaunchedEffect(selectionMode) {
        if (!selectionMode) showSelectionActions = false
    }
    BackHandler(enabled = searchOpen || (embedded && selectionMode)) {
        if (searchOpen) closeSearch() else endSelection()
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
            embedded && !takesOverBottomNavigation ->
                Modifier.windowInsetsPadding(
                    WindowInsets.statusBars.union(WindowInsets.displayCutout),
                )
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
            val toolbar: @Composable () -> Unit = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(
                            start = foundation.spacing.screenMargin,
                            end = foundation.spacing.screenMargin,
                            top = 8.dp,
                            bottom = 12.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectionMode) {
                        BrowserIcon(CommonR.drawable.symbol_close, stringResource(CommonString.dialog_cancel), ::endSelection)
                    } else if (!embedded && host.showsStandaloneBack()) {
                        BrowserIcon(
                            CommonR.drawable.symbol_arrow_back,
                            stringResource(CommonString.dialog_cancel),
                            host::browserBack,
                        )
                    }
                    Text(
                        if (selectionMode) {
                            stringResource(R.string.browse_selected_count, selectedCount)
                        } else {
                            TR.qtMiscBrowse()
                        },
                        modifier =
                            Modifier.weight(1f).semantics {
                                heading()
                                if (selectionMode) liveRegion = LiveRegionMode.Polite
                            },
                        style = if (selectionMode) foundation.type.pageTitle else foundation.type.screenTitle,
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
                                entries = host.browserMenu(selectionMode, state, request),
                            )
                        }
                    }
                }
            }
            Column(Modifier.fillMaxSize()) {
                if (selectionMode) toolbar()
                Box(Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().nestedScroll(rememberNestedScrollInteropConnection()),
                        contentPadding = PaddingValues(bottom = if (selectionMode) 0.dp else 88.dp),
                    ) {
                        item {
                            if (!selectionMode) toolbar()
                            BrowserSearchLauncher(
                                text = submittedText,
                                mode = mode,
                                modifier = Modifier.padding(horizontal = foundation.spacing.screenMargin),
                            ) {
                                if (selectionMode) endSelection()
                                host.openSearch()
                            }
                            // the chips scroll edge to edge, starting on the screen margin
                            BrowserChips(
                                filters = displayRequest.filters,
                                flagNames = flagNames,
                                onClick = { sheet = it },
                                contentPadding = PaddingValues(horizontal = foundation.spacing.screenMargin),
                            )
                            Column(Modifier.padding(horizontal = foundation.spacing.screenMargin)) {
                                BrowserResultControls(
                                    state = state,
                                    mode = mode,
                                    sort = sort,
                                    onSort = { sheet = BrowserSheet.SORT },
                                    onMode = {
                                        if (mode != it) {
                                            if (selectionMode) endSelection()
                                            vm.setCardsOrNotes(it)
                                        }
                                    },
                                )
                                HorizontalDivider(color = colors.outline, thickness = foundation.shapes.hairline)
                            }
                        }
                        if (state is SearchState.Completed && results.ids.isEmpty()) {
                            item {
                                Column(Modifier.fillMaxWidth().padding(horizontal = foundation.spacing.screenMargin, vertical = 20.dp)) {
                                    Text(
                                        stringResource(
                                            if (mode ==
                                                CardsOrNotes.CARDS
                                            ) {
                                                R.string.browse_no_cards
                                            } else {
                                                R.string.browse_no_notes
                                            },
                                        ),
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
                                value =
                                    rowCache.get(id)
                                        ?: loadBrowserResultRow(id, results.mode, vm.showMediaFilenames, columns.columns)?.also {
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
                }
                if (selectionMode) {
                    BrowserSelectionBar(
                        enabled = selectedCount > 0,
                        actions =
                            host.selectionShortcuts(
                                onFlag = { sheet = BrowserSheet.SELECTED_FLAG },
                                onMore = { showSelectionActions = true },
                            ),
                    )
                }
            }
            if (!selectionMode) {
                FloatingActionButton(
                    onClick = host::addNote,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(foundation.spacing.addButton),
                    shape = foundation.shapes.addButton,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(painterResource(CommonR.drawable.symbol_add), stringResource(CommonString.menu_add_note))
                }
            }
        }
    }

    if (selectionMode && selectedCount > 0 && showSelectionActions) {
        val countResource = if (mode == CardsOrNotes.CARDS) R.plurals.browse_selection_cards else R.plurals.browse_selection_notes
        BrowserSelectionActionsSheet(
            title = pluralStringResource(countResource, selectedCount, selectedCount),
            entries = host.selectionActions(selectedCount),
            onDismiss = { showSelectionActions = false },
        )
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
                if (searchOpen) {
                    host.replaceSearchDraft(updated)
                } else {
                    if (selectionMode && updated != current) endSelection()
                    search.submitCurrentSearch(updated)
                }
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

/** The result count and the sort, then the Cards and Notes switch at the end */
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // the count and the sort share the start; on a narrow screen the sort wraps under the count
        FlowRow(
            modifier = Modifier.weight(1f),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (state is SearchState.Completed) {
                Text(
                    state.formatCardCount(context.resources),
                    color = colors.onBackgroundMuted,
                    style = foundation.type.body,
                    modifier = Modifier.padding(end = 4.dp),
                )
            } else if (state == SearchState.Searching || state == SearchState.Initializing) {
                LinearProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp).size(width = 54.dp, height = 2.dp),
                    color = colors.progressFill,
                    trackColor = colors.progressTrack,
                )
            }
            val sortLabel = stringResource(R.string.browse_sort)
            TextButton(
                onClick = onSort,
                modifier = Modifier.semantics { contentDescription = sortLabel },
                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
            ) {
                val ordering = sort as? SortChangeNotification.CollectionOrdering
                val icon =
                    when (ordering?.reverse) {
                        true -> R.drawable.symbol_arrow_downward_alt
                        false -> R.drawable.symbol_arrow_upward_alt
                        null -> R.drawable.symbol_sort
                    }
                Icon(painterResource(icon), null, Modifier.size(ButtonDefaults.IconSize), tint = colors.onBackground)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    ordering?.columnLabel ?: sortLabel,
                    color = colors.onBackground,
                    style = foundation.type.button,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        BrowserModeSwitch(mode, onMode)
    }
}

/** Cards or Notes: Material 3's segmented button */
@Composable
internal fun BrowserModeSwitch(
    mode: CardsOrNotes,
    onMode: (CardsOrNotes) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val options = listOf(CardsOrNotes.CARDS to R.string.browse_mode_cards, CardsOrNotes.NOTES to R.string.browse_mode_notes)
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, (option, label) ->
            SegmentedButton(
                selected = mode == option,
                onClick = { onMode(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(stringResource(label), style = foundation.type.button, maxLines = 1) },
            )
        }
    }
}

/**
 * The search's filters as Material 3 filter chips, each opening its choices: Decks, Card state,
 * Flags and Tags. A chip with a choice is selected and names it; a Deck goes by its own name, as
 * the search above already shows its path. The row scrolls sideways, inside [contentPadding].
 */
@Composable
internal fun BrowserChips(
    filters: SearchFilters,
    flagNames: Map<Flag, String>,
    onClick: (BrowserSheet) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val foundation = MaterialTheme.foundation

    @Composable
    fun chip(
        value: List<String>,
        fallback: String,
        sheet: BrowserSheet,
    ) {
        val checked = value.isNotEmpty()
        val text = value.firstOrNull()?.let { first -> if (value.size == 1) first else "$first +${value.size - 1}" } ?: fallback
        FilterChip(
            selected = checked,
            onClick = { onClick(sheet) },
            label = {
                Text(
                    text,
                    style = foundation.type.button,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = CHIP_LABEL_MAX_WIDTH),
                )
            },
            modifier = Modifier.semantics { contentDescription = fallback },
            leadingIcon =
                if (checked) {
                    { Icon(painterResource(R.drawable.symbol_check), null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
            trailingIcon = { Icon(painterResource(R.drawable.symbol_arrow_drop_down), null, Modifier.size(FilterChipDefaults.IconSize)) },
        )
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState())
                .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chip(
            filters.decks.map { it.name.substringAfterLast("::") },
            with(LocalContext.current) { TR.sentenceCase.allDecks },
            BrowserSheet.DECKS,
        )
        chip(filters.cardStates.sortedBy { it.code }.map { it.label }, stringResource(R.string.browse_card_state), BrowserSheet.STATES)
        chip(filters.flags.sortedBy { it.code }.mapNotNull(flagNames::get), TR.browsingSidebarFlags(), BrowserSheet.FLAGS)
        chip(filters.tags, stringResource(CommonString.card_details_tags), BrowserSheet.TAGS)
    }
}

/** A chip's label is cut at this width, so one long choice never hides the chips after it */
private val CHIP_LABEL_MAX_WIDTH = 160.dp

/** Lays the content out [horizontal] wider on each side, over its parent's padding */
internal fun Modifier.bleed(horizontal: Dp): Modifier =
    layout { measurable, constraints ->
        if (!constraints.hasBoundedWidth) {
            val placeable = measurable.measure(constraints)
            return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        val extra = horizontal.roundToPx()
        val wider = constraints.copy(minWidth = constraints.minWidth + extra * 2, maxWidth = constraints.maxWidth + extra * 2)
        val placeable = measurable.measure(wider)
        layout(placeable.width - extra * 2, placeable.height) { placeable.place(-extra, 0) }
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
