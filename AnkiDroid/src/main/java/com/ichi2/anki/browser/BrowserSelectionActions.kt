// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ichi2.compose.theme.foundation

@Composable
internal fun BrowserSelectionBar(
    enabled: Boolean,
    actions: List<BrowserSelectionShortcut>,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    Column(Modifier.fillMaxWidth().background(colors.background)) {
        HorizontalDivider(thickness = foundation.shapes.hairline, color = colors.outline)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = if (LocalDensity.current.fontScale >= 1.5f) 3 else 5,
        ) {
            for (item in actions) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = foundation.spacing.touchTarget)
                            .clickable(enabled = enabled, role = Role.Button, onClick = item.action)
                            .semantics { contentDescription = item.description }
                            .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    val color = if (enabled) colors.onBackground else colors.onBackgroundMuted
                    Icon(painterResource(item.icon), null, Modifier.size(22.dp), tint = color)
                    Text(item.label, style = foundation.type.caption, color = color, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

internal data class BrowserSelectionShortcut(
    val label: String,
    val description: String,
    val icon: Int,
    val action: () -> Unit,
)

internal data class BrowserSelectionAction(
    val label: String,
    val icon: Int,
    val detail: String? = null,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val startsGroup: Boolean = false,
    val action: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowserSelectionActionsSheet(
    title: String,
    entries: List<BrowserSelectionAction>,
    onDismiss: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = foundation.shapes.button.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        containerColor = colors.background,
        contentColor = colors.onBackground,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.onBackgroundMuted) },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = foundation.spacing.screenMargin)) {
            Text(
                title,
                Modifier.padding(bottom = 12.dp).semantics { heading() },
                style = foundation.type.pageTitle,
                color = colors.onBackground,
            )
            HorizontalDivider(thickness = foundation.shapes.hairline, color = colors.outline)
            LazyColumn {
                items(entries) { item ->
                    if (item.startsGroup) HorizontalDivider(thickness = foundation.shapes.hairline, color = colors.outline)
                    val color =
                        when {
                            item.destructive -> MaterialTheme.colorScheme.error
                            !item.enabled -> colors.onBackgroundMuted
                            else -> colors.onBackground
                        }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = foundation.spacing.touchTarget)
                            .clickable(enabled = item.enabled, role = Role.Button) {
                                onDismiss()
                                item.action()
                            }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(item.icon),
                            null,
                            Modifier.size(20.dp),
                            tint = if (item.destructive) MaterialTheme.colorScheme.error else colors.onBackgroundMuted,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item.label, style = foundation.type.rowTitle, color = color)
                            item.detail?.let { Text(it, style = foundation.type.caption, color = colors.onBackgroundMuted) }
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}
