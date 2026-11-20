// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import android.view.MenuItem
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.children
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation

/** The existing tablet DrawerLayout hosts its navigation controls in Compose. */
internal class TabletDrawer(
    context: Context,
) : AbstractComposeView(context) {
    var onItemSelected: ((MenuItem) -> Boolean)? = null

    private val menu =
        MenuBuilder(context).apply {
            SupportMenuInflater(context).inflate(R.menu.navigation_drawer, this)
            findItem(R.id.nav_decks).title = TR.actionsDecks()
            findItem(R.id.nav_stats).title = TR.statisticsTitle()
        }
    private var selectedId by mutableIntStateOf(-1)

    init {
        id = R.id.navdrawer_items_container
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    fun select(itemId: Int) {
        selectedId = itemId
        menu.children.forEach { it.isChecked = it.itemId == itemId }
    }

    @Composable
    override fun Content() {
        AnkiDroidTheme {
            val foundation = MaterialTheme.foundation
            Column(Modifier.fillMaxSize().background(foundation.colors.background).verticalScroll(rememberScrollState())) {
                Image(
                    painterResource(R.mipmap.hayaki_mark),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp).size(72.dp).testTag("drawer-mark"),
                )
                var previousGroup = 0
                for (item in menu.children) {
                    if (previousGroup != 0 && previousGroup != item.groupId) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = foundation.colors.outline)
                    }
                    previousGroup = item.groupId
                    val active = selectedId == item.itemId
                    val action =
                        if (item.isCheckable) {
                            Modifier.selectable(active, role = Role.Tab) { onItemSelected?.invoke(item) }
                        } else {
                            Modifier.clickable(role = Role.Button) { onItemSelected?.invoke(item) }
                        }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .background(if (active) foundation.colors.progressTrack else foundation.colors.background)
                            .then(action)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item.icon?.let {
                            Icon(
                                it.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                Modifier.size(24.dp),
                                tint = foundation.colors.onBackground,
                            )
                        }
                        Text(item.title.toString(), style = foundation.type.rowTitle, color = foundation.colors.onBackground)
                    }
                }
            }
        }
    }
}
