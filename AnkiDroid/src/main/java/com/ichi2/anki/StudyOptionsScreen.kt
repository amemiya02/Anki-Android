// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.common.android.themes.interfaceTypeface
import com.ichi2.anki.libanki.Decks
import com.ichi2.anki.ui.internationalization.trCase
import com.ichi2.compose.theme.foundation
import com.ichi2.ui.CollectionMediaImageGetter
import java.io.File

@Composable
internal fun StudyOptionsScreen(
    state: StudyOptionsState,
    scrollState: ScrollState,
    inPane: Boolean,
    mediaDir: File?,
    onStart: () -> Unit,
    onCustomStudy: () -> Unit,
) {
    val paper = MaterialTheme.foundation
    Row(
        Modifier
            .fillMaxSize()
            .background(paper.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .semantics { testTagsAsResourceId = true },
    ) {
        if (inPane) {
            Spacer(
                Modifier
                    .fillMaxHeight()
                    .width(paper.shapes.hairline)
                    .background(paper.colors.outline)
                    .testTag("study_pane_rule"),
            )
        }
        if (state == StudyOptionsState.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Row
        }
        key(state.deckIdOrNull()) {
            Column(Modifier.fillMaxSize().padding(horizontal = paper.spacing.screenMargin, vertical = 12.dp)) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("study_options_scroll")
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val name = Decks.path(state.deckNameOrNull().orEmpty())
                    val abbreviated =
                        when {
                            name.size > 3 -> listOf(name.first(), name[1] + "…", name.last())
                            else -> name.toList()
                        }
                    Text(
                        abbreviated.joinToString("\n"),
                        Modifier.semantics { heading() }.testTag("study_deck_title"),
                        color = paper.colors.onBackground,
                        style = paper.type.pageTitle,
                    )
                    val counts = state.dataOrNull()
                    if (counts != null && (state !is StudyOptionsState.Congrats || state.isDynamic)) {
                        Column(Modifier.fillMaxWidth().testTag("study_counts")) {
                            HorizontalDivider(color = paper.colors.outline)
                            if (counts.hasBuriedCards) {
                                Text(
                                    TR.studyingCountsDiffer(),
                                    style = paper.type.caption,
                                    color = paper.colors.onBackgroundMuted,
                                )
                            }
                            CountRow(TR.actionsNew(), counts.newCardsToday, counts.buriedNew, paper.colors.countNew, "study_new")
                            CountRow(
                                TR.schedulingLearning(),
                                counts.lrnCardsToday,
                                counts.buriedLearning,
                                paper.colors.countLearning,
                                "study_learning",
                            )
                            CountRow(
                                TR.studyingToReview(),
                                counts.revCardsToday,
                                counts.buriedReview,
                                paper.colors.countDue,
                                "study_review",
                            )
                            HorizontalDivider(color = paper.colors.outline)
                            CountRow(
                                stringResource(CommonString.studyoptions_total_new_label),
                                counts.totalNewCards,
                                0,
                                paper.colors.onBackground,
                            )
                            CountRow(
                                stringResource(CommonString.studyoptions_total_label),
                                counts.numberOfCardsInDeck,
                                0,
                                paper.colors.onBackground,
                            )
                        }
                    }
                    val description =
                        if (state.isFiltered()) {
                            stringResource(CommonString.dyn_deck_desc)
                        } else {
                            (state as? StudyOptionsState.StudyOptions)?.deckDescription.orEmpty()
                        }
                    if (description.isNotEmpty()) DeckDescription(description, mediaDir)
                }
                if (state is StudyOptionsState.StudyOptions || (state is StudyOptionsState.Congrats && !state.isDynamic)) {
                    Button(
                        onClick = if (state is StudyOptionsState.StudyOptions) onStart else onCustomStudy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = paper.spacing.primaryButton).testTag("studyoptions_start"),
                        shape = paper.shapes.button,
                    ) {
                        Text(
                            if (state is StudyOptionsState.StudyOptions) {
                                stringResource(CommonString.studyoptions_start)
                            } else {
                                trCase().customStudy()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountRow(
    label: String,
    value: Int,
    buried: Int,
    color: Color,
    tag: String? = null,
) {
    val paper = MaterialTheme.foundation
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, Modifier.weight(1f), style = paper.type.body, color = paper.colors.onBackground)
        Text(value.toString(), if (tag != null) Modifier.testTag(tag) else Modifier, color = color, style = paper.type.rowTitle)
        if (buried > 0) {
            Text(
                pluralStringResource(CommonPlurals.studyoptions_buried_count, buried, buried),
                style = paper.type.caption,
                color = paper.colors.onBackgroundMuted,
            )
        }
    }
}

/** Preserve clickable HTML spans and asynchronously resolved collection images. */
@Composable
private fun DeckDescription(
    html: String,
    mediaDir: File?,
) {
    val paper = MaterialTheme.foundation
    val scope = rememberCoroutineScope()
    key(html, mediaDir) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                TextView(context).apply {
                    id = R.id.studyoptions_deck_description
                    setTextAppearance(com.ichi2.anki.common.android.R.style.TextAppearance_Foundation_Body)
                    typeface = context.interfaceTypeface(paper.type.body.fontSize.value)
                    movementMethod = LinkMovementMethod.getInstance()
                    text =
                        StudyOptionsFragment.formatDescription(html, mediaDir?.let { CollectionMediaImageGetter(context, this, it, scope) })
                }
            },
            update = {
                it.setTextColor(paper.colors.onBackground.toArgb())
                it.setLinkTextColor(paper.colors.emphasis.toArgb())
            },
        )
    }
}
