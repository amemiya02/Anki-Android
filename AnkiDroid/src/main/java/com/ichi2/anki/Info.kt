// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2009 Nicolas Raoul <nicolas.raoul@gmail.com>
// SPDX-FileCopyrightText: Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>
// SPDX-FileCopyrightText: Copyright (c) 2015 Tim Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.utils.android.getColorFromAttr
import com.ichi2.anki.multimedia.AttachmentAppBar
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.workarounds.OnWebViewRecreatedListener
import com.ichi2.anki.workarounds.SafeWebViewClient
import com.ichi2.anki.workarounds.SafeWebViewLayout
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import com.ichi2.utils.IntentUtil.canOpenIntent
import com.ichi2.utils.IntentUtil.tryOpenIntent
import com.ichi2.utils.VersionUtils.pkgVersionName
import com.ichi2.utils.ViewGroupUtils.setRenderWorkaround
import com.ichi2.utils.toRGBHex
import timber.log.Timber
import com.ichi2.anki.common.android.R as CommonR

private const val CHANGE_LOG_URL = "https://docs.ankidroid.org/changelog.html"

/** Compatibility entry for existing shortcuts to the changelog. */
class Info : SingleFragmentActivity() {
    override fun createFragment(): Fragment = InfoFragment()
}

class InfoFragment :
    Fragment(),
    OnWebViewRecreatedListener {
    private lateinit var page: SafeWebViewLayout
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var loading by mutableStateOf(true)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = inflater.context

        fun compose(content: @Composable () -> Unit) =
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent { AnkiDroidTheme { content() } }
            }
        val toolbar =
            compose {
                AttachmentAppBar("${stringResource(CommonString.app_name)} v$pkgVersionName", onBack = {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                })
            }.apply { id = R.id.toolbar_container }
        page = SafeWebViewLayout(context).apply { id = R.id.web_view }
        val webArea =
            FrameLayout(context).apply {
                addView(page, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                addView(
                    compose { if (loading) CircularProgressIndicator() },
                    FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER),
                )
            }
        val canRate = canOpenMarketUri()
        val body =
            LinearLayout(context).apply {
                id = R.id.content
                orientation = LinearLayout.VERTICAL
                addView(webArea, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
                addView(compose { Actions(canRate) }, MATCH_PARENT, WRAP_CONTENT)
            }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColorFromAttr(context, CommonR.attr.foundationBackground))
            addView(toolbar, MATCH_PARENT, WRAP_CONTENT)
            addView(body, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                val bars = insets.getInsets(systemBars() or displayCutout())
                toolbar.updatePadding(left = bars.left, top = bars.top, right = bars.right)
                body.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
                insets
            }
        }
    }

    @Composable
    private fun Actions(canRate: Boolean) {
        val foundation = MaterialTheme.foundation
        Column {
            HorizontalDivider(thickness = foundation.shapes.hairline, color = foundation.colors.outline)
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                if (canRate) {
                    TextButton(onClick = { tryOpenIntent(requireActivity(), AnkiDroidApp.getMarketIntent(requireContext())) }) {
                        Text(stringResource(CommonString.info_rate))
                    }
                }
                Button(onClick = { finishScreen(Activity.RESULT_OK) }, shape = foundation.shapes.button) {
                    Text(stringResource(CommonString.dialog_continue))
                }
            }
            if (BuildConfig.SHOW_DONATE_LINKS) {
                TextButton(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    onClick = { (requireActivity() as AnkiActivity).openUrl(R.string.link_opencollective_donate) },
                ) { Text(stringResource(CommonString.help_item_support_opencollective_donate)) }
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        InitialActivity.setUpgradedToLatestVersion(requireContext().sharedPrefs())
        onBackPressedCallback =
            object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    if (page.canGoBack()) page.goBack()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
        setupWebView()
    }

    override fun onWebViewRecreated() {
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        loading = true
        val context = requireContext()
        val backgroundColor = getColorFromAttr(context, CommonR.attr.foundationBackground)
        val textColor = getColorFromAttr(context, CommonR.attr.foundationOnBackground).toRGBHex()
        val anchorTextColor = getColorFromAttr(context, CommonR.attr.foundationPrimary).toRGBHex()
        val background = backgroundColor.toRGBHex()

        page.setBackgroundColor(backgroundColor)
        page.settings.allowFileAccess = true
        page.settings.allowContentAccess = true
        setRenderWorkaround(requireActivity())

        page.setWebChromeClient(
            object : WebChromeClient() {
                override fun onProgressChanged(
                    view: WebView,
                    progress: Int,
                ) {
                    // Hide the progress indicator when the page has finished loaded
                    if (progress == 100) {
                        loading = false
                    }
                }
            },
        )

        page.settings.javaScriptEnabled = true
        page.setWebViewClient(
            object : SafeWebViewClient() {
                override fun onPageFinished(
                    view: WebView,
                    url: String,
                ) {
                /* The order of below javascript code must not change (this order works both in debug and release mode)
                 *  or else it will break in any one mode.
                 */
                    @Suppress("ktlint:standard:max-line-length")
                    view.loadUrl(
                        """javascript:document.body.style.setProperty("color", "$textColor");
                            x=document.getElementsByTagName("a");
                            for(i=0; i<x.length; i++){
                              x[i].style.color="$anchorTextColor";
                            }
                            document.getElementsByTagName("h1")[0].style.color="$textColor";
                            x=document.getElementsByTagName("h2");
                            for(i=0; i<x.length; i++){
                              x[i].style.color="$anchorTextColor";
                            }
                            document.body.style.setProperty("background", "$background");""",
                    )
                    if (!BuildConfig.SHOW_DONATE_LINKS) {
                        // remove donation links, keeping the text
                        view.evaluateJavascript(
                            """document.querySelectorAll('a[href*="opencollective.com"]')
                                .forEach((a) => a.replaceWith(...a.childNodes));""",
                            null,
                        )
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    // Excludes the url that are opened inside the changelog.html
                    // and redirect the user to the browser
                    val url = request?.url?.toString() ?: return false
                    if (url == CHANGE_LOG_URL) {
                        return false
                    }
                    (requireActivity() as AnkiActivity).openUrl(url)
                    return true
                }

                override fun doUpdateVisitedHistory(
                    view: WebView?,
                    url: String?,
                    isReload: Boolean,
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    onBackPressedCallback.isEnabled = page.canGoBack()
                }
            },
        )
        page.loadUrl("/android_asset/changelog.html")
    }

    override fun onDestroyView() {
        page.destroy()
        super.onDestroyView()
    }

    private fun canOpenMarketUri(): Boolean =
        try {
            canOpenIntent(requireContext(), AnkiDroidApp.getMarketIntent(requireContext()))
        } catch (e: Exception) {
            Timber.w(e)
            false
        }
}
