// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.permissions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.preferences.SettingsPage
import com.ichi2.anki.preferences.SettingsSectionTitle
import com.ichi2.anki.preferences.SettingsSwitchRow
import com.ichi2.compose.theme.foundation

@Composable
internal fun PermissionsScreen(
    title: String,
    rows: List<PermissionRowState>,
    showGroupLabels: Boolean,
    onBack: (() -> Unit)?,
    onContinue: (() -> Unit)?,
) {
    val spacing = MaterialTheme.foundation.spacing
    SettingsPage(title, onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
            rows.groupBy { it.request.optional }.forEach { (optional, group) ->
                if (showGroupLabels) {
                    val label = if (optional) CommonString.heading_optional_permissions else CommonString.heading_required_permissions
                    SettingsSectionTitle(stringResource(label))
                }
                group.forEach { row ->
                    val request = row.request
                    SettingsSwitchRow(
                        title = stringResource(request.title),
                        summary = stringResource(request.summary),
                        icon = request.icon,
                        checked = row.granted,
                        onChange = { request.onClick(row.granted) },
                    )
                }
            }
        }
        if (onContinue != null) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = spacing.touchTarget),
                enabled = rows.isNotEmpty() && rows.all { it.granted },
            ) {
                Text(stringResource(CommonString.dialog_continue))
            }
        }
    }
}
