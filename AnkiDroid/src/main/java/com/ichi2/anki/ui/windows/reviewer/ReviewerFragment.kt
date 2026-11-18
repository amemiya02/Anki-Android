// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ActionMenuView
import androidx.compose.runtime.getValue
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import anki.scheduler.CardAnswer.Rating
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.AnkiDroidJsAPI
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.DispatchKeyEventListener
import com.ichi2.anki.Flag
import com.ichi2.anki.R
import com.ichi2.anki.android.AnkiShakeDetector
import com.ichi2.anki.cardviewer.Gesture
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.common.destinations.BrowserDestination
import com.ichi2.anki.common.destinations.DeckOptionsDestination
import com.ichi2.anki.common.destinations.PreferencesDestination
import com.ichi2.anki.common.destinations.StatisticsDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.common.utils.android.isRobolectric
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.databinding.FragmentReviewerBinding
import com.ichi2.anki.dialogs.help.HelpDialog
import com.ichi2.anki.dialogs.showDeckOptionsSelectionDialog
import com.ichi2.anki.dialogs.tags.TagsDialogFactory
import com.ichi2.anki.dialogs.tags.TagsDialogListener
import com.ichi2.anki.libanki.Card
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.Note
import com.ichi2.anki.libanki.NoteId
import com.ichi2.anki.model.CardStateFilter
import com.ichi2.anki.pages.PostRequestUri
import com.ichi2.anki.preferences.reviewer.ViewerAction
import com.ichi2.anki.preferences.reviewer.WhiteboardAction
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.previewer.CardViewerFragment
import com.ichi2.anki.previewer.TypeAnswer
import com.ichi2.anki.previewer.setFrameStyle
import com.ichi2.anki.previewer.stdHtml
import com.ichi2.anki.reviewer.BindingMap
import com.ichi2.anki.reviewer.FullScreenMode
import com.ichi2.anki.reviewer.ReviewerBinding
import com.ichi2.anki.scheduling.ForgetCardsDialog
import com.ichi2.anki.scheduling.SetDueDateDialog
import com.ichi2.anki.scheduling.registerOnForgetHandler
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.settings.enums.FrameStyle
import com.ichi2.anki.settings.enums.HideSystemBars
import com.ichi2.anki.settings.enums.ToolbarPosition
import com.ichi2.anki.snackbar.BaseSnackbarBuilderProvider
import com.ichi2.anki.snackbar.SnackbarBuilder
import com.ichi2.anki.snackbar.setMaxLines
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.study.StudyCompletionUi
import com.ichi2.anki.ui.windows.reviewer.audiorecord.CheckPronunciationFragment
import com.ichi2.anki.ui.windows.reviewer.whiteboard.WhiteboardFragment
import com.ichi2.anki.utils.ext.collectInViewLifecycle
import com.ichi2.anki.utils.ext.sharedPrefs
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.anki.utils.ext.window
import com.ichi2.anki.utils.openUrl
import com.ichi2.anki.workarounds.SafeWebViewLayout
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.themes.Themes
import com.ichi2.ui.TypeAnswerEditText
import com.ichi2.utils.IntentUtil
import com.ichi2.utils.ViewGroupUtils.setRenderWorkaround
import com.ichi2.utils.show
import com.squareup.seismic.ShakeDetector
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.reflect.jvm.jvmName
import kotlin.time.Duration.Companion.milliseconds

