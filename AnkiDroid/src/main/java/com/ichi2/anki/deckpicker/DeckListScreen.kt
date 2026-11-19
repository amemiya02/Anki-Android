// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.deckpicker

import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.R
import com.ichi2.anki.libanki.DeckId
import com.ichi2.compose.theme.foundation
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt
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
 * The deck list in the Material 3 Direction (ADR-0006): today's summary in a primary container,
 * then the Deck tree, each top-level Deck and its open sub-Decks in one surface container, with
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

/** The deck list's title: "Decks" with today's date under it. Shown in the top bar on phones. */
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
        Text(text = TR.actionsDecks(), style = foundation.type.screenTitle, color = foundation.colors.onBackground)
        Text(text = dateText, style = foundation.type.caption, color = foundation.colors.onBackgroundMuted)
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
        // the current Deck, when it has Cards left: the summary offers to study it
        val current = state.decks.firstOrNull { it.isSelected && it.hasCardsLeft }
        item(key = "summary") {
            TodaySummary(
                dueToday = state.dueToday,
                studiedToday = state.studiedToday,
                current = current,
                onStudy = actions::onDeckClick,
            )
        }
        // the headings name count columns: with every Deck done there are none to name
        if (state.decks.any { it.hasCardsLeft }) stickyHeader(key = "headings") { CountHeadings(columnWidth) }
        itemsIndexed(state.decks, key = { _, deck -> deck.did }) { index, deck ->
            // a top-level Deck and its open sub-Decks share one container
            val next = state.decks.getOrNull(index + 1)
            DeckRow(
                deck = deck,
                columnWidth = columnWidth,
                actions = actions,
                position =
                    GroupPosition(
                        isFirst = deck.depth == 0,
                        isLast = next == null || next.depth == 0,
                        isFirstGroup = index == 0,
                    ),
                modifier =
                    Modifier.animateItem(
                        fadeInSpec = tween(EXPAND_MILLIS),
                        placementSpec = tween(EXPAND_MILLIS),
                        fadeOutSpec = tween(EXPAND_MILLIS),
                    ),
            )
        }
        item(key = "end") { Spacer(Modifier.height(GROUP_GAP)) }
    }
}

/**
 * Today at a glance, in a primary container: a ring of how much of the day's study is done beside
 * "58 cards to study today" and "9 studied so far", and a button that studies the current Deck.
 * With nothing left the ring is full and checked. Coming back from a session, the ring moves on
 * from where it last stood.
 */
