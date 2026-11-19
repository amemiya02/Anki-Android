// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.deckpicker

import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.R
import com.ichi2.anki.libanki.DeckId
import com.ichi2.compose.theme.foundation
import java.text.SimpleDateFormat
import java.util.Date
import com.ichi2.anki.common.android.R as CommonR

/** What a person can do on the deck list ([DeckListScreen]) */
interface DeckListActions {
    /** Study the Deck, or open the finished screen when nothing is left in it */
    fun onDeckClick(deckId: DeckId)

    /** The counts of a Deck were tapped: its study options */
    fun onDeckCountsClick(deckId: DeckId)

    /** The Deck's menu: rename, options, export, delete... */
    fun onDeckLongClick(deckId: DeckId)

    /** The Deck's menu at a mouse position, in pixels from the deck list's top-left corner */
    fun onDeckRightClick(
        deckId: DeckId,
        x: Float,
        y: Float,
    )

    fun onToggleExpand(deckId: DeckId)

    fun onGetSharedDecks()

    fun onImport()

    fun onCreateDeck()
}

/**
 * The deck list, as the Core Loop Mockup draws it (`docs/redesign/core-loop/decks-*.png`):
 * the total due today and today's study count over a progress line, then the Deck tree with
 * three columns of counts (new, learning, due). A Newcomer without Decks is told what to do first.
 */
@Composable
fun DeckListScreen(
    state: DeckListScreenState,
    actions: DeckListActions,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val root = modifier.semantics { testTagsAsResourceId = true }
    if (state.isEmptyCollection) {
        EmptyDeckList(
            actions = actions,
            contentPadding = contentPadding,
            modifier = root.testTag("${LocalContext.current.packageName}:id/no_decks_placeholder"),
        )
    } else {
        Decks(state = state, actions = actions, listState = listState, contentPadding = contentPadding, modifier = root)
    }
}

/** The deck list's title: today's date over "Decks". Shown in the top bar on phones. */
@Composable
fun DeckListTitle(
    date: Date,
    modifier: Modifier = Modifier,
) {
    val foundation = MaterialTheme.foundation
    val configuration = LocalConfiguration.current
    val dateText =
        remember(date, configuration) {
            val locale = ConfigurationCompat.getLocales(configuration)[0] ?: java.util.Locale.getDefault()
            SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale).format(date)
        }
    Column(modifier = modifier.padding(top = 6.dp, bottom = 10.dp).semantics(mergeDescendants = true) { heading() }) {
        Text(text = dateText, style = foundation.type.caption, color = foundation.colors.onBackgroundMuted)
        Text(text = TR.actionsDecks(), style = foundation.type.screenTitle, color = foundation.colors.onBackground)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Decks(
    state: DeckListScreenState,
    actions: DeckListActions,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier,
) {
    val columnWidth = countColumnWidth(state.decks)
    LazyColumn(
        state = listState,
        // hands scrolling to the bottom bar's HideViewOnScrollBehavior
        modifier = modifier.fillMaxSize().nestedScroll(rememberNestedScrollInteropConnection()),
        contentPadding = contentPadding,
    ) {
        item(key = "summary") { TodaySummary(dueToday = state.dueToday, studiedToday = state.studiedToday) }
        stickyHeader(key = "headings") { CountHeadings(columnWidth) }
        itemsIndexed(state.decks, key = { _, deck -> deck.did }) { index, deck ->
            // a hairline between top-level Decks; a Deck and its sub-Decks are one group
            if (deck.depth == 0 && index > 0) DeckDivider()
            DeckRow(deck = deck, columnWidth = columnWidth, actions = actions)
        }
    }
}

/** "58 cards to study today", "9 studied so far", and a line showing how far through the day's study is */
@Composable
private fun TodaySummary(
    dueToday: Int,
    studiedToday: Int,
) {
    val foundation = MaterialTheme.foundation
    val margin = foundation.spacing.screenMargin
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = margin, end = margin, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = pluralStringResource(CommonR.plurals.deck_list_due_today, dueToday, dueToday),
                style = foundation.type.summary,
                color = foundation.colors.onBackground,
                modifier = Modifier.weight(1f).alignByBaseline(),
            )
            Text(
                text = pluralStringResource(CommonR.plurals.deck_list_studied_today, studiedToday, studiedToday),
                style = foundation.type.caption,
                color = foundation.colors.onBackgroundMuted,
                textAlign = TextAlign.End,
                modifier = Modifier.alignByBaseline(),
            )
        }
        val total = dueToday + studiedToday
        val done = if (total == 0) 0f else studiedToday.toFloat() / total
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(foundation.colors.progressTrack)
                    // the counts above say it in words
                    .clearAndSetSemantics { },
        ) {
            Box(modifier = Modifier.fillMaxWidth(done).height(3.dp).background(foundation.colors.progressFill))
        }
    }
}

