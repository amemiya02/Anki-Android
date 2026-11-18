// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.preferences

import android.content.Context
import android.util.AttributeSet
import com.ichi2.anki.R
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.reviewer.Binding
import com.ichi2.anki.reviewer.CardSide
import com.ichi2.anki.reviewer.MappableBinding
import com.ichi2.anki.reviewer.MappableBinding.Companion.toPreferenceString
import com.ichi2.anki.reviewer.ReviewerBinding
import com.ichi2.anki.utils.ext.usingStyledAttributes

open class ReviewerControlPreference(
    context: Context,
    attrs: AttributeSet? = null,
) : ControlPreference(context, attrs) {
    open var side: CardSide? =
        context.usingStyledAttributes(attrs, R.styleable.ReviewerControlPreference) {
            when (getInt(R.styleable.ReviewerControlPreference_cardSide, -1)) {
                0 -> CardSide.QUESTION
                1 -> CardSide.ANSWER
                2 -> CardSide.BOTH
                else -> null
            }
        }
        protected set

    override fun displayBinding(binding: MappableBinding): String =
        if (side != null) binding.binding.toDisplayString(context) else super.displayBinding(binding)

    override val areGesturesEnabled: Boolean
        get() = true

    override fun getMappableBindings(): List<ReviewerBinding> = ReviewerBinding.fromPreferenceString(value).toList()

    @Suppress("UNCHECKED_CAST")
    override fun getRelatedPreferences(): List<ReviewerControlPreference> =
        relatedSettings
            .filter {
                it::class == ReviewerControlPreference::class
            } as List<ReviewerControlPreference>

    override fun getPreferenceAssignedTo(binding: Binding): ControlPreference? {
        val cardSide = side ?: return super.getPreferenceAssignedTo(binding)
        val reviewerBinding = ReviewerBinding(binding, cardSide)
        // Bindings only conflict when the card sides overlap
        return getPreferencesAssignedTo(reviewerBinding).firstOrNull()
    }

    @NeedsTest("Ensure correct preferences are returned for side-specific binding")
    private fun getPreferencesAssignedTo(binding: ReviewerBinding): List<ReviewerControlPreference> =
        getRelatedPreferences().filter { preference -> binding in preference.getMappableBindings() }

    fun addBinding(
        binding: Binding,
        side: CardSide,
    ) {
        val newBinding = ReviewerBinding(binding, side)
        // Before adding new binding, remove all conflicting bindings
        getPreferencesAssignedTo(newBinding).forEach { preference ->
            preference.removeDuplicateBindings(newBinding)
        }
        val bindings = ReviewerBinding.fromPreferenceString(value).toMutableList()
        bindings.add(newBinding)
        value = bindings.toPreferenceString()
    }

    @NeedsTest("Check dup removal, including partial side overlap: e.g. QUESTION & BOTH")
    private fun removeDuplicateBindings(binding: ReviewerBinding) {
        val bindings = ReviewerBinding.fromPreferenceString(value).toMutableList()
        bindings.removeAll { it == binding } // Uses overridden .equals() to detect overlaps
        value = bindings.toPreferenceString()
    }
}
