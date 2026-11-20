// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.setMargins
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputLayout
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.cardviewer.TypeAnswerWebViewLayout
import com.ichi2.anki.preferences.reviewer.ReviewerMenuView
import com.ichi2.ui.TypeAnswerEditText
import kotlin.math.roundToInt
import com.ichi2.anki.common.android.R as CommonR

/** Places the Compose study controls around the card's WebView and its Android input connection. */
internal class ReviewerLayout(
    context: Context,
) : CoordinatorLayout(context) {
    val mainLayout = column(R.id.main_layout)
    val studyBar = column(R.id.study_bar)
    val toolsLayout = ConstraintLayout(context).apply { id = R.id.tools_layout }
    val backButton = ComposeView(context).apply { id = R.id.back_button }
    val studyBarTitles = ComposeView(context).apply { id = R.id.study_bar_titles }
    val timer =
        AnswerTimerView(context, null).apply {
            id = R.id.timer
            setTextAppearance(CommonR.style.TextAppearance_Foundation_Caption)
            fontFeatureSettings = "tnum"
            visibility = GONE
        }
    val reviewerMenuView =
        ReviewerMenuView(context).apply {
            id = R.id.reviewer_menu_view
        }
    val studyProgress = ComposeView(context).apply { id = R.id.study_progress }
    val webViewContainer = card(R.id.web_view_container).apply { setCardBackgroundColor(color(CommonR.attr.foundationStudyCanvas)) }
    val webViewLayout = TypeAnswerWebViewLayout(context, null).apply { id = R.id.web_view_layout }
    val cardIndicators = LinearLayout(context).apply { id = R.id.card_indicators }
    val flagIcon =
        AppCompatImageView(context).apply {
            id = R.id.flag_icon
            visibility = GONE
        }
    val markIcon =
        AppCompatImageView(context).apply {
            id = R.id.mark_icon
            visibility = GONE
            setImageResource(R.drawable.symbol_star)
            imageTintList = ColorStateList.valueOf(color(CommonR.attr.foundationEmphasis))
        }
    val answerFeedback =
        AnswerFeedbackView(context).apply {
            id = R.id.answer_feedback
            visibility = GONE
        }
    val bottomLayout = column(R.id.bottom_layout)
    val typeAnswerContainer = card(R.id.type_answer_container).apply { visibility = GONE }
    val typeAnswerEditText =
        TypeAnswerEditText(context, null).apply {
            id = R.id.type_answer_edit_text
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_DONE
            setTextAppearance(CommonR.style.TextAppearance_Foundation_Lead)
            setTextColor(color(CommonR.attr.foundationOnBackground))
        }
    val checkPronunciationContainer =
        FragmentContainerView(context).apply {
            id = R.id.check_pronunciation_container
            clipChildren = false
            elevation = dp(2).toFloat()
            visibility = GONE
        }
    val answerArea = ComposeView(context).apply { id = R.id.answer_area }

    init {
        id = R.id.root_layout
        setBackgroundColor(color(CommonR.attr.foundationStudyCanvas))
        fitsSystemWindows = true
        isFocusableInTouchMode = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) defaultFocusHighlightEnabled = false
        mainLayout.clipChildren = false
        addView(mainLayout, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        mainLayout.addView(studyBar, MATCH_PARENT, WRAP_CONTENT)
        addStudyBar()
        addCard()
        addAnswerControls()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val available = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            centreWithin(webViewContainer.layoutParams as LinearLayout.LayoutParams, available, maxWidthDp = 820)
            centreWithin(bottomLayout.layoutParams as LinearLayout.LayoutParams, available, maxWidthDp = 640)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun centreWithin(params: LinearLayout.LayoutParams, available: Int, maxWidthDp: Int) {
        params.width = (available - params.marginStart - params.marginEnd).coerceIn(0, dp(maxWidthDp))
        params.gravity = Gravity.CENTER_HORIZONTAL
    }

    private fun addStudyBar() {
        val parent = ConstraintLayout.LayoutParams.PARENT_ID
        toolsLayout.minimumHeight = dp(56)
        toolsLayout.setPadding(0, 0, 0, dp(6))
        studyBar.addView(
            toolsLayout,
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            },
        )

        fun centered(
            width: Int,
            height: Int = WRAP_CONTENT,
        ) = ConstraintLayout.LayoutParams(width, height).apply {
            topToTop = parent
            bottomToBottom = parent
        }
        toolsLayout.addView(
            backButton,
            centered(dp(48), dp(48)).apply {
                startToStart = parent
                marginStart = dp(4)
            },
        )
        toolsLayout.addView(
            studyBarTitles,
            centered(0).apply {
                startToEnd = backButton.id
                endToStart = timer.id
            },
        )
        toolsLayout.addView(
            timer,
            centered(WRAP_CONTENT).apply {
                endToStart = reviewerMenuView.id
                marginStart = dp(6)
                marginEnd = dp(6)
            },
        )
        toolsLayout.addView(reviewerMenuView, centered(WRAP_CONTENT).apply { endToEnd = parent })
        studyBar.addView(studyProgress, MATCH_PARENT, WRAP_CONTENT)
    }

    private fun addCard() {
        val margin = resources.getDimensionPixelSize(CommonR.dimen.foundation_spacing_study_margin)
        mainLayout.addView(
            webViewContainer,
            LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f).apply {
                topMargin = dp(14)
                marginStart = margin
                marginEnd = margin
            },
        )
        webViewContainer.addView(webViewLayout, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        webViewContainer.addView(cardIndicators, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(dp(6)) })
        val icon = resources.getDimensionPixelSize(R.dimen.reviewer_mark_flag)
        cardIndicators.addView(flagIcon, LinearLayout.LayoutParams(icon, icon).apply { marginEnd = dp(2) })
        cardIndicators.addView(markIcon, icon, icon)
        cardIndicators.addView(Space(context), LinearLayout.LayoutParams(0, 0, 1f))
        cardIndicators.addView(answerFeedback, icon, icon)
    }

    private fun addAnswerControls() {
        val margin = resources.getDimensionPixelSize(CommonR.dimen.foundation_spacing_study_margin)
        mainLayout.addView(
            bottomLayout,
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                marginStart = margin
                marginEnd =
                    margin
            },
        )
        bottomLayout.elevation = dp(1).toFloat()
        bottomLayout.clipChildren = false
        bottomLayout.dividerDrawable = context.getDrawable(R.drawable.vertical_spacer_8dp)
        bottomLayout.showDividers = LinearLayout.SHOW_DIVIDER_BEGINNING or LinearLayout.SHOW_DIVIDER_MIDDLE or LinearLayout.SHOW_DIVIDER_END
        bottomLayout.addView(typeAnswerContainer, MATCH_PARENT, WRAP_CONTENT)
        val hintInk = ColorStateList.valueOf(color(CommonR.attr.foundationOnBackgroundMuted))
        val field =
            TextInputLayout(context).apply {
                setPadding(0, dp(4), 0, 0)
                hint = context.getString(CommonString.type_answer_hint)
                defaultHintTextColor = hintInk
                setHintTextAppearance(CommonR.style.TextAppearance_Foundation_Caption)
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_FILLED
                boxBackgroundColor = Color.TRANSPARENT
                boxStrokeWidth = 0
                boxStrokeWidthFocused = 0
                endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                setEndIconDrawable(CommonR.drawable.symbol_close)
                setEndIconTintList(hintInk)
                addView(typeAnswerEditText, MATCH_PARENT, WRAP_CONTENT)
            }
        typeAnswerContainer.addView(field, MATCH_PARENT, WRAP_CONTENT)
        bottomLayout.addView(
            checkPronunciationContainer,
            LinearLayout.LayoutParams(MATCH_PARENT, dp(56)).apply {
                gravity =
                    Gravity.CENTER_HORIZONTAL
            },
        )
        bottomLayout.addView(answerArea, MATCH_PARENT, WRAP_CONTENT)
    }

    private fun card(viewId: Int) =
        MaterialCardView(context).apply {
            id = viewId
            radius = resources.getDimension(CommonR.dimen.foundation_shape_card_sheet)
            cardElevation = 0f
            setCardBackgroundColor(color(CommonR.attr.foundationBackground))
            strokeWidth = resources.getDimensionPixelSize(CommonR.dimen.foundation_shape_hairline)
            strokeColor = color(CommonR.attr.foundationOutline)
        }

    private fun column(viewId: Int) =
        LinearLayout(context).apply {
            id = viewId
            orientation = LinearLayout.VERTICAL
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    private fun color(attr: Int) = MaterialColors.getColor(context, attr, "Reviewer")
}
