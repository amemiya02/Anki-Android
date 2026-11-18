// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.previewer

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ichi2.anki.CommonString
import com.ichi2.anki.Flag
import com.ichi2.anki.R
import com.ichi2.anki.browser.flag
import com.ichi2.anki.ui.internationalization.trCase
import com.ichi2.anki.ui.windows.reviewer.StudyBarState
import com.ichi2.anki.ui.windows.reviewer.StudyBarTitles
import com.ichi2.anki.workarounds.SafeWebViewLayout
import com.ichi2.compose.theme.foundation
import kotlin.math.roundToInt
import androidx.appcompat.R as AppCompatR
import com.ichi2.anki.common.android.R as CommonR

@Composable
internal fun PreviewerScreen(
    viewModel: PreviewerViewModel,
    webView: SafeWebViewLayout,
    flagNames: Map<Flag, String>,
    fullBleed: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val index by viewModel.currentIndex.collectAsStateWithLifecycle()
    val deckPath by viewModel.deckPath.collectAsStateWithLifecycle()
    val flag by viewModel.flag.collectAsStateWithLifecycle()
    val isMarked by viewModel.isMarked.collectAsStateWithLifecycle()
    val showingAnswer by viewModel.showingAnswer.collectAsStateWithLifecycle()
    val backSideOnly by viewModel.backSideOnly.collectAsStateWithLifecycle()
    val previousEnabled by viewModel.isBackButtonEnabled.collectAsStateWithLifecycle(initialValue = false)
    val nextEnabled by viewModel.isNextButtonEnabled.collectAsStateWithLifecycle(initialValue = true)
    val cardCount = viewModel.cardsCount()
    val hasPosition = cardCount > 1
    val foundation = MaterialTheme.foundation

    PreviewerLayout(
        webView = webView,
        fullBleed = fullBleed,
        standalone = true,
        sheetTopPadding = if (hasPosition) 0.dp else 14.dp,
        header = {
            PreviewerBar {
                PreviewerBack(onBack)
                Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                    StudyBarTitles(StudyBarState(deckPath = deckPath))
                    if (hasPosition) {
                        Text(
                            text = stringResource(CommonString.preview_progress_bar_text, index + 1, cardCount),
                            modifier = Modifier.padding(horizontal = 6.dp),
                            style = foundation.type.caption,
                            color = foundation.colors.onBackgroundMuted,
                        )
                    }
                }
                FlagMenu(flag, flagNames, viewModel::setFlag)
                PreviewerIcon(
                    icon = R.drawable.symbol_star,
                    label = if (isMarked) stringResource(CommonString.menu_unmark_note) else trCase().markNote(),
                    tint = if (isMarked) foundation.colors.emphasis else foundation.colors.onBackground,
                    onClick = viewModel::toggleMark,
                )
                PreviewerIcon(R.drawable.symbol_edit, stringResource(CommonString.cardeditor_title_edit_card), onEdit)
                PreviewerMenu(backSideOnly, viewModel::toggleBackSideOnly)
            }
            if (hasPosition) {
                PreviewerSlider(index + 1, cardCount, viewModel::onSliderChange)
            }
        },
    ) {
        OutlinedButton(
            onClick = viewModel::onPreviousButtonClick,
            enabled = previousEnabled,
            modifier = Modifier.width(foundation.spacing.primaryButton).heightIn(min = foundation.spacing.primaryButton),
            border = BorderStroke(foundation.shapes.hairline, foundation.colors.outline),
            shape = foundation.shapes.button,
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = foundation.colors.onBackground,
                    disabledContentColor = foundation.colors.onBackground.copy(alpha = 0.38f),
                ),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(painterResource(R.drawable.symbol_chevron_left), stringResource(CommonString.previewer_back))
        }
        PreviewerPrimaryButton(
            label = stringResource(if (showingAnswer || backSideOnly) CommonString.previewer_next else CommonString.show_answer),
            modifier = Modifier.weight(1f),
            enabled = nextEnabled,
            showNextIcon = showingAnswer || backSideOnly,
            onClick = viewModel::onNextButtonClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewerSlider(
    currentPosition: Int,
    cardCount: Int,
    onPositionSelected: (Int) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    var position by remember(currentPosition) { mutableFloatStateOf(currentPosition.toFloat()) }
    val displayedPosition = stringResource(CommonString.preview_progress_bar_text, position.roundToInt(), cardCount)
    Slider(
        value = position,
        onValueChange = { position = it },
        onValueChangeFinished = {
            val chosen = position.roundToInt()
            if (chosen != currentPosition) onPositionSelected(chosen)
        },
        valueRange = 1f..cardCount.toFloat(),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = foundation.spacing.studyMargin)
                .height(foundation.spacing.touchTarget)
                .systemGestureExclusion()
                .semantics { stateDescription = displayedPosition },
        thumb = {
            Box(Modifier.size(15.dp).background(foundation.colors.progressFill, CircleShape))
        },
        track = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(foundation.colors.progressTrack),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((position - 1f) / (cardCount - 1))
                        .height(3.dp)
                        .background(foundation.colors.progressFill),
                )
            }
        },
    )
}