/** New, Learning and Due over the count columns. They stay at the top while the Decks scroll. */
@Composable
private fun CountHeadings(columnWidth: Dp) {
    val foundation = MaterialTheme.foundation
    val margin = foundation.spacing.screenMargin
    Column(modifier = Modifier.fillMaxWidth().background(foundation.colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = margin, end = margin, bottom = 6.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            for (heading in countHeadings()) {
                // the narrow columns would cut a long heading: it shrinks to fit instead
                BasicText(
                    text = heading,
                    style = foundation.type.label.copy(color = foundation.colors.onBackgroundMuted, textAlign = TextAlign.Center),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 4.sp, stepSize = 0.25.sp, maxFontSize = foundation.type.label.fontSize),
                    modifier = Modifier.width(columnWidth),
                )
            }
        }
        DeckDivider()
    }
}

@Composable
private fun countHeadings() = remember { listOf(TR.actionsNew(), TR.schedulingLearning(), TR.decksReviewHeader()) }

@Composable
private fun DeckRow(
    deck: DisplayDeckNode,
    columnWidth: Dp,
    actions: DeckListActions,
) {
    val foundation = MaterialTheme.foundation
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (deck.depth == 0) foundation.spacing.deckRow else foundation.spacing.deckRowNested)
                .testTag("${LocalContext.current.packageName}:id/deck_name")
                .onGloballyPositioned { coordinates = it }
                .pointerInput(deck.did) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Press || !event.buttons.isSecondaryPressed) continue
                            val change = event.changes.first()
                            val inList = coordinates?.localToRoot(change.position) ?: change.position
                            change.consume()
                            actions.onDeckRightClick(deck.did, inList.x, inList.y)
                        }
                    }
                }.combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = stringResource(CommonR.string.deck_list_deck_actions),
                    onLongClick = { actions.onDeckLongClick(deck.did) },
                    onClick = { actions.onDeckClick(deck.did) },
                ).semantics { if (deck.isSelected) selected = true }
                .padding(start = NESTED_INDENT * deck.depth, end = foundation.spacing.screenMargin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeckMarker(deck = deck, onToggle = { actions.onToggleExpand(deck.did) })
        Text(
            text = deck.lastDeckNameComponent,
            style = if (deck.depth == 0) foundation.type.deckName else foundation.type.deckNameNested,
            // a filtered Deck's name is set apart, as the old deck list did
            color = if (deck.filtered) foundation.colors.emphasis else foundation.colors.onBackground,
            // a long name wraps once, then is cut: the counts beside it never move
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(top = 6.dp, bottom = 6.dp, end = 8.dp),
        )
        DeckCounts(deck = deck, columnWidth = columnWidth, onClick = { actions.onDeckCountsClick(deck.did) })
    }
}

/** The chevron of a Deck with sub-Decks; a dot marks the current Deck */
@Composable
private fun DeckMarker(
    deck: DisplayDeckNode,
    onToggle: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val toggle =
        if (deck.canCollapse) {
            val label = stringResource(if (deck.collapsed) CommonR.string.expand else CommonR.string.collapse)
            Modifier.clickable(role = Role.Button, onClick = onToggle).semantics { contentDescription = label }
        } else {
            Modifier
        }
    Box(
        modifier = Modifier.size(foundation.spacing.touchTarget).then(toggle).padding(end = 4.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            when {
                deck.canCollapse ->
                    Icon(
                        painter =
                            painterResource(
                                if (deck.collapsed) CommonR.drawable.symbol_chevron_right else CommonR.drawable.symbol_expand_more,
                            ),
                        contentDescription = null,
                        tint = if (deck.isSelected) foundation.colors.emphasis else foundation.colors.onBackgroundMuted,
                        modifier = Modifier.size(22.dp),
                    )
                deck.isSelected ->
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(foundation.colors.emphasis))
            }
        }
    }
}