@Composable
private fun TodaySummary(
    dueToday: Int,
    studiedToday: Int,
    current: DisplayDeckNode?,
    onStudy: (DeckId) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val scheme = MaterialTheme.colorScheme
    val margin = foundation.spacing.screenMargin
    val total = dueToday + studiedToday
    val nothingDue = dueToday == 0
    val done = if (nothingDue) 1f else studiedToday.toFloat() / total
    var lastShown by rememberSaveable { mutableFloatStateOf(0f) }
    val ring = remember { Animatable(lastShown) }
    LaunchedEffect(done) {
        ring.animateTo(done, tween(durationMillis = RING_MILLIS, easing = EmphasizedDecelerate))
        lastShown = done
    }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = margin, end = margin, top = 4.dp, bottom = 16.dp)
                .clip(foundation.shapes.extraLarge)
                .background(scheme.primaryContainer)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            // the counts beside it say it in words
            modifier = Modifier.size(RING_SIZE).clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { ring.value },
                modifier = Modifier.fillMaxSize(),
                color = scheme.primary,
                trackColor = scheme.onPrimaryContainer.copy(alpha = RING_TRACK_ALPHA),
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round,
            )
            if (nothingDue) {
                Icon(
                    painter = painterResource(R.drawable.symbol_check),
                    contentDescription = null,
                    tint = scheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                Text(
                    text = "${(done * 100).roundToInt()}%",
                    style = foundation.type.label,
                    color = scheme.onPrimaryContainer,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text =
                    if (nothingDue) {
                        stringResource(CommonR.string.deck_list_nothing_due)
                    } else {
                        pluralStringResource(CommonR.plurals.deck_list_due_today, dueToday, dueToday)
                    },
                style = foundation.type.summary,
                color = scheme.onPrimaryContainer,
            )
            Text(
                text = pluralStringResource(CommonR.plurals.deck_list_studied_today, studiedToday, studiedToday),
                style = foundation.type.body,
                color = scheme.onPrimaryContainer,
            )
            if (current != null) {
                Button(
                    onClick = { onStudy(current.did) },
                    modifier = Modifier.padding(top = 10.dp).heightIn(min = foundation.spacing.touchTarget),
                    shape = foundation.shapes.button,
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(CommonR.string.studyoptions_start) + " · " + current.lastDeckNameComponent,
                        style = foundation.type.button,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * New, Learning and Due over the count columns. They stay at the top while the Decks scroll.
 * All three share one size: the label size, or smaller when the longest heading would not fit.
 */
@Composable
private fun CountHeadings(columnWidth: Dp) {
    val foundation = MaterialTheme.foundation
    val margin = foundation.spacing.screenMargin
    val headings = countHeadings()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val label = foundation.type.label
    val style =
        remember(headings, columnWidth, label, density) {
            val widest = headings.maxOf { measurer.measure(it, label).size.width }
            val room = with(density) { (columnWidth - HEADING_SIDE_ROOM).toPx() }
            if (widest <= room) label else label.copy(fontSize = label.fontSize * (room / widest))
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(foundation.colors.background)
                .padding(start = margin, end = margin + ROW_END_PADDING, bottom = 6.dp, top = 2.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        for (heading in headings) {
            Text(
                text = heading,
                style = style,
                color = foundation.colors.onBackgroundMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.width(columnWidth),
            )
        }
    }
}

@Composable
private fun countHeadings() = remember { listOf(TR.actionsNew(), TR.schedulingLearning(), TR.decksReviewHeader()) }

/** Where a row sits in its Deck's container, which rounds its outer corners */
private data class GroupPosition(
    val isFirst: Boolean,
    val isLast: Boolean,
    val isFirstGroup: Boolean,
)

@Composable
private fun DeckRow(
    deck: DisplayDeckNode,
    columnWidth: Dp,
    actions: DeckListActions,
    position: GroupPosition,
    modifier: Modifier = Modifier,
) {
    val foundation = MaterialTheme.foundation
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val corner = GROUP_CORNER
    val shape =
        RoundedCornerShape(
            topStart = if (position.isFirst) corner else 0.dp,
            topEnd = if (position.isFirst) corner else 0.dp,
            bottomStart = if (position.isLast) corner else 0.dp,
            bottomEnd = if (position.isLast) corner else 0.dp,
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = foundation.spacing.screenMargin)
                .padding(top = if (position.isFirst && !position.isFirstGroup) GROUP_GAP else 0.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
                .padding(start = NESTED_INDENT * deck.depth, end = ROW_END_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeckMarker(deck = deck, onToggle = { actions.onToggleExpand(deck.did) })
        Text(
            text = deck.lastDeckNameComponent,
            style = if (deck.depth == 0) foundation.type.deckName else foundation.type.deckNameNested,
            // a filtered Deck's name is set apart, as the old deck list did; so is the current Deck
            color = if (deck.filtered || deck.isSelected) foundation.colors.emphasis else foundation.colors.onBackground,
            // a long name wraps once, then is cut: the counts beside it never move
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(top = 8.dp, bottom = 8.dp, end = 8.dp),
        )
        DeckCounts(deck = deck, columnWidth = columnWidth, onClick = { actions.onDeckCountsClick(deck.did) })
    }
}

/** The chevron of a Deck with sub-Decks, turning as it opens; a dot marks the current Deck */
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
    // the chevron points along the reading direction when closed, and down when open
    val open = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -90f else 90f
    val turn by animateFloatAsState(
        targetValue = if (deck.collapsed) 0f else open,
        animationSpec = tween(EXPAND_MILLIS),
        label = "deck chevron",
    )
    Box(
        modifier = Modifier.size(foundation.spacing.touchTarget).then(toggle),
        contentAlignment = Alignment.Center,
    ) {
        when {
            deck.canCollapse ->
                Icon(
                    painter = painterResource(CommonR.drawable.symbol_chevron_right),
                    contentDescription = null,
                    tint = if (deck.isSelected) foundation.colors.emphasis else foundation.colors.onBackgroundMuted,
                    modifier = Modifier.size(24.dp).rotate(turn),
                )
            deck.isSelected ->
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(foundation.colors.emphasis))
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
            .clip(CircleShape)
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
                color = foundation.colors.onBackgroundMuted,
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
            Box(
                modifier = Modifier.width(columnWidth).semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Count(count, colour)
            }
        }
    }
}

/** A count on a tint of its column's colour; nothing to do is a plain muted zero */
@Composable
private fun Count(
    count: Int,
    colour: Color,
) {
    val foundation = MaterialTheme.foundation
    Text(
        text = count.toString(),
        style = if (count == 0) foundation.type.count.copy(fontWeight = FontWeight.Normal) else foundation.type.count,
        color = if (count == 0) foundation.colors.onBackgroundMuted else colour,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        modifier =
            Modifier
                .clip(CircleShape)
                .then(if (count == 0) Modifier else Modifier.background(colour.copy(alpha = COUNT_TINT)))
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Wide enough for the largest count on its pill, so the columns line up */
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
        maxOf(foundation.spacing.countColumn, textWidth + 20.dp)
    }
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                modifier = Modifier.padding(end = 8.dp).size(20.dp),
            )
            Text(stringResource(CommonR.string.menu_get_shared_decks), style = foundation.type.button, textAlign = TextAlign.Center)
        }
        Column(
            modifier =
                Modifier
                    .padding(bottom = 16.dp)
                    .clip(foundation.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = foundation.colors.onBackgroundMuted,
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
}

/** How far a sub-Deck is indented under its parent */
private val NESTED_INDENT = 24.dp

/** The space between Deck containers, and their corners */
private val GROUP_GAP = 8.dp
private val GROUP_CORNER = 16.dp

/** Room between the last count and the container's edge */
private val ROW_END_PADDING = 8.dp

/** The summary ring: its size, its faint track, and how long it takes to move on */
private val RING_SIZE = 64.dp
private const val RING_TRACK_ALPHA = 0.16f
private const val RING_MILLIS = 400

/** Space kept either side of a column heading */
private val HEADING_SIDE_ROOM = 4.dp

/** The tint behind a count: its own colour, faint enough that the count still reads at AA */
private const val COUNT_TINT = 0.10f

/** Opening or closing a Deck: the chevron turns and the sub-Decks settle in this time */
private const val EXPAND_MILLIS = 200

/** Material 3's emphasized decelerate easing, for things arriving at rest */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** Whether any of the Deck's new, learning or due Cards are left today */
private val DisplayDeckNode.hasCardsLeft: Boolean
    get() = newCount > 0 || lrnCount > 0 || revCount > 0