@Composable
private fun FlagMenu(
    flag: Flag,
    flagNames: Map<Flag, String>,
    onFlag: (Flag) -> Unit,
) {
    val colors = MaterialTheme.foundation.colors
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        PreviewerIcon(
            icon = flag.drawableRes,
            label = trCase().flagCard(),
            tint = if (flag == Flag.NONE) colors.onBackground else colors.flag(flag),
            state = flagNames[flag],
            onClick = { menuOpen = true },
        )
        PreviewerDropdown(menuOpen, onDismiss = { menuOpen = false }) {
            flagNames.forEach { (entry, name) ->
                DropdownMenuItem(
                    text = { PreviewerMenuText(name) },
                    leadingIcon = { Icon(painterResource(entry.drawableRes), null, tint = colors.flag(entry)) },
                    trailingIcon = {
                        if (entry == flag) Icon(painterResource(R.drawable.symbol_check), null, tint = colors.emphasis)
                    },
                    onClick = {
                        menuOpen = false
                        onFlag(entry)
                    },
                )
            }
        }
    }
}

@Composable
private fun PreviewerMenu(
    backSideOnly: Boolean,
    onToggleBackSide: () -> Unit,
) {
    val colors = MaterialTheme.foundation.colors
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        PreviewerIcon(
            CommonR.drawable.symbol_more_vert,
            stringResource(AppCompatR.string.abc_action_menu_overflow_description),
            onClick = { menuOpen = true },
        )
        PreviewerDropdown(menuOpen, onDismiss = { menuOpen = false }) {
            DropdownMenuItem(
                text = { PreviewerMenuText(stringResource(R.string.previewer_show_back_only)) },
                trailingIcon = {
                    if (backSideOnly) Icon(painterResource(R.drawable.symbol_check), null, tint = colors.emphasis)
                },
                modifier = Modifier.semantics { toggleableState = ToggleableState(backSideOnly) },
                onClick = {
                    menuOpen = false
                    onToggleBackSide()
                },
            )
        }
    }
}

@Composable
private fun PreviewerDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val foundation = MaterialTheme.foundation
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = foundation.colors.background,
        shape = foundation.shapes.button,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(foundation.shapes.hairline, foundation.colors.outline),
        content = content,
    )
}

@Composable
private fun PreviewerMenuText(label: String) {
    val foundation = MaterialTheme.foundation
    Text(label, style = foundation.type.body, color = foundation.colors.onBackground)
}

