// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.children
import com.ichi2.anki.deckpicker.SyncIconState
import com.ichi2.compose.theme.foundation
import com.ichi2.ui.AccessibleSearchView
import androidx.appcompat.R as AppCompatR
import com.ichi2.anki.common.android.R as CommonR

/** Retains MenuProvider and keyboard behavior while the visible app bar and overflow use Compose. */
internal class DeckPickerToolbarMenu(
    context: Context,
) : MenuBuilder(context) {
    var version by mutableIntStateOf(0)
        private set
    var search by mutableStateOf<AccessibleSearchView?>(null)
        private set

    override fun onItemsChanged(structureChanged: Boolean) {
        super.onItemsChanged(structureChanged)
        version++
    }

    override fun expandItemActionView(item: MenuItemImpl): Boolean {
        val view = item.actionView as? AccessibleSearchView ?: return false
        search = view
        item.isActionViewExpanded = true
        view.onActionViewExpanded()
        return true
    }

    override fun collapseItemActionView(item: MenuItemImpl): Boolean {
        item.isActionViewExpanded = false
        search?.onActionViewCollapsed()
        search = null
        return true
    }
}

@Composable
internal fun DeckPickerToolbar(
    menu: DeckPickerToolbarMenu?,
    syncState: SyncIconState?,
    syncingMedia: Boolean,
    onNavigate: (() -> Unit)?,
    onAction: (MenuItem) -> Unit,
    title: @Composable () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val iconColors = IconButtonDefaults.iconButtonColors(contentColor = foundation.colors.onBackground)
    var popup by remember(menu) { mutableStateOf<List<MenuItem>?>(null) }
    val version = menu?.version ?: 0
    val items = menu?.children?.filter { it.isVisible }.orEmpty()
    val search = menu?.search
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(foundation.colors.background)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (onNavigate != null || search != null) {
            val closeSearch: () -> Unit = { menu?.findItem(R.id.deck_picker_action_filter)?.collapseActionView() }
            IconButton(onClick = if (search != null) closeSearch else requireNotNull(onNavigate), modifier = Modifier.size(48.dp), colors = iconColors) {
                Icon(
                    painterResource(if (search == null) R.drawable.symbol_menu else CommonR.drawable.symbol_arrow_back),
                    stringResource(if (search == null) CommonString.drawer_open else AppCompatR.string.abc_action_bar_up_description),
                )
            }
        }
        if (search != null) {
            AndroidView(
                factory = {
                    (search.parent as? ViewGroup)?.removeView(search)
                    search
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(Modifier.weight(1f).padding(start = if (onNavigate == null) 16.dp else 4.dp)) { title() }
        }
        val direct =
            items.filter { item ->
                item.itemId == R.id.action_sync ||
                    (search == null && (item as? MenuItemImpl)?.requiresActionButton() == true)
            }
        for (item in direct) {
            IconButton(
                onClick = {
                    if (item.itemId == R.id.deck_picker_action_filter) item.expandActionView() else onAction(item)
                },
                enabled = item.isEnabled,
                modifier = Modifier.size(48.dp),
                colors = iconColors,
            ) {
                if (item.itemId == R.id.action_sync) {
                    val label =
                        when (syncState) {
                            SyncIconState.OneWay -> CommonString.sync_menu_title_one_way_sync
                            SyncIconState.NotLoggedIn -> CommonString.sync_menu_title_no_account
                            else -> CommonString.button_sync
                        }
                    BadgedBox(badge = {
                        when (syncState) {
                            SyncIconState.OneWay -> Badge { Text("!") }
                            SyncIconState.PendingChanges -> Badge(containerColor = foundation.colors.answerHard)
                            else -> Unit
                        }
                    }) {
                        val description = stringResource(label)
                        Icon(painterResource(R.drawable.symbol_sync), description)
                        if (syncingMedia) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else {
                    val icon = remember(item, version) { item.icon?.toBitmap()?.asImageBitmap() }
                    if (icon != null) Icon(icon, item.title.toString(), Modifier.size(24.dp))
                }
            }
        }
        val overflow = items.filterNot { it in direct }.toList()
        if (overflow.isNotEmpty() && search == null) {
            Box {
                IconButton(onClick = { popup = overflow }, modifier = Modifier.size(48.dp), colors = iconColors) {
                    Icon(
                        painterResource(CommonR.drawable.symbol_more_vert),
                        stringResource(AppCompatR.string.abc_action_menu_overflow_description),
                    )
                }
                DropdownMenu(popup != null, onDismissRequest = { popup = null }) {
                    for (item in popup.orEmpty()) {
                        DropdownMenuItem(
                            text = { Text(item.title.toString()) },
                            enabled = item.isEnabled,
                            onClick = {
                                popup =
                                    item.subMenu
                                        ?.children
                                        ?.filter { it.isVisible }
                                        ?.toList()
                                if (!item.hasSubMenu()) onAction(item)
                            },
                        )
                    }
                }
            }
        }
    }
}