class ReviewerFragment :
    CardViewerFragment(R.layout.fragment_reviewer),
    BaseSnackbarBuilderProvider,
    ActionMenuView.OnMenuItemClickListener,
    DispatchKeyEventListener,
    TagsDialogListener,
    ShakeDetector.Listener {
    override val viewModel: ReviewerViewModel by viewModels()

    @VisibleForTesting
    internal val binding by viewBinding(FragmentReviewerBinding::bind)

    override val webViewLayout: SafeWebViewLayout get() = binding.webViewLayout
    private lateinit var bindingMap: BindingMap<ReviewerBinding, ViewerAction>
    private lateinit var reviewerWebViewClient: ReviewerWebViewClient
    private var shakeDetector: AnkiShakeDetector? = null
    private val whiteboardFragment get() = childFragmentManager.findFragmentByTag(WhiteboardFragment::class.jvmName) as? WhiteboardFragment
    private val isBigScreen: Boolean get() = resources.configuration.smallestScreenWidthDp >= 720

    override val baseSnackbarBuilder: SnackbarBuilder = {
        anchorView =
            when {
                binding.typeAnswerContainer.isVisible -> binding.typeAnswerContainer
                binding.answerArea.isVisible -> binding.answerArea
                Prefs.toolbarPosition == ToolbarPosition.BOTTOM -> binding.toolsLayout
                else -> null
            }
    }

    private lateinit var tagsDialogFactory: TagsDialogFactory
    private var templateNavigationDialog: AlertDialog? = null

    private val jsApiDelegate =
        lazy {
            AnkiDroidJsAPI(
                context = requireContext(),
                showMessage = { if (view != null) showSnackbar(it) },
                cardApi =
                    object : AnkiDroidJsAPI.CardApi {
                        override val isDisplayingAnswer: Boolean get() = viewModel.showingAnswer.value
                        override val isFullscreen: Boolean get() = !isTopbarShown
                        override val isTopbarShown: Boolean get() = binding.studyBar.isVisible
                        override val isInNightMode: Boolean get() = Themes.isNightTheme

                        override suspend fun getCard(): Card = viewModel.currentCard.await()

                        override suspend fun getCardData(includeEta: Boolean): AnkiDroidJsAPI.CardDataForJsApi {
                            // The JavaScript API still honors the saved ETA setting from older profiles.
                            val reportEta = includeEta && sharedPrefs().getBoolean(getString(R.string.show_eta_preference), false)
                            return viewModel.getCardDataForJsApi(reportEta)
                        }

                        override suspend fun showOrAnswer(rating: Rating?) {
                            viewModel.onCardTemplateAction(rating, waitForFlip = true)
                        }

                        override suspend fun toggleFlag(flag: Flag): Boolean = viewModel.onCardTemplateToggleFlag(flag)

                        override suspend fun performAction(action: ViewerAction): Boolean = viewModel.onCardTemplateCommand(action)

                        override suspend fun setCardDue(days: Int): Boolean = viewModel.onCardTemplateSetDue(days)

                        override suspend fun resetProgress(): Boolean = viewModel.onCardTemplateResetProgress()

                        override suspend fun updateNoteTags(
                            card: Card,
                            noteId: NoteId,
                            update: Collection.(Note) -> Unit,
                        ): Boolean = viewModel.onCardTemplateUpdateTags(card, noteId, update)

                        override fun searchCard(query: String): Boolean {
                            navigate(BrowserDestination.Search(query, allDecks = false))
                            return true
                        }

                        override fun setFullscreenMode(mode: FullScreenMode): Boolean {
                            viewModel.cardTemplateFullscreenMode = mode
                            applyStudyScreenVisibility()
                            return true
                        }

                        override fun setScrollbar(
                            horizontal: Boolean,
                            enabled: Boolean,
                        ): Boolean = webViewLayout.setScrollbar(horizontal, enabled)

                        override fun showNavigation(): Boolean = showTemplateNavigation()

                        override fun showOptionsMenu(): Boolean = binding.reviewerMenuView.showOptionsMenu(binding.cardIndicators)

                        override fun canStartSpeechRecognition(): Boolean = Prefs.allowTemplatesToRecordAudio

                        override fun captureCallback(): (String) -> Unit {
                            val owner = viewLifecycleOwner
                            val layout = webViewLayout
                            val client = reviewerWebViewClient
                            val card = viewModel.currentCard
                            val renderVersion = viewModel.cardTemplateRenderVersion
                            return { script ->
                                if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                                    client === reviewerWebViewClient &&
                                    card === viewModel.currentCard &&
                                    renderVersion == viewModel.cardTemplateRenderVersion
                                ) {
                                    layout.evaluateJavascript(script)
                                }
                            }
                        }
                    },
                showDeveloperMessage = { message ->
                    if (view != null) {
                        showSnackbar(message, Snackbar.LENGTH_INDEFINITE) {
                            setMaxLines(3)
                            setAction(CommonString.reviewer_invalid_api_version_visit_documentation) {
                                openUrl("https://github.com/ankidroid/Anki-Android/wiki".toUri())
                            }
                        }
                    }
                },
            )
        }
    private val jsApi by jsApiDelegate

    override suspend fun handlePostRequest(
        uri: PostRequestUri,
        bytes: ByteArray,
    ): ByteArray {
        if (uri.ankidroidMethodName == "relinquishFocus") {
            focusNativeAnswerField()
            return byteArrayOf()
        }
        val methodName = uri.jsApiMethodName ?: return super.handlePostRequest(uri, bytes)
        return jsApi.handleJsApiRequest(methodName, bytes)
    }

    private fun focusNativeAnswerField() {
        if (Prefs.autoFocusTypeAnswer && binding.typeAnswerContainer.isVisible) {
            binding.typeAnswerEditText.apply {
                requestFocus()
                WindowInsetsControllerCompat(window, this).show(WindowInsetsCompat.Type.ime())
            }
        } else {
            binding.rootLayout.requestFocus()
        }
    }

    override fun onLoadInitialHtml(): String =
        stdHtml(
            context = requireContext(),
            extraJsAssets = listOf("scripts/ankidroid-reviewer.js", "scripts/type-answer.js", "scripts/js-api.js"),
            nightMode = Themes.isNightTheme,
            // "Image zoom" in settings scales the Card's images apart from its text
            extraCss =
                Prefs.imageZoom
                    .takeIf { it != 100 }
                    ?.let { "img { zoom: ${it / 100.0} }" }
                    .orEmpty(),
        )

    override fun onStart() {
        super.onStart()
        shakeDetector?.start()
    }

    override fun onStop() {
        super.onStop()
        if (!requireActivity().isChangingConfigurations) {
            viewModel.stopAutoAdvance()
        }
        shakeDetector?.stop()
        if (jsApiDelegate.isInitialized()) jsApi.cancelSpeechRecognition()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tagsDialogFactory = TagsDialogFactory(this).attachToActivity<TagsDialogFactory>(requireActivity())
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            requireActivity().finish()
        }

        setupBindings()
        setupImmersiveMode()
        setupTypeAnswer()
        setupAnswerButtons()
        setupStudyBar()
        setupMenu()
        setupToolbarPosition()
        applyStudyScreenVisibility()
        setupAnswerTimer()
        setupMargins()
        setupResetProgress()
        setupCheckPronunciation()
        setupActions()
        setupWhiteboard()
        setupTimebox()

        val viewScope = viewLifecycleOwner.lifecycleScope

        viewModel.finishResultFlow
            .onEach { result ->
                requireActivity().run {
                    setResult(result)
                    // the deck list opens the finished screen from the result; nobody awaits
                    // one when studying was opened from a shortcut or the finished screen
                    if (result == RESULT_NO_MORE_CARDS && callingActivity == null) {
                        StudyCompletionUi.openFinishedScreen(this)
                    }
                    finish()
                }
            }.launchIn(viewScope)

        viewModel.statesMutationEvalFlow
            .onEach { eval ->
                // Completion is signaled by `statesMutated`
                webViewLayout.evaluateJavascript(eval) { result ->
                    // eval failed, usually a syntax error
                    // Note: this is `"null"`, not null
                    if ("null" == result) {
                        viewModel.onStateMutationCallback()
                    }
                }
            }.launchIn(viewScope)

        viewModel.showingAnswer
            .onEach { showingAnswer ->
                // reset zoom if flipped
                if (showingAnswer) reviewerWebViewClient.resetZoom()
                if (jsApiDelegate.isInitialized()) jsApi.cancelSpeechRecognition()
                // focus on the whole layout so motion controllers can be captured
                // without navigating the other View elements
                binding.rootLayout.requestFocus()
            }.launchIn(viewScope)

        viewModel.onCardUpdatedFlow
            .onEach {
                // reset zoom if the card changes (incl. bury/suspend)
                reviewerWebViewClient.resetZoom()
                if (jsApiDelegate.isInitialized()) jsApi.cancelSpeechRecognition()
                binding.typeAnswerEditText.text = null
                whiteboardFragment?.resetCanvas()
            }.launchIn(viewScope)

        viewModel.navigateFlow.collectInViewLifecycle { destination ->
            if (destination is DeckOptionsDestination && destination.options.size > 1) {
                requireContext().showDeckOptionsSelectionDialog(destination.options) { selectedOption ->
                    Timber.i("Deck options target selected: ${selectedOption.deckId}")
                    navigate(
                        destination.copy(
                            deckId = selectedOption.deckId,
                            isFiltered = selectedOption.isFiltered,
                        ),
                    )
                }
                return@collectInViewLifecycle
            }
            navigate(destination)
        }

        binding.webViewContainer.setFrameStyle()

        if (Prefs.showAnswerFeedback) {
            viewModel.answerFeedbackFlow.collectInViewLifecycle { rating ->
                binding.answerFeedback.show(rating)
            }
        }

        // an auto-advancing session must not fall asleep in the middle
        val keepScreenOn = Prefs.keepScreenOn
        viewModel.isAutoAdvanceEnabledFlow.collectInViewLifecycle { isAutoAdvancing ->
            if (keepScreenOn || isAutoAdvancing) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        setRenderWorkaround(requireActivity())
    }

    override fun onWebViewRecreated() {
        if (jsApiDelegate.isInitialized()) jsApi.cancelSpeechRecognition()
        super.onWebViewRecreated()
        setRenderWorkaround(requireActivity())
    }

    override fun onDestroyView() {
        templateNavigationDialog?.dismiss()
        templateNavigationDialog = null
        super.onDestroyView()
    }

    private fun setupTypeAnswer() {
        binding.typeAnswerEditText.apply {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    viewModel.onShowAnswer()
                    return@setOnEditorActionListener true
                }
                false
            }
            setOnFocusChangeListener { editTextView, hasFocus ->
                val insetsController = WindowInsetsControllerCompat(window, editTextView)
                if (hasFocus) {
                    insetsController.show(WindowInsetsCompat.Type.ime())
                } else {
                    insetsController.hide(WindowInsetsCompat.Type.ime())
                }
            }
        }

        val isHtmlTypeAnswerEnabled = Prefs.isHtmlTypeAnswerEnabled
        val autoFocusTypeAnswer = Prefs.autoFocusTypeAnswer

        /**
         * Sync keyboard suggestions and `imeHintLocales` on the answer field to match
         * [typeInAnswer]. Returns `true` if anything changed (caller should `restartInput()`).
         *
         * @see TypeAnswerEditText.noSuggest
         */
        fun TypeAnswerEditText.syncTypeAnswerProperties(typeInAnswer: TypeAnswer): Boolean {
            var changed = false
            if (noSuggest != typeInAnswer.noSuggest) {
                noSuggest = typeInAnswer.noSuggest
                changed = true
            }
            if (imeHintLocales != typeInAnswer.imeHintLocales) {
                imeHintLocales = typeInAnswer.imeHintLocales
                changed = true
            }
            return changed
        }

        viewModel.typeAnswerFlow.collectInViewLifecycle { typeInAnswer ->
            if (typeInAnswer == null) {
                binding.typeAnswerContainer.isVisible = false
                return@collectInViewLifecycle
            }

            if (isHtmlTypeAnswerEnabled) {
                if (!autoFocusTypeAnswer) return@collectInViewLifecycle
                webViewLayout.focusOnWebView()
                // `evaluateJavascript()` doesn't trigger the IME unless the WebView
                // has been touched before, so ´loadUrl()` is used instead.
                webViewLayout.loadUrl("javascript:document.getElementById('typeans')?.focus();")
                return@collectInViewLifecycle
            }

            binding.typeAnswerContainer.isVisible = true
            binding.typeAnswerEditText.apply {
                if (syncTypeAnswerProperties(typeInAnswer)) {
                    context?.getSystemService<InputMethodManager>()?.restartInput(this)
                }
                if (autoFocusTypeAnswer) {
                    requestFocus()
                }
            }
        }

        viewModel.onTypedAnswerResultFlow.collectInViewLifecycle { request ->
            if (isHtmlTypeAnswerEnabled) {
                val script = """document.getElementById("typeans").value;"""
                webViewLayout.evaluateJavascript(script) { callback ->
                    // the retuned string comes with surrounding `"`, so remove it once
                    val typedAnswer = callback.removeSurrounding("\"")
                    request.complete(typedAnswer)
                }
            } else {
                val typedAnswer = binding.typeAnswerEditText.text.toString()
                request.complete(typedAnswer)
            }
        }
    }

    @NeedsTest("Whiteboard takes priority on key events")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (
            event.action != KeyEvent.ACTION_DOWN ||
            view?.let { binding.typeAnswerEditText }?.isFocused == true
        ) {
            return false
        }
        return whiteboardFragment?.dispatchKeyEvent(event) == true || bindingMap.onKeyDown(event)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        Timber.v("ReviewerFragment::onMenuItemClick %s", item)
        if (item.hasSubMenu()) return false
        val action = ViewerAction.fromId(item.itemId)
        viewModel.executeAction(action)
        return true
    }

    @NeedsTest("Whiteboard takes priority on shake events")
    override fun hearShake() {
        // Sensor events still arrive while a dialog or another window covers the reviewer.
        if (view?.hasWindowFocus() != true) {
            Timber.d("Ignoring shake: reviewer window does not have focus")
            return
        }

        if (whiteboardFragment?.onScreenShake() != true) {
            bindingMap.onGesture(Gesture.SHAKE)
        }
    }

    private fun setupBindings() {
        bindingMap = BindingMap(sharedPrefs(), ViewerAction.entries, viewModel)
        binding.root.setOnGenericMotionListener { _, event ->
            bindingMap.onGenericMotionEvent(event)
        }
        if (isBound(Gesture.SHAKE)) {
            shakeDetector = AnkiShakeDetector.createInstance(requireContext(), this)
        }
    }

    private fun isBound(gesture: Gesture): Boolean =
        bindingMap.isBound(gesture) || BindingMap(sharedPrefs(), WhiteboardAction.entries).isBound(gesture)

    private fun setupAnswerButtons() {
        val insetsController = WindowInsetsControllerCompat(window, binding.rootLayout)
        viewModel.showingAnswer
            .onEach { isAnswerShown ->
                if (isAnswerShown && binding.answerArea.isVisible) {
                    insetsController.hide(WindowInsetsCompat.Type.ime())
                }
            }.launchIn(viewLifecycleOwner.lifecycleScope)

        val showHardAndEasyButtons = !Prefs.hideHardAndEasyButtons
        val buttonsHeightPercentage = Prefs.newStudyScreenAnswerButtonSize
        val showAnswerMinimumPressMs = Prefs.showAnswerMinimumPressMs
        val doubleTapIntervalMs = Prefs.doubleTapInterval
        var hasShownPressHint = false
        binding.answerArea.setContent {
            AnkiDroidTheme {
                val isAnswerShown by viewModel.showingAnswer.collectAsStateWithLifecycle()
                val nextTimes by viewModel.answerButtonsNextTimeFlow.collectAsStateWithLifecycle()
                AnswerArea(
                    isAnswerShown = isAnswerShown,
                    nextTimes = nextTimes,
                    onRatingClicked = { viewModel.answerCard(it) },
                    onShowAnswerClicked = { viewModel.onShowAnswer() },
                    showHardAndEasyButtons = showHardAndEasyButtons,
                    buttonsHeightPercentage = buttonsHeightPercentage,
                    onShowAnswerPressTooShort = {
                        if (!hasShownPressHint) {
                            hasShownPressHint = true
                            showMinimumPressHint()
                        }
                    },
                    showAnswerMinimumPressMs = showAnswerMinimumPressMs,
                    doubleTapIntervalMs = doubleTapIntervalMs,
                )
            }
        }
    }

    /** Says once that Show answer needs a longer press, and where that is set */
    private fun showMinimumPressHint() {
        val message =
            getString(
                CommonString.show_answer_hint_long_press,
                getString(CommonString.pref_show_answer_long_press_time),
            )
        showSnackbar(message) {
            setAction(CommonString.settings) { navigate(PreferencesDestination.Accessibility) }
        }
    }

    private fun setupStudyBar() {
        binding.studyBarTitles.setContent {
            AnkiDroidTheme {
                val state by viewModel.studyBarFlow.collectAsStateWithLifecycle()
                StudyBarTitles(state)
            }
        }
        binding.studyProgress.setContent {
            AnkiDroidTheme {
                val state by viewModel.studyBarFlow.collectAsStateWithLifecycle()
                StudyProgress(state.progress)
            }
        }
    }

    private fun setupMenu() {
        binding.reviewerMenuView.apply {
            setup(lifecycle, viewModel)
            setOnMenuItemClickListener(this@ReviewerFragment)
        }
    }

    private fun setupImmersiveMode() {
        applySystemBarVisibility()
        if (Prefs.hideSystemBars == HideSystemBars.NONE && viewModel.cardTemplateFullscreenMode == null) return
        val minTopPadding =
            if (Prefs.frameStyle == FrameStyle.CARD && Prefs.toolbarPosition != ToolbarPosition.TOP) {
                8F.dp.toPx(requireContext())
            } else {
                0
            }
        val ignoreDisplayCutout = Prefs.ignoreDisplayCutout
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val defaultTypes = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            val typeMask =
                if (ignoreDisplayCutout) {
                    defaultTypes
                } else {
                    defaultTypes or WindowInsetsCompat.Type.displayCutout()
                }
            val bars = insets.getInsets(typeMask)
            // don't let a 'card' frame reach the top of the screen
            val topPadding = max(bars.top, minTopPadding)
            v.updatePadding(
                left = bars.left,
                top = topPadding,
                right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupToolbarPosition() {
        if (Prefs.toolbarPosition != ToolbarPosition.BOTTOM) return
        binding.mainLayout.removeView(binding.studyBar)
        binding.mainLayout.addView(binding.studyBar)
        // the progress line stays between the Card and the bar
        binding.studyBar.removeView(binding.studyProgress)
        binding.studyBar.addView(binding.studyProgress, 0)
    }

    private fun applyStudyScreenVisibility() {
        val mode = viewModel.cardTemplateFullscreenMode
        val showToolbar =
            if (mode == null) Prefs.toolbarPosition != ToolbarPosition.NONE else mode == FullScreenMode.BUTTONS_AND_MENU
        val showAnswers = if (mode == null) Prefs.showAnswerButtons else mode != FullScreenMode.FULLSCREEN_ALL_GONE
        binding.studyBar.isVisible = showToolbar
        binding.answerArea.isVisible = showAnswers
        if (isBigScreen && Prefs.toolbarPosition == ToolbarPosition.BOTTOM) {
            placeTabletAnswerButtons(inToolbar = showToolbar && showAnswers)
        }
        if (mode != null) setupImmersiveMode()
    }

    private fun placeTabletAnswerButtons(inToolbar: Boolean) {
        val answerArea = binding.answerArea
        if (inToolbar == (answerArea.parent === binding.toolsLayout)) return
        if (inToolbar) {
            binding.bottomLayout.removeView(answerArea)
            binding.toolsLayout.addView(answerArea)
            answerArea.updateLayoutParams<ConstraintLayout.LayoutParams> {
                width = 0
                matchConstraintPercentWidth = 0.6F
                matchConstraintMaxWidth = 480.dp.toPx(requireContext())
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
        } else {
            binding.toolsLayout.removeView(answerArea)
            binding.bottomLayout.addView(
                answerArea,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
        }
        binding.reviewerMenuView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = if (inToolbar) 0 else ConstraintLayout.LayoutParams.WRAP_CONTENT
            matchConstraintPercentWidth = if (inToolbar) 0.16F else 1F
        }
    }

    private fun applySystemBarVisibility() {
        val barsToHide =
            when (viewModel.cardTemplateFullscreenMode) {
                FullScreenMode.BUTTONS_ONLY, FullScreenMode.FULLSCREEN_ALL_GONE -> WindowInsetsCompat.Type.systemBars()
                FullScreenMode.BUTTONS_AND_MENU -> 0
                null ->
                    when (Prefs.hideSystemBars) {
                        HideSystemBars.NONE -> 0
                        HideSystemBars.STATUS_BAR -> WindowInsetsCompat.Type.statusBars()
                        HideSystemBars.NAVIGATION_BAR -> WindowInsetsCompat.Type.navigationBars()
                        HideSystemBars.ALL -> WindowInsetsCompat.Type.systemBars()
                    }
            }
        val window = requireActivity().window
        with(WindowInsetsControllerCompat(window, window.decorView)) {
            systemBarsBehavior =
                if (barsToHide == 0) {
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                } else {
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            show(WindowInsetsCompat.Type.systemBars() and barsToHide.inv())
            if (barsToHide != 0) hide(barsToHide)
        }
    }

    private fun showTemplateNavigation(): Boolean {
        if (templateNavigationDialog?.isShowing == true) return true
        val destinations =
            arrayOf(
                CollectionManager.TR.actionsDecks(),
                CollectionManager.TR.qtMiscBrowse(),
                CollectionManager.TR.qtMiscStats(),
                getString(CommonString.settings),
                getString(CommonString.help),
                getString(CommonString.help_title_support_ankidroid),
            )
        templateNavigationDialog =
            AlertDialog.Builder(requireContext()).show {
                setItems(destinations) { _, item ->
                    when (item) {
                        0 ->
                            startActivity(
                                DeckPicker
                                    .getIntent(
                                        requireContext(),
                                    ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                            )
                        1 -> navigate(BrowserDestination.Open)
                        2 -> navigate(StatisticsDestination)
                        3 -> navigate(PreferencesDestination.Root)
                        4 -> showDialogFragment(HelpDialog.newHelpInstance())
                        5 -> {
                            val canRateApp = IntentUtil.canOpenIntent(requireContext(), AnkiDroidApp.getMarketIntent(requireContext()))
                            showDialogFragment(HelpDialog.newSupportInstance(canRateApp))
                        }
                    }
                }
            }
        return true
    }

    /**
     * Updates margins based on the possible combinations
     * of [Prefs.toolbarPosition], [Prefs.frameStyle] and `Hide answer buttons`
     */
    private fun setupMargins() {
        if (Prefs.toolbarPosition == ToolbarPosition.BOTTOM && !isBigScreen) {
            binding.bottomLayout.showDividers =
                LinearLayout.SHOW_DIVIDER_MIDDLE or LinearLayout.SHOW_DIVIDER_BEGINNING
        }
    }

    private fun setupAnswerTimer() {
        lifecycle.addObserver(viewModel.answerTimer)
        viewModel.answerTimer.state
            .onEach { state ->
                binding.timer.setup(state)
            }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupResetProgress() {
        viewModel.resetProgressFlow.collectInViewLifecycle {
            showDialogFragment(ForgetCardsDialog())
        }
        // TODO handle 'Reset progress' in the ViewModel instead of the activity, once
        //  a mechanism of showing a progress bar if the operation takes too long is implemented
        registerOnForgetHandler { listOf(viewModel.getCardId()) }
    }

    private fun setupCheckPronunciation() {
        viewModel.voiceRecorderEnabledFlow.collectInViewLifecycle { isEnabled ->
            if (isEnabled && binding.checkPronunciationContainer.getFragment<CheckPronunciationFragment?>() == null) {
                childFragmentManager.commit {
                    add(binding.checkPronunciationContainer.id, CheckPronunciationFragment())
                }
            }
            binding.checkPronunciationContainer.isVisible = isEnabled
        }
    }

    private fun setupWhiteboard() {
        childFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager,
                    f: Fragment,
                    v: View,
                    savedInstanceState: Bundle?,
                ) {
                    if (f !is WhiteboardFragment) return
                    f.setOnScrollByListener { y ->
                        webViewLayout.scrollVerticallyBy(y)
                    }
                    f.onDrawingShownChanged = { isShown -> viewModel.setWhiteboardDrawingShown(isShown) }
                    viewModel.whiteboardDrawingShownFlow.collectInViewLifecycle { isShown -> f.setDrawingShown(isShown) }
                }
            },
            false,
        )

        viewModel.whiteboardEnabledFlow.collectInViewLifecycle { isEnabled ->
            val existingFragment = whiteboardFragment
            childFragmentManager.commit {
                if (isEnabled) {
                    if (existingFragment != null) {
                        show(existingFragment)
                    } else {
                        val newFragment = WhiteboardFragment()
                        newFragment.gestureFallbackListener = { gesture -> bindingMap.onGesture(gesture) }
                        add(R.id.web_view_container, newFragment, WhiteboardFragment::class.jvmName)
                    }
                } else {
                    existingFragment?.let { hide(it) }
                }
            }
        }
    }

    private fun setupTimebox() {
        viewModel.timeBoxReachedFlow.collectInViewLifecycle { timebox ->
            Timber.i("ReviewerFragment: Timebox reached (reps %d - secs %d)", timebox.reps, timebox.secs)

            viewModel.stopAutoAdvance()

            val minutes = (timebox.secs / 60f).roundToInt()
            val message = CollectionManager.TR.studyingCardStudiedIn(timebox.reps) + " " + CollectionManager.TR.studyingMinute(minutes)

            AlertDialog.Builder(requireContext()).show {
                setTitle(CommonString.timebox_reached_title)
                setMessage(message)
                setPositiveButton(CollectionManager.TR.studyingContinue()) { _, _ ->
                    Timber.i("ReviewerFragment: Timebox 'Continue'")
                    viewModel.onPageFinished(false)
                }
                setNegativeButton(CollectionManager.TR.studyingFinish()) { _, _ ->
                    Timber.i("ReviewerFragment: Timebox 'Finish'")
                    requireActivity().finish()
                }
                setCancelable(false)
            }
        }
    }

    private fun setupActions() {
        viewModel.actionFeedbackFlow.collectInViewLifecycle { message ->
            showSnackbar(message, duration = 500)
        }

        viewModel.editNoteTagsFlow.collectInViewLifecycle { noteId ->
            if (tagsDialogFactory.show(requireActivity(), noteIds = listOf(noteId))) {
                viewModel.editingNoteId = noteId
            }
        }

        viewModel.setDueDateFlow.collectInViewLifecycle { cardId ->
            SetDueDateDialog.show(requireActivity(), listOf(cardId))
        }

        viewModel.pageUpFlow.collectInViewLifecycle {
            webViewLayout.pageUp()
        }

        viewModel.pageDownFlow.collectInViewLifecycle {
            webViewLayout.pageDown()
        }

        val repository = StudyScreenRepository()

        viewModel.isMarkedFlow.collectInViewLifecycle { isMarked ->
            if (!repository.isMarkShownInToolbar) {
                binding.markIcon.isVisible = isMarked
            }
        }
        val flagView = binding.flagIcon
        viewModel.flagFlow.collectInViewLifecycle { flag ->
            if (!repository.isFlagShownInToolbar) {
                if (flag == Flag.NONE) {
                    flagView.isVisible = false
                } else {
                    flagView.setImageDrawable(ContextCompat.getDrawable(requireContext(), flag.drawableRes))
                    flagView.isVisible = true
                }
            }
        }
    }

    override fun onSelectedTags(
        selectedTags: List<String>,
        indeterminateTags: List<String>,
        stateFilter: CardStateFilter,
    ) = viewModel.onEditedTags(selectedTags)

    override fun onCreateWebViewClient(savedInstanceState: Bundle?): CardViewerWebViewClient =
        ReviewerWebViewClient(savedInstanceState).also { reviewerWebViewClient = it }

    override fun onCreateWebChromeClient(): CardViewerWebChromeClient = ReviewerWebChromeClient()

    private inner class ReviewerWebViewClient(
        savedInstanceState: Bundle?,
    ) : CardViewerWebViewClient(savedInstanceState) {
        // Capture the fresh WebView's scale before any pinch zoom; this includes screen density.
        private val initialScale: Float = if (!isRobolectric) webViewLayout.scale else 1F
        private var scale: Float = initialScale
        private var isScrolling: Boolean = false
        private var isScrollingJob: Job? = null
        private val gestureParser by lazy {
            GestureParser(
                scope = lifecycleScope,
                isDoubleTapEnabled = bindingMap.isBound(Gesture.DOUBLE_TAP),
            )
        }

        /** Resets pinch zoom, leaving the viewport untouched when already at the initial scale. */
        fun resetZoom() {
            val factor = initialScale / scale
            if (abs(factor - 1f) < 0.001f) return
            // zoomBy is asynchronous. Keep _showQuestion/_showAnswer queued until the renderer
            // has reset its visual viewport, otherwise zoom completion can undo answer anchoring.
            val pageScale = initialScale / resources.displayMetrics.density
            webViewLayout.evaluateJavascript("ankidroid.waitForZoomReset($pageScale);")
            webViewLayout.zoomBy(factor.coerceIn(0.01f, 100f))
        }

        init {
            webViewLayout.setOnScrollChangeListener { _, _, _, _, _ ->
                isScrolling = true
                isScrollingJob?.cancel()
                isScrollingJob =
                    lifecycleScope.launch {
                        delay(300.milliseconds)
                        isScrolling = false
                    }
            }
        }

        override fun handleUrl(
            webView: WebView,
            url: Uri,
        ): Boolean {
            return when (url.scheme) {
                "gesture" -> {
                    if (isScrolling) return true
                    gestureParser.parse(url, scale, webView) { gesture ->
                        if (gesture == null) return@parse
                        Timber.v("ReviewerFragment::onGesture %s", gesture)
                        // Stylus mode passes finger input through to the WebView. Whiteboard
                        // bindings still take priority while the whiteboard is visible.
                        if (whiteboardFragment?.takeIf { it.isVisible }?.onGesture(gesture) != true) {
                            bindingMap.onGesture(gesture)
                        }
                    }
                    true
                }
                "ankidroid" -> {
                    when (url.host) {
                        "show-answer" -> viewModel.onShowAnswer()
                    }
                    true
                }
                "signal" -> {
                    when (url.schemeSpecificPart) {
                        "relinquishFocus" -> focusNativeAnswerField()
                        "reload_card_html" -> viewLifecycleOwner.lifecycleScope.launch { viewModel.onCardTemplateReload() }
                        "show_answer" -> viewLifecycleOwner.lifecycleScope.launch { viewModel.onCardTemplateAction(null) }
                        "answer_ease1", "answer_ease2", "answer_ease3", "answer_ease4" -> {
                            val method = "answerEase${url.schemeSpecificPart.last()}"
                            val rating = AnkiDroidJsAPI.answerRating(method)
                            viewLifecycleOwner.lifecycleScope.launch { viewModel.onCardTemplateAction(rating) }
                        }
                        // signal:typefocus is a legacy no-op.
                    }
                    true
                }
                else -> super.handleUrl(webView, url)
            }
        }

        override fun onScaleChanged(
            view: WebView?,
            oldScale: Float,
            newScale: Float,
        ) {
            super.onScaleChanged(view, oldScale, newScale)
            scale = newScale
        }

        override fun onPageFinished(
            view: WebView?,
            url: String?,
        ) {
            super.onPageFinished(view, url)
            Prefs.cardZoom.let {
                if (it == 100) return@let
                val scale = it / 100.0
                val script = """document.body.style.zoom = `$scale`;"""
                view?.evaluateJavascript(script, null)
            }
        }
    }

    private inner class ReviewerWebChromeClient : CardViewerWebChromeClient() {
        override fun onHideCustomView() {
            (window.decorView as FrameLayout).removeView(paramView)
            applySystemBarVisibility()
        }
    }

    companion object {
        const val RESULT_DEFAULT = 50
        const val RESULT_NO_MORE_CARDS = 52

        fun getIntent(context: Context): Intent = CardViewerActivity.getIntent(context, ReviewerFragment::class)
    }
}
