// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** A Compose bar placed by the existing nested-scroll behavior and system-inset adapter. */
internal class DeckBottomBar(
    context: Context,
) : AbstractComposeView(context) {
    var selectedTab by mutableStateOf(NavigationItem.HOME)
    var onTabClick: (NavigationItem) -> Unit = {}

    var selectedItemId: Int
        get() = selectedTab.id
        set(id) {
            NavigationItem.fromId(id)?.let(onTabClick)
        }

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    @Composable
    override fun Content() {
        AnkiDroidTheme {
            val foundation = MaterialTheme.foundation
            Column(Modifier.fillMaxWidth().background(foundation.colors.background)) {
                Box(Modifier.fillMaxWidth().height(foundation.shapes.hairline).background(foundation.colors.outline))
                Row(Modifier.fillMaxWidth().selectableGroup()) {
                    NavigationItem.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        val color = if (selected) foundation.colors.onBackground else foundation.colors.onBackgroundMuted
                        Box(
                            Modifier
                                .weight(1f)
                                .heightIn(min = dimensionResource(R.dimen.bottom_bar_height) - foundation.shapes.hairline)
                                .testTag("tab-${tab.tag}")
                                .selectable(selected, role = Role.Tab) { onTabClick(tab) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .width(dimensionResource(R.dimen.bottom_bar_current_rule_width))
                                        .height(2.dp)
                                        .background(foundation.colors.emphasis),
                                )
                            }
                            Column(
                                Modifier.padding(horizontal = 3.dp, vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(painterResource(tab.icon), contentDescription = null, Modifier.size(24.dp), tint = color)
                                Text(
                                    text = tab.title(context),
                                    style = foundation.type.caption,
                                    color = color,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