@Composable
internal fun TemplatePreviewerScreen(
    webView: SafeWebViewLayout,
    titles: List<String>,
    selectedTab: Int,
    showingAnswer: Boolean,
    standalone: Boolean,
    fullBleed: Boolean,
    minimumButtonHeight: Dp,
    onSelect: (Int) -> Unit,
    onToggleSide: () -> Unit,
    onBack: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    PreviewerLayout(
        webView = webView,
        fullBleed = fullBleed,
        standalone = standalone,
        sheetTopPadding = 14.dp,
        header = {
            if (standalone) {
                PreviewerBar {
                    PreviewerBack(onBack)
                    Text(
                        stringResource(CommonString.card_editor_preview_card),
                        modifier = Modifier.padding(start = 6.dp),
                        style = foundation.type.studyTitle,
                        color = foundation.colors.onBackground,
                    )
                }
            }
            if (selectedTab in titles.indices) {
                PreviewerTabs(titles, selectedTab, onSelect)
            }
        },
    ) {
        PreviewerPrimaryButton(
            label = stringResource(if (showingAnswer) R.string.previewer_show_question else CommonString.show_answer),
            modifier = Modifier.fillMaxWidth(),
            minimumHeight = minimumButtonHeight,
            onClick = onToggleSide,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewerTabs(
    titles: List<String>,
    selectedTab: Int,
    onSelect: (Int) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = foundation.spacing.studyMargin,
        minTabWidth = foundation.spacing.touchTarget,
        containerColor = foundation.colors.background,
        divider = { HorizontalDivider(thickness = foundation.shapes.hairline, color = foundation.colors.outline) },
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTab, matchContentSize = true),
                height = foundation.shapes.currentTabRule,
                color = foundation.colors.emphasis,
            )
        },
    ) {
        titles.forEachIndexed { index, name ->
            val selected = index == selectedTab
            Tab(
                selected = selected,
                onClick = { onSelect(index) },
                modifier = Modifier.heightIn(min = foundation.spacing.touchTarget),
            ) {
                Text(
                    text = name,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = foundation.type.body,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) foundation.colors.onBackground else foundation.colors.onBackgroundMuted,
                )
            }
        }
    }
}

@Composable
private fun PreviewerLayout(
    webView: SafeWebViewLayout,
    fullBleed: Boolean,
    standalone: Boolean,
    sheetTopPadding: Dp,
    header: @Composable () -> Unit = {},
    controls: @Composable RowScope.() -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val insetModifier = if (standalone) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier
    Column(
        Modifier
            .fillMaxSize()
            .background(foundation.colors.background)
            .then(insetModifier),
    ) {
        header()
        val frame =
            if (fullBleed) {
                Modifier
            } else {
                Modifier
                    .padding(horizontal = foundation.spacing.studyMargin)
                    .border(foundation.shapes.hairline, foundation.colors.outline, foundation.shapes.cardSheet)
                    .clip(foundation.shapes.cardSheet)
            }
        AndroidView(
            factory = { webView },
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = sheetTopPadding)
                    .then(frame),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = foundation.spacing.studyMargin)
                    .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = controls,
        )
    }
}

@Composable
private fun PreviewerBar(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun PreviewerBack(onBack: () -> Unit) {
    PreviewerIcon(CommonR.drawable.symbol_arrow_back, stringResource(AppCompatR.string.abc_action_bar_up_description), onBack)
}

@Composable
private fun PreviewerIcon(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.foundation.colors.onBackground,
    state: String? = null,
) {
    IconButton(
        onClick = onClick,
        modifier =
            Modifier
                .size(MaterialTheme.foundation.spacing.touchTarget)
                .semantics { if (state != null) stateDescription = state },
    ) {
        Icon(painterResource(icon), contentDescription = label, tint = tint)
    }
}

@Composable
private fun PreviewerPrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showNextIcon: Boolean = false,
    minimumHeight: Dp = MaterialTheme.foundation.spacing.primaryButton,
    onClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = minimumHeight),
        // Reveal and Next share a location. Each action starts with its own press state.
        interactionSource = remember(label) { MutableInteractionSource() },
        shape = foundation.shapes.button,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = foundation.colors.primary,
                contentColor = foundation.colors.onPrimary,
            ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, style = foundation.type.button, textAlign = TextAlign.Center)
        if (showNextIcon) {
            Spacer(Modifier.width(6.dp))
            Icon(painterResource(CommonR.drawable.symbol_chevron_right), contentDescription = null)
        }
    }
}
