package com.everlog.ui.views

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.everlog.R

/**
 * The XML version of the Compose design system's AppFooter: actions pinned to the bottom of the
 * screen in a floating card, inset by the screen margin and kept above the navigation bar, with the
 * cards' corners and a hairline border, blurring the content scrolling behind it.
 *
 * Its children go in the card, stacked vertically, and bring their own margins (16dp, as the XML
 * buttons have), which make the card's padding.
 */
class FloatingFooterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val content: LinearLayout

    init {
        inflate(context, R.layout.view_floating_footer, this)
        val card = findViewById<View>(R.id.floatingFooterCard)
        val radius = resources.getDimension(R.dimen.card_radius_large)
        card.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        card.clipToOutline = true
        content = findViewById(R.id.floatingFooterContent)

        val margin = resources.getDimensionPixelSize(R.dimen.activity_margin)
        val bottomMargin = resources.getDimensionPixelSize(R.dimen.activity_margin_half)
        updatePadding(left = margin, right = margin, bottom = bottomMargin)
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            updatePadding(bottom = navigationBars.bottom + bottomMargin)
            insets
        }
    }

    // Children from the layout go in the card
    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (child.id == R.id.floatingFooterCard) {
            super.addView(child, index, params)
        } else {
            // The layout inflates them with this view's FrameLayout params, keep their gravity in
            // the card's LinearLayout
            val contentParams = when (params) {
                is FrameLayout.LayoutParams -> LinearLayout.LayoutParams(params).also { it.gravity = params.gravity }
                else -> params
            }
            content.addView(child, index, contentParams)
        }
    }
}
