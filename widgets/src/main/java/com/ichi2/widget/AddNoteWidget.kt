// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.destinations.DeferredNavigation
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.destinations.toIntent
import com.ichi2.anki.widgets.R
import com.ichi2.anki.common.android.R as CommonR

class AddNoteWidget : AnalyticsWidgetProvider() {
    override val requiresCollection = false
    override val glanceAppWidget: PaperWidget<*> get() = AddNote

    companion object {
        fun refresh(context: Context) = AddNote.refresh(context)
    }
}

private object AddNote : PaperWidget<Unit>(AddNoteWidget::class.java) {
    override suspend fun load(
        context: Context,
        id: AppWidgetId,
    ) = Unit

    @Composable
    @GlanceComposable
    override fun Content(
        appearance: WidgetAppearance,
        data: Unit,
    ) {
        val colors = appearance.colors
        val compact = LocalSize.current.height < 66.dp || LocalSize.current.width < 66.dp
        val tiny = LocalSize.current.height < 48.dp || LocalSize.current.width < 48.dp
        val iconBox =
            if (tiny) {
                16.dp
            } else if (compact) {
                20.dp
            } else {
                36.dp
            }
        val strings = appearance.localizedContext.resources
        val label = strings.getString(CommonString.menu_add_note)
        val intent = with(DeferredNavigation) { NoteEditorDestination.AddNote().toIntent() }
        WidgetSheet(appearance, actionStartActivity(intent)) {
            Column(
                GlanceModifier.fillMaxSize().padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    GlanceModifier
                        .size(iconBox)
                        .paperBackground(R.drawable.widget_action, colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        ImageProvider(CommonR.drawable.symbol_add),
                        contentDescription = null,
                        modifier =
                            GlanceModifier.size(
                                if (tiny) {
                                    13.dp
                                } else if (compact) {
                                    16.dp
                                } else {
                                    24.dp
                                },
                            ),
                        colorFilter = ColorFilter.tint(ColorProvider(colors.onPrimary)),
                    )
                }
                Spacer(GlanceModifier.height(if (compact) 1.dp else 4.dp))
                WidgetText(
                    if (tiny) strings.getString(R.string.widget_add_note_compact) else label,
                    colors.onBackground,
                    appearance.textSize(
                        if (tiny) {
                            8
                        } else if (compact) {
                            9
                        } else {
                            12
                        },
                        if (tiny) {
                            8
                        } else if (compact) {
                            11
                        } else {
                            18
                        },
                    ),
                    GlanceModifier.semantics { contentDescription = label },
                )
            }
        }
    }
}
