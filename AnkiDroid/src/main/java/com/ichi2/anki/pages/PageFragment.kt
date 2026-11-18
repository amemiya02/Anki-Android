// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.MaterialColors
import com.ichi2.anki.multimedia.AttachmentAppBar
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.workarounds.NestedScrollingSafeWebViewLayout
import com.ichi2.anki.workarounds.OnWebViewRecreatedListener
import com.ichi2.anki.workarounds.SafeWebViewLayout
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.themes.Themes
import com.ichi2.utils.showDialogIfWebViewOutdated
import timber.log.Timber
import com.ichi2.anki.common.android.R as CommonR

/**
 * Base class for displaying Anki HTML pages
 */
abstract class PageFragment :
    Fragment(),
    PostRequestHandler,
    OnWebViewRecreatedListener {
    lateinit var webViewLayout: SafeWebViewLayout
    protected abstract val pagePath: String

    protected var pageTitle by mutableStateOf("")
    protected var pageLoading by mutableStateOf(false)
    protected open val scrollToolbar = false
    protected open val showBack = true
    protected open val selectDeck: (() -> Unit)? = null
    protected open val save: (() -> Unit)? = null

    @Composable
    protected open fun PageActions() { }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = inflater.context
        val header =
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AnkiDroidTheme {
                        AttachmentAppBar(
                            title = pageTitle,
                            onBack = if (showBack) ({ requireActivity().onBackPressedDispatcher.onBackPressed() }) else null,
                            onSave = save,
                            onTitleClick = selectDeck,
                        ) { PageActions() }
                    }
                }
            }
        val appBar =
            AppBarLayout(context).apply {
                addView(
                    header,
                    AppBarLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        scrollFlags =
                            if (scrollToolbar) {
                                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                                    AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
                            } else {
                                0
                            }
                    },
                )
            }
        webViewLayout = if (scrollToolbar) NestedScrollingSafeWebViewLayout(context) else SafeWebViewLayout(context)
        webViewLayout.visibility = if (scrollToolbar) View.VISIBLE else View.INVISIBLE
        val body =
            FrameLayout(context).apply {
                addView(webViewLayout, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                addView(
                    ComposeView(context).apply {
                        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                        setContent { AnkiDroidTheme { if (pageLoading) CircularProgressIndicator() } }
                    },
                    FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER),
                )
            }
        return CoordinatorLayout(context).apply {
            addView(appBar, CoordinatorLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(
                body,
                CoordinatorLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                    behavior = AppBarLayout.ScrollingViewBehavior()
                },
            )
        }
    }

    /**
     * Override this to set a custom [WebViewClient] to the page.
     * This is called in [onViewCreated].
     *
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    protected open fun onCreateWebViewClient(savedInstanceState: Bundle?) = PageWebViewClient()

    protected open fun onWebViewCreated() { }

    /**
     * When the webview calls `BridgeCommand("foo")`, the PageFragment execute `bridgeCommands["foo"]`.
     * By default, only bridge command is allowed, subclasses must redefine it if they expect bridge commands.
     */
    open val bridgeCommands: Map<String, () -> Unit> = mapOf()

    /**
     * Ensures that [pageWebViewClient] can receive `bridgeCommand` requests and execute the command from [bridgeCommands].
     */
    private fun setupBridgeCommand(pageWebViewClient: PageWebViewClient) {
        if (bridgeCommands.isEmpty()) {
            return
        }
        webViewLayout.addJavascriptInterface(
            object : Any() {
                @JavascriptInterface
                fun bridgeCommandImpl(request: String) {
                    bridgeCommands.getOrDefault(request) {
                        Timber.d("Unknown request received %s", request)
                    }()
                }
            },
            "bridgeCommandInterface",
        )
        pageWebViewClient.onPageFinishedCallbacks.add { webView ->
            webView.evaluateJavascript(
                "bridgeCommand = function(request){ bridgeCommandInterface.bridgeCommandImpl(request); };",
            ) {}
        }
    }

    @CallSuper
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        // the pages can't send requests without PostRequestBridge, which requires a modern WebView
        val isOutdated =
            with(requireContext()) {
                showDialogIfWebViewOutdated {
                    finishScreen()
                }
            }
        if (isOutdated) {
            Timber.w("${this::class.simpleName} requires modern WebView version, aborting load")
            return
        }
        setupWebView(savedInstanceState)
    }

    private fun setupWebView(savedInstanceState: Bundle?) {
        val pageWebViewClient = onCreateWebViewClient(savedInstanceState)
        webViewLayout.apply {
            setBackgroundColor(MaterialColors.getColor(this, CommonR.attr.foundationBackground))
            setAcceptThirdPartyCookies(true)
            with(settings) {
                javaScriptEnabled = true
                displayZoomControls = false
                builtInZoomControls = true
                setSupportZoom(true)
            }
            setWebViewClient(pageWebViewClient)
            setWebChromeClient(PageChromeClient())
            setupBridgeCommand(pageWebViewClient)
            onWebViewCreated()
        }
        PostRequestBridge(this, viewLifecycleOwner.lifecycleScope).attachTo(webViewLayout)
        val nightMode = if (Themes.isNightTheme) "#night" else ""
        val url = "${PostRequestBridge.BASE_URL}$pagePath$nightMode".toUri()
        Timber.i("Loading $url")
        webViewLayout.loadUrl(url.toString())
    }

    override suspend fun handlePostRequest(
        uri: PostRequestUri,
        bytes: ByteArray,
    ): ByteArray {
        val methodName = uri.backendMethodName ?: throw IllegalArgumentException("unhandled request: $uri")

        val resolvedUiMethod =
            when (val uiResponse = activity.handleUiPostRequest(methodName, bytes, this)) {
                is UiPostRequestResponse.Handled -> return uiResponse.data
                is UiPostRequestResponse.UnknownMethod -> false
                is UiPostRequestResponse.Ignored -> true
            }

        return handleCollectionPostRequest(methodName, bytes) ?: run {
            if (!resolvedUiMethod) {
                Timber.w("Unknown TS method called.")
                Timber.d("No handlers resolve TS method %s", methodName)
            }
            throw IllegalArgumentException("unhandled method: $methodName")
        }
    }

    @CallSuper
    override fun onDestroyView() {
        webViewLayout.destroy()
        super.onDestroyView()
    }

    override fun onWebViewRecreated() {
        setupWebView(null)
    }
}
