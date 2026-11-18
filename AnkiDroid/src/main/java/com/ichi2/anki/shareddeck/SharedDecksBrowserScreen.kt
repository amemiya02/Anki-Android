// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.shareddeck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.workarounds.SafeWebViewLayout
import com.ichi2.compose.theme.foundation
import com.ichi2.anki.common.android.R as CommonR

@Composable
internal fun SharedDecksBrowserScreen(
    webView: SafeWebViewLayout,
    onUp: () -> Unit,
    onHome: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Column(Modifier.fillMaxSize().background(foundation.colors.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onUp) {
                Icon(
                    painterResource(CommonR.drawable.symbol_arrow_back),
                    stringResource(androidx.appcompat.R.string.abc_action_bar_up_description),
                    modifier = Modifier.scale(scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f, scaleY = 1f),
                    tint = foundation.colors.onBackground,
                )
            }
            Text(
                stringResource(CommonString.download_deck),
                modifier = Modifier.weight(1f).semantics { heading() },
                style = foundation.type.pageTitle,
                color = foundation.colors.onBackground,
            )
            IconButton(onClick = onHome) {
                Icon(
                    painterResource(R.drawable.symbol_home),
                    stringResource(CommonString.home),
                    tint = foundation.colors.onBackground,
                )
            }
        }
        AndroidView(factory = { webView }, modifier = Modifier.weight(1f).fillMaxWidth())
    }
}