@Composable
private fun DeckCounts(
    deck: DisplayDeckNode,
    columnWidth: Dp,
    onClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val clickable =
        Modifier
            .heightIn(min = foundation.spacing.touchTarget)
            .clickable(role = Role.Button, onClick = onClick)
    if (deck.newCount == 0 && deck.lrnCount == 0 && deck.revCount == 0) {
        Row(
            modifier = clickable.width(columnWidth * 3).semantics(mergeDescendants = true) { },
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.symbol_check_circle),
                contentDescription = null,
                tint = foundation.colors.countDue,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(CommonR.string.finished_title_done),
                style = foundation.type.caption,
                color = foundation.colors.countDue,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        return
    }
    val headings = countHeadings()
    Row(modifier = clickable, verticalAlignment = Alignment.CenterVertically) {
        val counts =
            listOf(
                deck.newCount to foundation.colors.countNew,
                deck.lrnCount to foundation.colors.countLearning,
                deck.revCount to foundation.colors.countDue,
            )
        counts.forEachIndexed { index, (count, colour) ->
            val description = stringResource(CommonR.string.deck_list_count_description, headings[index], count)
            Count(count, colour, Modifier.width(columnWidth).semantics { contentDescription = description })
        }
    }
}

/** A count in its column's colour; nothing to do is said with weight, not a faint colour */
@Composable
private fun Count(
    count: Int,
    colour: Color,
    modifier: Modifier,
) {
    val foundation = MaterialTheme.foundation
    Text(
        text = count.toString(),
        style = if (count == 0) foundation.type.count.copy(fontWeight = FontWeight.Normal) else foundation.type.count,
        color = if (count == 0) foundation.colors.onBackgroundMuted else colour,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
    )
}

/** The Mockup's 46dp, widened when the largest count would not fit, so the columns line up */
@Composable
private fun countColumnWidth(decks: List<DisplayDeckNode>): Dp {
    val foundation = MaterialTheme.foundation
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val widest = decks.maxOfOrNull { maxOf(it.newCount, it.lrnCount, it.revCount) } ?: 0
    val style = foundation.type.count
    return remember(widest, style, density) {
        val textWidth =
            with(density) {
                measurer
                    .measure(widest.toString(), style)
                    .size.width
                    .toDp()
            }
        maxOf(foundation.spacing.countColumn, textWidth + 8.dp)
    }
}

@Composable
private fun DeckDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = MaterialTheme.foundation.spacing.screenMargin),
        thickness = MaterialTheme.foundation.shapes.hairline,
        color = MaterialTheme.foundation.colors.outline,
    )
}

/** The deck list for a Newcomer: get shared Decks first, or import a file, or create a Deck */
@Composable
private fun EmptyDeckList(
    actions: DeckListActions,
    contentPadding: PaddingValues,
    modifier: Modifier,
) {
    val foundation = MaterialTheme.foundation
    val margin = foundation.spacing.screenMargin
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(rememberNestedScrollInteropConnection())
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(start = margin, end = margin, top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.hayaki_mark),
            contentDescription = null,
            modifier = Modifier.size(foundation.spacing.emptyMark).offset(x = (-8).dp),
        )
        Text(
            text = stringResource(CommonR.string.deck_list_empty_title),
            style = foundation.type.pageTitle,
            color = foundation.colors.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Button(
            onClick = actions::onGetSharedDecks,
            modifier = Modifier.fillMaxWidth().heightIn(min = foundation.spacing.primaryButton),
            shape = foundation.shapes.button,
            colors = ButtonDefaults.buttonColors(containerColor = foundation.colors.primary, contentColor = foundation.colors.onPrimary),
            elevation = null,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.symbol_download),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp).size(22.dp),
            )
            Text(stringResource(CommonR.string.menu_get_shared_decks), style = foundation.type.button, textAlign = TextAlign.Center)
        }
        Column(modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)) {
            HorizontalDivider(thickness = foundation.shapes.hairline, color = foundation.colors.outline)
            EmptyListRow(
                icon = R.drawable.symbol_file_open,
                title = stringResource(CommonR.string.deck_list_import_file),
                onClick = actions::onImport,
            )
            EmptyListRow(
                icon = R.drawable.symbol_create_new_folder,
                title = stringResource(CommonR.string.deck_list_create_deck),
                onClick = actions::onCreateDeck,
            )
        }
    }
}

@Composable
private fun EmptyListRow(
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = foundation.spacing.listRow)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = foundation.colors.onBackground,
            modifier = Modifier.size(24.dp),
        )
        Text(title, Modifier.weight(1f), style = foundation.type.rowTitle, color = foundation.colors.onBackground)
        Icon(
            painter = painterResource(CommonR.drawable.symbol_chevron_right),
            contentDescription = null,
            tint = foundation.colors.onBackgroundMuted,
            modifier = Modifier.size(20.dp),
        )
    }
    HorizontalDivider(thickness = foundation.shapes.hairline, color = foundation.colors.outline)
}

/** How far a sub-Deck is indented under its parent */
private val NESTED_INDENT = 26.dp
