// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ichi2.anki.BottomNavController.NavigationItem
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation

/**
 * Material 3's navigation bar, as a Compose view placed by the existing nested-scroll behavior and
 * system-inset adapter.
 */
internal class DeckBottomBar(
    context: Context,
) : AbstractComposeView(context) {
    var selectedTab by mutableStateOf(NavigationItem.HOME)
    var onTabClick: (NavigationItem) -> Unit = {}

    val selectedItemId: Int get() = selectedTab.id

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    @Composable
    override fun Content() {
        AnkiDroidTheme {
            val foundation = MaterialTheme.foundation
            val scheme = MaterialTheme.colorScheme
            Row(Modifier.fillMaxWidth().background(scheme.surfaceContainer).selectableGroup()) {
                NavigationItem.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    // the pill grows out from the icon's centre as the destination becomes current
                    val shown by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = tween(INDICATOR_MILLIS),
                        label = "navigation indicator",
                    )
                    // as in Material 3's bar, a press or focus shows on the pill, never across the label
                    val interactions = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = dimensionResource(R.dimen.bottom_bar_height))
                            .testTag("tab-${tab.tag}")
                            .selectable(selected, interactionSource = interactions, indication = null, role = Role.Tab) {
                                onTabClick(tab)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            Modifier.padding(horizontal = 3.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(
                                        width = dimensionResource(R.dimen.bottom_bar_indicator_width),
                                        height = dimensionResource(R.dimen.bottom_bar_indicator_height),
                                    ).clip(CircleShape)
                                    .indication(interactions, ripple()),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .width(dimensionResource(R.dimen.bottom_bar_indicator_width) * shown)
                                        .clip(CircleShape)
                                        .background(scheme.secondaryContainer),
                                )
                                Icon(
                                    painterResource(tab.icon),
                                    contentDescription = null,
                                    Modifier.size(24.dp),
                                    tint = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = tab.title(context),
                                style = foundation.type.label,
                                color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.Bold else foundation.type.label.fontWeight,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** How long the indicator takes to grow behind a newly current destination */
private const val INDICATOR_MILLIS = 200
