// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.account

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.preferences.SettingsPage
import com.ichi2.compose.theme.foundation

@Composable
internal fun SyncStepPage(
    title: String,
    step: Int,
    button: String,
    canContinue: Boolean,
    bottomCornerClearancePx: Int,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsPage(
        title = title,
        onBack = onBack,
        modifier = Modifier.windowInsetsPadding(WindowInsets(bottom = bottomCornerClearancePx)),
        titleBelowNavigation = true,
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SyncStepIndicator(step)
            content()
        }
        SyncPrimaryButton(button, onContinue, Modifier.padding(bottom = 12.dp), enabled = canContinue)
    }
}

@Composable
private fun SyncStepIndicator(step: Int) {
    val foundation = MaterialTheme.foundation
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(2) { index ->
            Box(
                Modifier
                    .size(width = 18.dp, height = 4.dp)
                    .background(
                        if (index < step) foundation.colors.onBackground else foundation.colors.outline,
                        CircleShape,
                    ),
            )
        }
        Text(
            stringResource(CommonString.paper_sync_step, step),
            Modifier.padding(start = 4.dp),
            style = foundation.type.caption,
            color = foundation.colors.onBackgroundMuted,
        )
    }
}

@Composable
internal fun SyncPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    val foundation = MaterialTheme.foundation
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = enabled,
        shape = foundation.shapes.button,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = foundation.colors.primary,
                contentColor = foundation.colors.onPrimary,
            ),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
            Box(Modifier.width(8.dp))
        }
        Text(label, style = foundation.type.rowTitle)
    }
}
