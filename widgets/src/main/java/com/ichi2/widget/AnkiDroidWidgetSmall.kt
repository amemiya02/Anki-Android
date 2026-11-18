// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import com.ichi2.anki.CommonPlurals
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.destinations.DeferredNavigation
import com.ichi2.anki.common.destinations.LauncherDestination
import com.ichi2.anki.common.destinations.toIntent
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.widgets.R

/** A home-screen shortcut with the due count and the estimated time to study. */
class AnkiDroidWidgetSmall : AnalyticsWidgetProvider() {
    override val glanceAppWidget: PaperWidget<*> get() = DueCount

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.sharedPrefs().edit { putBoolean("widgetSmallEnabled", true) }
        DayRolloverAlarm.scheduleNext(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.sharedPrefs().edit { putBoolean("widgetSmallEnabled", false) }
    }

    override fun onReadyToUpdate(context: Context) = WidgetStatus.updateInBackground(context)

    companion object {
        fun refresh(context: Context) = DueCount.refresh(context)
    }
}

private object DueCount : PaperWidget<SmallWidgetStatus>(AnkiDroidWidgetSmall::class.java) {
    override suspend fun load(
        context: Context,
        id: AppWidgetId,
    ): SmallWidgetStatus = WidgetStatus.fetchSmall()

    @Composable
    @GlanceComposable
    override fun Content(
        appearance: WidgetAppearance,
        data: SmallWidgetStatus,
    ) {
        val strings = appearance.localizedContext.resources
        val colors = appearance.colors
        val compact = LocalSize.current.height < 66.dp || LocalSize.current.width < 66.dp
        val tiny = LocalSize.current.height < 48.dp || LocalSize.current.width < 48.dp
        val count = appearance.number(data.dueCardsCount)
        val countSize =
            when {
                tiny -> 13
                compact -> 20
                else -> 28
            }
        val labelSize =
            appearance.textSize(
                if (tiny) {
                    8
                } else if (compact) {
                    9
                } else {
                    12
                },
                if (tiny) 8 else 15,
            )
        val intent = with(DeferredNavigation) { LauncherDestination.toIntent() }
        WidgetSheet(appearance, actionStartActivity(intent)) {
            Column(
                GlanceModifier.fillMaxSize().padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WidgetText(
                    count,
                    colors.countDue,
                    appearance.textSize(countSize, minOf(countSize, (LocalSize.current.width.value * 1.4f / count.length).toInt())),
                    modifier =
                        GlanceModifier.semantics {
                            contentDescription =
                                if (data.dueCardsCount == 0) {
                                    strings.getString(CommonString.finished_title_done)
                                } else {
                                    strings.getQuantityString(CommonPlurals.widget_cards_due, data.dueCardsCount, data.dueCardsCount)
                                }
                        },
                    weight = FontWeight.Bold,
                )
                WidgetText(strings.getString(R.string.widget_due_label), colors.onBackgroundMuted, labelSize)
                if (data.eta > 0 && data.dueCardsCount > 0) {
                    WidgetText(
                        strings.getString(R.string.widget_minutes, data.eta),
                        colors.onBackgroundMuted,
                        labelSize,
                        GlanceModifier.semantics {
                            contentDescription = strings.getQuantityString(CommonPlurals.widget_eta, data.eta, data.eta)
                        },
                    )
                }
            }
        }
    }
}
