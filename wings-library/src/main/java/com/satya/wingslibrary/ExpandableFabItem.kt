package com.satya.wingslibrary

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.graphics.toColorInt

class ExpandableFabItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    // ══════════════════════════════════════════════════════
    //  Night-mode helper
    // ══════════════════════════════════════════════════════

    private val isNightMode: Boolean
        get() {
            val flags = context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
            return flags == Configuration.UI_MODE_NIGHT_YES
        }

    // ══════════════════════════════════════════════════════
    //  Default colors (auto light / dark)
    // ══════════════════════════════════════════════════════

    /**
     * Default mini-FAB background:
     *   Light mode → white  (#FFFFFF)
     *   Night mode → dark gray (#1E1E1E)
     * Overridable per item via efab_itemColor or setItemColor().
     */
    private fun defaultItemColor(): Int =
        if (isNightMode) "#1E1E1E".toColorInt() else Color.WHITE

    /**
     * Default icon tint:
     *   Light mode → black (#000000)
     *   Night mode → white (#FFFFFF)
     * Overridable per item via efab_iconTint or setIconTint().
     */
    private fun defaultIconTint(): Int =
        if (isNightMode) Color.WHITE else Color.BLACK

    /**
     * Default label chip background:
     *   Light mode → white
     *   Night mode → dark gray
     */
    private fun defaultLabelBackground(): Int =
        if (isNightMode) "#2A2A2A".toColorInt() else Color.WHITE

    /**
     * Default label text color:
     *   Light mode → near-black
     *   Night mode → near-white
     */
    private fun defaultLabelTextColor(): Int =
        if (isNightMode) "#E0E0E0".toColorInt() else "#1A1A1A".toColorInt()

    // ══════════════════════════════════════════════════════
    //  Public properties
    // ══════════════════════════════════════════════════════

    var efabIcon: Drawable? = null
        set(value) { field = value; fab.setImageDrawable(value) }

    var efabLabel: String = ""
        set(value) {
            field = value
            labelView.text = value
            refreshLabelVisibility()
        }

    var efabLabelPosition: EfabLabelPos = EfabLabelPos.AUTO

    var efabVisible: Boolean = true
        set(value) { field = value; visibility = if (value) VISIBLE else GONE }

    @ColorInt
    var efabItemColor: Int = defaultItemColor()
        set(value) { field = value; fab.backgroundTintList = ColorStateList.valueOf(value) }

    @ColorInt
    var efabIconTint: Int = defaultIconTint()
        set(value) { field = value; fab.imageTintList = ColorStateList.valueOf(value) }

    @ColorInt
    var efabLabelTextColor: Int = defaultLabelTextColor()
        set(value) { field = value; labelView.setTextColor(value) }

    @ColorInt
    var efabLabelBackground: Int = defaultLabelBackground()
        set(value) { field = value; labelCard.setCardBackgroundColor(value) }

    var efabLabelTextSizeSp: Float = 14f
        set(value) { field = value; labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, value) }

    // ══════════════════════════════════════════════════════
    //  Internal child views
    // ══════════════════════════════════════════════════════

    internal val fab = FloatingActionButton(context).apply {
        size = FloatingActionButton.SIZE_MINI
        compatElevation = dp(4f)
    }

    var fabSize: EfabSize = EfabSize.MINI
        set(value) {
            field = value
            fab.size = when (value) {
                EfabSize.MINI   -> FloatingActionButton.SIZE_MINI
                EfabSize.LARGE  -> FloatingActionButton.SIZE_AUTO
                EfabSize.NORMAL -> FloatingActionButton.SIZE_NORMAL
            }
        }

    internal val labelCard = CardView(context).apply {
        cardElevation = dp(3f)
        radius = dp(6f)
        useCompatPadding = true
        visibility = GONE
    }

    private val labelView = TextView(context).apply {
        setPadding(dp(10f).toInt(), dp(5f).toInt(), dp(10f).toInt(), dp(5f).toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        gravity = Gravity.CENTER_VERTICAL
        maxLines = 1
    }

    // ══════════════════════════════════════════════════════
    //  State injected by parent ExpandableFab
    // ══════════════════════════════════════════════════════

    internal var resolvedLabelPos: EfabLabelPos = EfabLabelPos.LEFT
        set(value) {
            if (field == value) return
            field = value
            refreshLabelVisibility()
            requestLayout()
        }

    internal var onItemClickListener: ((ExpandableFabItem) -> Unit)? = null

    // ══════════════════════════════════════════════════════
    //  Init
    // ══════════════════════════════════════════════════════

    init {
        setBackgroundColor(Color.TRANSPARENT)

        fab.backgroundTintList = ColorStateList.valueOf(defaultItemColor())
        fab.imageTintList      = ColorStateList.valueOf(defaultIconTint())
        labelCard.setCardBackgroundColor(defaultLabelBackground())
        labelView.setTextColor(defaultLabelTextColor())

        labelCard.addView(
            labelView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )
        addView(fab,       LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(labelCard, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        fab.setOnClickListener { onItemClickListener?.invoke(this) }
        labelCard.setOnClickListener { onItemClickListener?.invoke(this) }

        attrs?.let { parseAttrs(it) }
    }

    // ══════════════════════════════════════════════════════
    //  XML attribute parsing
    // ══════════════════════════════════════════════════════

    @SuppressLint("CustomViewStyleable")
    private fun parseAttrs(set: AttributeSet) {
        val ta = context.obtainStyledAttributes(set, R.styleable.WingsChildFab)
        try {
            fabSize = EfabSize.entries.toTypedArray()[ta.getInt(R.styleable.WingsChildFab_childFabSize, 0)]
            
            ta.getDrawable(R.styleable.WingsChildFab_efab_icon)
                ?.let { efabIcon = it }

            ta.getString(R.styleable.WingsChildFab_efab_label)
                ?.let { efabLabel = it }

            efabLabelPosition = EfabLabelPos.entries.toTypedArray()[
                ta.getInt(R.styleable.WingsChildFab_efab_labelPosition, 0)
            ]

            // Only override color if explicitly set in XML
            ta.getColor(R.styleable.WingsChildFab_efab_itemColor, Int.MIN_VALUE)
                .takeIf { it != Int.MIN_VALUE }?.let { efabItemColor = it }

            ta.getColor(R.styleable.WingsChildFab_efab_iconTint, Int.MIN_VALUE)
                .takeIf { it != Int.MIN_VALUE }?.let { efabIconTint = it }

            ta.getColor(R.styleable.WingsChildFab_efab_labelTextColor, Int.MIN_VALUE)
                .takeIf { it != Int.MIN_VALUE }?.let { efabLabelTextColor = it }

            ta.getColor(R.styleable.WingsChildFab_efab_labelBackground, Int.MIN_VALUE)
                .takeIf { it != Int.MIN_VALUE }?.let { efabLabelBackground = it }

            ta.getDimension(R.styleable.WingsChildFab_efab_labelTextSize, -1f)
                .takeIf { it > 0f }
                ?.let { efabLabelTextSizeSp = it / resources.displayMetrics.density }

            efabVisible = ta.getBoolean(R.styleable.WingsChildFab_efab_visible, true)
        } finally {
            ta.recycle()
        }
    }

    // ══════════════════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════════════════

    fun setIcon(@DrawableRes res: Int)       { efabIcon = ContextCompat.getDrawable(context, res) }
    fun setIcon(drawable: Drawable?)         { efabIcon = drawable }
    fun setLabel(text: String)               { efabLabel = text }
    fun setItemColor(@ColorInt c: Int)       { efabItemColor = c }
    fun setIconTint(@ColorInt c: Int)        { efabIconTint = c }
    fun setLabelTextColor(@ColorInt c: Int)  { efabLabelTextColor = c }
    fun setLabelBackground(@ColorInt c: Int) { efabLabelBackground = c }
    fun setLabelTextSizeSp(sp: Float)        { efabLabelTextSizeSp = sp }
    fun show() { efabVisible = true }
    fun hide() { efabVisible = false }

    // ══════════════════════════════════════════════════════
    //  Internal helpers
    // ══════════════════════════════════════════════════════

    private fun refreshLabelVisibility() {
        labelCard.visibility =
            if (efabLabel.isNotEmpty() && resolvedLabelPos != EfabLabelPos.HIDDEN)
                VISIBLE else GONE
    }

    // ══════════════════════════════════════════════════════
    //  Measure
    // ══════════════════════════════════════════════════════

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChild(fab,       widthMeasureSpec, heightMeasureSpec)
        measureChild(labelCard, widthMeasureSpec, heightMeasureSpec)

        val labelShown = efabLabel.isNotEmpty() && resolvedLabelPos != EfabLabelPos.HIDDEN
        val fw = fab.measuredWidth;  val fh = fab.measuredHeight
        val lw = if (labelShown) labelCard.measuredWidth  else 0
        val lh = if (labelShown) labelCard.measuredHeight else 0
        val gap = if (labelShown) dp(8f).toInt() else 0

        val totalW: Int
        val totalH: Int
        when {
            !labelShown -> { totalW = fw; totalH = fh }
            resolvedLabelPos == EfabLabelPos.LEFT ||
                    resolvedLabelPos == EfabLabelPos.RIGHT -> {
                totalW = fw + gap + lw; totalH = maxOf(fh, lh)
            }
            else -> { totalW = maxOf(fw, lw); totalH = fh + gap + lh }
        }
        setMeasuredDimension(resolveSize(totalW, widthMeasureSpec),
            resolveSize(totalH, heightMeasureSpec))
    }

    // ══════════════════════════════════════════════════════
    //  Layout
    // ══════════════════════════════════════════════════════

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val labelShown = efabLabel.isNotEmpty() && resolvedLabelPos != EfabLabelPos.HIDDEN
        val fw = fab.measuredWidth;  val fh = fab.measuredHeight
        val lw = labelCard.measuredWidth; val lh = labelCard.measuredHeight
        val gap = dp(8f).toInt()
        val mw = measuredWidth; val mh = measuredHeight

        if (!labelShown) { fab.layout(0, 0, fw, fh); labelCard.layout(0,0,0,0); return }

        when (resolvedLabelPos) {
            EfabLabelPos.LEFT -> {
                val lt = (mh - lh) / 2; labelCard.layout(0, lt, lw, lt + lh)
                val ft = (mh - fh) / 2; fab.layout(lw + gap, ft, lw + gap + fw, ft + fh)
            }
            EfabLabelPos.RIGHT -> {
                val ft = (mh - fh) / 2; fab.layout(0, ft, fw, ft + fh)
                val lt = (mh - lh) / 2; labelCard.layout(fw + gap, lt, fw + gap + lw, lt + lh)
            }
            EfabLabelPos.TOP -> {
                val ll = (mw - lw) / 2; labelCard.layout(ll, 0, ll + lw, lh)
                val fl = (mw - fw) / 2; fab.layout(fl, lh + gap, fl + fw, lh + gap + fh)
            }
            EfabLabelPos.BOTTOM -> {
                val fl = (mw - fw) / 2; fab.layout(fl, 0, fl + fw, fh)
                val ll = (mw - lw) / 2; labelCard.layout(ll, fh + gap, ll + lw, fh + gap + lh)
            }
            else -> {
                val lt = (mh - lh) / 2; labelCard.layout(0, lt, lw, lt + lh)
                val ft = (mh - fh) / 2; fab.layout(lw + gap, ft, lw + gap + fw, ft + fh)
            }
        }
    }

    // ══════════════════════════════════════════════════════
    //  Utility
    // ══════════════════════════════════════════════════════

    internal fun dp(v: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
}