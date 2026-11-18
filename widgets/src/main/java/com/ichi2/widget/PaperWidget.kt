// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.widget

import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.ConfigurationCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ichi2.anki.common.android.withAppLocale
import com.ichi2.anki.common.coroutines.applicationScope
import com.ichi2.anki.launchCatching
import com.ichi2.anki.settings.enums.DayTheme
import com.ichi2.anki.settings.enums.NightTheme
import com.ichi2.anki.settings.enums.Theme
import com.ichi2.anki.widgets.R
import com.ichi2.compose.theme.toFoundation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/** Uses the same resolved theme as an app screen, including Black with an explicit outline. */
class WidgetAppearance internal constructor(
    context: Context,
) {
    private val theme = resolveTheme(context)
    val localizedContext = context.widgetLocaleContext()
    val colors = ContextThemeWrapper(localizedContext, theme.styleResId).toFoundation().colors
    val black = theme == NightTheme.BLACK
    private val locale = ConfigurationCompat.getLocales(localizedContext.resources.configuration)[0] ?: Locale.getDefault()
    private val fontScale =
        localizedContext.resources.configuration.fontScale
            .coerceAtLeast(1f)

    fun number(value: Int): String = NumberFormat.getIntegerInstance(locale).format(value)

    /**
     * RemoteViews must fit inside the host's physical bounds. Honor font scaling until the
     * resulting text would outgrow the space for the given layout.
     */
    fun textSize(
        preferred: Int,
        limit: Int,
    ): TextUnit = minOf(preferred.toFloat(), limit / fontScale).sp

    fun textHeight(font: TextUnit): Float = font.value * fontScale * 1.28f

    companion object {
        private var resolveTheme: (Context) -> Theme = { DayTheme.LIGHT }

        fun registerThemeResolver(resolver: (Context) -> Theme) {
            resolveTheme = resolver
        }
    }
}

/** A widget may be the first component started after the app process has died. */
private fun Context.widgetLocaleContext(): Context {
    val liveLocale = withAppLocale()
    if (liveLocale !== this) return liveLocale
    val stored = LocaleManagerCompat.getApplicationLocales(this)
    if (stored.isEmpty) return this
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList.forLanguageTags(stored.toLanguageTags()))
    return createConfigurationContext(configuration)
}

abstract class PaperWidget<T>(
    private val receiver: Class<out AnalyticsWidgetProvider>,
) : GlanceAppWidget() {
    private val revisions = MutableStateFlow(0L)

    final override val sizeMode = SizeMode.Exact
    final override val stateDefinition = null

    /** Glance keeps a composition alive after the first render. Refresh the data in that session too. */
    internal fun invalidate() {
        revisions.update { it + 1 }
    }

    internal fun refresh(context: Context) {
        val appContext = context.applicationContext
        applicationScope.launchCatching(errorMessageHandler = {}) {
            val manager = getAppWidgetManager(appContext) ?: return@launchCatching
            val ids = manager.getAppWidgetIds(ComponentName(appContext, receiver))
            if (ids.isEmpty()) return@launchCatching
            invalidate()
            val glance = GlanceAppWidgetManager(appContext)
            ids.forEach { update(appContext, glance.getGlanceIdBy(it)) }
        }
    }

    final override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        // All of the host's simultaneous sizes share one query and one update observer.
        val initialRevision = revisions.value
        val hostId = AppWidgetId(GlanceAppWidgetManager(context).getAppWidgetId(id))

        suspend fun read() = WidgetAppearance(context) to load(context, hostId)
        coroutineScope {
            val result = MutableStateFlow(read())
            launch {
                revisions.collectLatest { revision ->
                    if (revision != initialRevision) result.value = read()
                }
            }
            provideContent {
                val state by result.collectAsState()
                Content(state.first, state.second)
            }
        }
    }

    protected abstract suspend fun load(
        context: Context,
        id: AppWidgetId,
    ): T

    @Composable
    @GlanceComposable
    protected abstract fun Content(
        appearance: WidgetAppearance,
        data: T,
    )
}

@Suppress("ktlint:standard:annotation")
@Composable
@GlanceComposable
internal fun WidgetSheet(
    appearance: WidgetAppearance,
    action: Action? = null,
    content: @Composable @GlanceComposable () -> Unit,
) {
    val compact = LocalSize.current.height < 80.dp
    val shape = if (compact) R.drawable.widget_sheet_compact else R.drawable.widget_sheet
    val click = if (action == null) GlanceModifier else GlanceModifier.clickable(action)
    val outline = if (appearance.black) appearance.colors.outline else appearance.colors.background
    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .paperBackground(shape, outline)
            .then(click)
            .padding(if (appearance.black) 1.dp else 0.dp),
    ) {
        Box(GlanceModifier.fillMaxSize().paperBackground(shape, appearance.colors.background)) {
            content()
        }
    }
}

internal fun GlanceModifier.paperBackground(
    shape: Int,
    color: Color,
): GlanceModifier =
    background(
        ImageProvider(shape),
        colorFilter = ColorFilter.tint(ColorProvider(color)),
    )

@Composable
@GlanceComposable
internal fun WidgetText(
    text: String,
    color: Color,
    size: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1,
) {
    Text(
        text,
        modifier,
        // Glance exposes system font families to the host. Use the Foundation's serif fallback.
        style = TextStyle(color = ColorProvider(color), fontSize = size, fontWeight = weight, fontFamily = FontFamily.Serif),
        maxLines = maxLines,
    )
}
