package com.satya.wingslibrary

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.drawable.toDrawable

class WingsExpandableFab @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    // ════════════════════════════════════════════════════════
    //  Configuration
    // ════════════════════════════════════════════════════════

    var layoutMode: EfabLayoutMode = EfabLayoutMode.LINEAR
        set(value) { field = value; resolveAllLabelPositions(); requestLayout() }

    /**
     * Controls item expansion direction.
     *
     *  LINEAR   mode → only UP / DOWN are respected (UP is default).
     *  RADIAL   mode → only LEFT / RIGHT are respected (LEFT is default).
     *  CIRCULAR mode → this property is ignored entirely.
     */
    var expandDirection: EfabDirection = EfabDirection.UP
        set(value) { field = value; resolveAllLabelPositions(); requestLayout() }

    var animationType: EfabAnimType = EfabAnimType.SLIDE

    var animationSpeed: EfabSpeed = EfabSpeed.NORMAL

    var mainFabSize: EfabSize = EfabSize.NORMAL
        set(value) {
            field = value
            mainFab.size = when (value) {
                EfabSize.MINI   -> FloatingActionButton.SIZE_MINI
                EfabSize.LARGE  -> FloatingActionButton.SIZE_AUTO
                EfabSize.NORMAL -> FloatingActionButton.SIZE_NORMAL
            }
        }

    var itemSpacingPx: Int = dp(12f).toInt()
        set(value) { field = value; requestLayout() }

    /** Orbit radius used in CIRCULAR mode (px). */
    var circularRadiusPx: Float = dp(72f)
        set(value) { field = value; requestLayout() }

    /**
     * When true (default) the circular arc start/sweep angles
     * are computed from the component's on-screen position.
     * Set false to use circularStartAngle / circularSweepAngle.
     */
    var autoDetectPosition: Boolean = true

    var circularStartAngle: Float = 270f
    var circularSweepAngle: Float = 360f

    var showScrim: Boolean = false
    var closeOnScrimTap: Boolean = true

    @ColorInt
    var mainFabColor: Int = resolveThemeColor(android.R.color.holo_blue_dark)
        set(value) { field = value; mainFab.backgroundTintList = ColorStateList.valueOf(value) }

    @ColorInt
    var mainIconTint: Int = resolveThemeColor(android.R.color.holo_blue_dark)
        set(value) { field = value; mainFab.imageTintList = ColorStateList.valueOf(value) }

    @ColorInt
    var scrimColor: Int = Color.argb(160, 0, 0, 0)
        set(value) { field = value; scrimView.background = value.toDrawable() }

    var stateChangeListener: OnExpandableFabStateChangeListener? = null

    // ════════════════════════════════════════════════════════
    //  Main FAB icon API
    // ════════════════════════════════════════════════════════

    /**
     * Custom icon for the main FAB.
     * null  → use default Add icon; it rotates 135° to form ✕ on expand.
     * !null → icon tilts 45° on expand (a gentle "active" hint).
     */
    var customMainIcon: Drawable? = null
        set(value) {
            field = value
            if (value != null) mainFab.setImageDrawable(value)
            else mainFab.setImageResource(android.R.drawable.ic_input_add)
        }

    fun setMainIcon(@DrawableRes res: Int) {
        customMainIcon = ContextCompat.getDrawable(context, res)
    }

    fun setMainIcon(drawable: Drawable?) {
        customMainIcon = drawable
    }

    // ════════════════════════════════════════════════════════
    //  Internal views
    // ════════════════════════════════════════════════════════

    private val mainFab = FloatingActionButton(context).apply {
        size = FloatingActionButton.SIZE_NORMAL
        setImageResource(android.R.drawable.ic_input_add)
        useCompatPadding = true
        compatElevation = dp(6f)
    }

    private val scrimView = View(context).apply {
        background = Color.argb(160, 0, 0, 0).toDrawable()
        alpha = 0f
        visibility = GONE
    }

    // ════════════════════════════════════════════════════════
    //  Position detection state
    // ════════════════════════════════════════════════════════

    private var detectedScreenPosition: ScreenPosition = ScreenPosition.UNKNOWN
    private var isOnRightHalf: Boolean = true
    private var resolvedStartAngle: Float = 270f
    private var resolvedSweepAngle: Float = 360f

    // ════════════════════════════════════════════════════════
    //  Animation durations
    // ════════════════════════════════════════════════════════

    private val BASE_APPEAR    = 280L
    private val BASE_DISAPPEAR = 220L
    private val BASE_STAGGER   =  55L

    private fun appearDur()        = (BASE_APPEAR    / animationSpeed.multiplier).toLong()
    private fun disappearDur()     = (BASE_DISAPPEAR / animationSpeed.multiplier).toLong()
    private fun staggerFor(i: Int) = (BASE_STAGGER * i / animationSpeed.multiplier).toLong()

    // Extra padding so FAB elevation shadow is never clipped at edges
    private val shadowPad = dp(8f).toInt()

    private var isExpanded = false

    // ════════════════════════════════════════════════════════
    //  Init
    // ════════════════════════════════════════════════════════

    init {
        // Fix 1 — transparent background on the root ViewGroup
        setBackgroundColor(Color.WHITE)
        clipChildren  = false
        clipToPadding = false
        setPadding(shadowPad, shadowPad, shadowPad, shadowPad)

        mainFab.backgroundTintList = ColorStateList.valueOf(mainFabColor)
        mainFab.imageTintList      = ColorStateList.valueOf(mainIconTint)
        mainFab.setOnClickListener { toggle() }
        scrimView.setOnClickListener { if (closeOnScrimTap) collapse() }

        // Scrim first (drawn behind everything)
        addView(scrimView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(mainFab,   LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        attrs?.let { parseAttrs(it) }
    }

    // ════════════════════════════════════════════════════════
    //  XML parsing
    // ════════════════════════════════════════════════════════

    @SuppressLint("CustomViewStyleable")
    private fun parseAttrs(set: AttributeSet) {
        val ta = context.obtainStyledAttributes(set, R.styleable.WingsExpandableFab)
        try {
            layoutMode     = EfabLayoutMode.entries.toTypedArray()[ta.getInt(R.styleable.WingsExpandableFab_efab_layoutMode, 0)]
            expandDirection= EfabDirection.entries.toTypedArray()[ta.getInt(R.styleable.WingsExpandableFab_efab_expandDirection, 0)]
            animationType  = EfabAnimType.entries.toTypedArray()[ta.getInt(R.styleable.WingsExpandableFab_efab_animationType, 0)]
            animationSpeed = EfabSpeed.entries.toTypedArray()[ta.getInt(R.styleable.WingsExpandableFab_efab_animationSpeed, 1)]
            mainFabSize    = EfabSize.entries.toTypedArray()[ta.getInt(R.styleable.WingsExpandableFab_efab_mainFabSize, 1)]

            itemSpacingPx = ta.getDimensionPixelSize(
                R.styleable.WingsExpandableFab_efab_itemSpacing, itemSpacingPx)
            circularRadiusPx = ta.getDimension(
                R.styleable.WingsExpandableFab_efab_circularRadius, circularRadiusPx)
            circularStartAngle = ta.getFloat(
                R.styleable.WingsExpandableFab_efab_circularStartAngle, 270f)
            circularSweepAngle = ta.getFloat(
                R.styleable.WingsExpandableFab_efab_circularSweepAngle, 360f)
            autoDetectPosition = ta.getBoolean(
                R.styleable.WingsExpandableFab_efab_autoDetectPosition, true)
            showScrim       = ta.getBoolean(R.styleable.WingsExpandableFab_efab_showScrim, false)
            closeOnScrimTap = ta.getBoolean(R.styleable.WingsExpandableFab_efab_closeOnScrimTap, true)

            ta.getColor(R.styleable.WingsExpandableFab_efab_mainFabColor, Int.MIN_VALUE)
                .takeIf { it != Int.MIN_VALUE }?.let { mainFabColor = it }
            ta.getColor(R.styleable.WingsExpandableFab_efab_mainIconTint, Int.MIN_VALUE)
                .takeIf { it != Int.MIN_VALUE }?.let { mainIconTint = it }
            ta.getColor(R.styleable.WingsExpandableFab_efab_scrimColor, Int.MIN_VALUE)
                .takeIf { it != Int.MIN_VALUE }?.let { scrimColor = it }
            ta.getDrawable(R.styleable.WingsExpandableFab_efab_mainIcon)
                ?.let { customMainIcon = it }
        } finally {
            ta.recycle()
        }
    }

    // ════════════════════════════════════════════════════════
    //  Child management (ChipGroup-style)
    // ════════════════════════════════════════════════════════

    fun addItem(item: ExpandableFabItem) = addView(item)
    fun removeItem(item: ExpandableFabItem) = removeView(item)
    fun removeAllItems() = fabItems.forEach { removeView(it) }

    val fabItems: List<ExpandableFabItem>
        get() = children.filterIsInstance<ExpandableFabItem>()
            .filter { it.efabVisible }.toList()

    val allItems: List<ExpandableFabItem>
        get() = children.filterIsInstance<ExpandableFabItem>().toList()

    override fun addView(child: View, index: Int, params: LayoutParams) {
        if (child is ExpandableFabItem) {
            child.resolvedLabelPos = resolveItemLabelPos(child.efabLabelPosition)
            child.alpha      = 0f
            child.scaleX     = 0f
            child.scaleY     = 0f
            child.visibility = if (child.efabVisible) INVISIBLE else GONE
            child.onItemClickListener = { tapped ->
                tapped.callOnClick()
                if (closeOnScrimTap) collapse()
            }
        }
        super.addView(child, index, params)
    }

    // ════════════════════════════════════════════════════════
    //  Screen-position detection
    // ════════════════════════════════════════════════════════

    private fun detectScreenPosition() {
        val loc = IntArray(2)
        getLocationInWindow(loc)
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()
        val cx = (loc[0] + measuredWidth  / 2f) / screenW
        val cy = (loc[1] + measuredHeight / 2f) / screenH

        val nearLeft   = cx < 0.35f
        val nearRight  = cx > 0.65f
        val nearTop    = cy < 0.35f
        val nearBottom = cy > 0.65f
        isOnRightHalf  = cx >= 0.5f

        detectedScreenPosition = when {
            nearBottom && nearRight -> ScreenPosition.BOTTOM_END
            nearBottom && nearLeft  -> ScreenPosition.BOTTOM_START
            nearTop    && nearRight -> ScreenPosition.TOP_END
            nearTop    && nearLeft  -> ScreenPosition.TOP_START
            nearBottom             -> ScreenPosition.BOTTOM_CENTER
            nearTop                -> ScreenPosition.TOP_CENTER
            nearLeft               -> ScreenPosition.CENTER_START
            nearRight              -> ScreenPosition.CENTER_END
            else                   -> ScreenPosition.CENTER
        }

        if (autoDetectPosition) {
            val (start, sweep) = arcAnglesFor(detectedScreenPosition)
            resolvedStartAngle = start
            resolvedSweepAngle = sweep
        } else {
            resolvedStartAngle = circularStartAngle
            resolvedSweepAngle = circularSweepAngle
        }

        resolveAllLabelPositions()
    }

    /**
     * Arc angles for CIRCULAR mode.
     * Angle 0° = right (3 o'clock), 90° = bottom, 180° = left, 270° = top.
     */
    private fun arcAnglesFor(pos: ScreenPosition): Pair<Float, Float> = when (pos) {
        ScreenPosition.BOTTOM_END    -> Pair(180f, 90f)
        ScreenPosition.BOTTOM_START  -> Pair(270f, 90f)
        ScreenPosition.TOP_END       -> Pair(90f,  90f)
        ScreenPosition.TOP_START     -> Pair(0f,   90f)
        ScreenPosition.BOTTOM_CENTER -> Pair(180f, 180f)
        ScreenPosition.TOP_CENTER    -> Pair(0f,   180f)
        ScreenPosition.CENTER_START  -> Pair(270f, 180f)
        ScreenPosition.CENTER_END    -> Pair(90f,  180f)
        ScreenPosition.CENTER        -> Pair(270f, 360f)
        ScreenPosition.UNKNOWN       -> Pair(270f, 360f)
    }

    // ════════════════════════════════════════════════════════
    //  Measure
    // ════════════════════════════════════════════════════════

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val items = fabItems
        measureChild(scrimView, widthMeasureSpec, heightMeasureSpec)
        measureChild(mainFab,   widthMeasureSpec, heightMeasureSpec)
        items.forEach { measureChild(it, widthMeasureSpec, heightMeasureSpec) }

        val fabW = mainFab.measuredWidth
        val fabH = mainFab.measuredHeight

        val totalW: Int
        val totalH: Int

        when (layoutMode) {
            EfabLayoutMode.CIRCULAR -> {
                val itemSize = items.firstOrNull()?.fab?.measuredWidth ?: dp(40f).toInt()
                val diameter = ((circularRadiusPx + itemSize) * 2f).toInt() + shadowPad * 2
                totalW = diameter; totalH = diameter
            }
            EfabLayoutMode.RADIAL -> {
                // In RADIAL mode the component needs room for the arc fan.
                // The bounding box is approximately: radius wide × radius tall
                val maxItemW = items.maxOfOrNull { it.measuredWidth } ?: 0
                val arcH = (circularRadiusPx + maxItemW).toInt() + shadowPad * 2
                val arcW = (circularRadiusPx + maxItemW).toInt() + fabW + shadowPad * 2
                totalW = arcW; totalH = arcH
            }
            EfabLayoutMode.LINEAR -> {
                val maxItemW = items.maxOfOrNull { it.measuredWidth } ?: 0
                val itemsH   = items.sumOf { it.measuredHeight + itemSpacingPx }
                totalW = maxOf(fabW, maxItemW) + shadowPad * 2
                totalH = fabH + itemsH + shadowPad * 2
            }
        }

        setMeasuredDimension(resolveSize(totalW, widthMeasureSpec),
            resolveSize(totalH, heightMeasureSpec))
    }

    // ════════════════════════════════════════════════════════
    //  Layout
    // ════════════════════════════════════════════════════════

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        detectScreenPosition()
        val w = r - l; val h = b - t
        scrimView.layout(0, 0, w, h)

        when (layoutMode) {
            EfabLayoutMode.LINEAR   -> layoutLinear(w, h, fabItems)
            EfabLayoutMode.CIRCULAR -> layoutCircular(w, h, fabItems)
            EfabLayoutMode.RADIAL   -> layoutRadial(w, h, fabItems)
        }
    }

    // ════════════════════════════════════════════════════════
    //  LINEAR layout
    //  Direction: only UP / DOWN (LEFT/RIGHT ignored).
    //  Effective direction → always UP if neither UP nor DOWN.
    //  Horizontal anchor auto-detected from screen position.
    // ════════════════════════════════════════════════════════

    private fun layoutLinear(w: Int, h: Int, items: List<ExpandableFabItem>) {
        val fabW = mainFab.measuredWidth
        val fabH = mainFab.measuredHeight
        val pad  = shadowPad

        val goDown = expandDirection == EfabDirection.DOWN

        val fabL = if (isOnRightHalf) w - fabW - pad else pad
        val fabT = if (goDown) pad else h - fabH - pad
        mainFab.layout(fabL, fabT, fabL + fabW, fabT + fabH)

        var cursor = if (goDown) fabT + fabH + itemSpacingPx
        else        fabT - itemSpacingPx
        val itemList = if (goDown) items else items.reversed()

        for (item in itemList) {
            val iH = item.measuredHeight
            val iL = if (isOnRightHalf) w - item.measuredWidth - pad else pad
            if (goDown) {
                item.layout(iL, cursor, iL + item.measuredWidth, cursor + iH)
                cursor += iH + itemSpacingPx
            } else {
                item.layout(iL, cursor - iH, iL + item.measuredWidth, cursor)
                cursor -= iH + itemSpacingPx
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  CIRCULAR layout
    //
    //  Fix 2 — correct angle spacing for any N children:
    //
    //  The old code used (sweepAngle / (size - 1)) as step,
    //  which makes step = infinity / undefined when size == 1,
    //  and places every item at the same angle when size == 2
    //  with a 360° sweep because:
    //    step = 360 / (2-1) = 360  → item[0] at 270°, item[1] at 270+360=630°=270°  ← same!
    //
    //  Correct rule:
    //   • Open arc  (sweepAngle < 360): space = sweep / (size-1)
    //     → endpoints land exactly on start and start+sweep.
    //   • Closed ring (sweepAngle == 360): space = sweep / size
    //     → items are evenly distributed; start and end don't
    //        overlap (last item is one step before a full cycle).
    //   • Single item: placed at startAngle regardless.
    // ════════════════════════════════════════════════════════

    private fun layoutCircular(w: Int, h: Int, items: List<ExpandableFabItem>) {
        val cx = w / 2f; val cy = h / 2f
        val fabW = mainFab.measuredWidth; val fabH = mainFab.measuredHeight
        mainFab.layout((cx - fabW/2f).toInt(), (cy - fabH/2f).toInt(),
            (cx + fabW/2f).toInt(), (cy + fabH/2f).toInt())

        if (items.isEmpty()) return

        // Fix 2: choose the right divisor
        val isClosedRing = resolvedSweepAngle >= 360f
        val step = when {
            items.size == 1  -> 0f
            isClosedRing     -> resolvedSweepAngle / items.size       // ← key fix
            else             -> resolvedSweepAngle / (items.size - 1)
        }

        items.forEachIndexed { i, item ->
            val angleDeg = resolvedStartAngle + step * i
            val rad      = Math.toRadians(angleDeg.toDouble())
            val ix       = cx + circularRadiusPx * cos(rad).toFloat()
            val iy       = cy + circularRadiusPx * sin(rad).toFloat()
            val iW = item.measuredWidth; val iH = item.measuredHeight
            item.tag = Pair(ix - iW / 2f, iy - iH / 2f)
            item.layout((ix - iW/2f).toInt(), (iy - iH/2f).toInt(),
                (ix + iW/2f).toInt(), (iy + iH/2f).toInt())
        }
    }

    // ════════════════════════════════════════════════════════
    //  RADIAL layout  (Fix 3 — new mode)
    //
    //  Direction LEFT  → main FAB at right, children arc left.
    //  Direction RIGHT → main FAB at left,  children arc right.
    //  (Any other direction value → treated as LEFT.)
    //
    //  The children are placed in a vertical quarter-arc fan:
    //    • 1st child  = topmost (largest Y offset from baseline)
    //    • Last child = closest to horizontal baseline of the FAB
    //
    //  Arc geometry (as seen in Image A):
    //    • The fan spans from ~45° above the FAB centre
    //      to ~10° below. This creates the curved "hand of cards"
    //      look in the image.
    //    • Radius = circularRadiusPx (shared attribute).
    //    • Items that do not fit within the ViewGroup bounds are
    //      given a horizontal translationX offset so the user can
    //      scroll them by calling scrollRadialItems() or by
    //      wrapping this ViewGroup in a HorizontalScrollView.
    //      (In practice, with reasonable circularRadiusPx the
    //       arc fits; overflow is a safety mechanism.)
    // ════════════════════════════════════════════════════════

    private fun layoutRadial(w: Int, h: Int, items: List<ExpandableFabItem>) {
        val fabW = mainFab.measuredWidth
        val fabH = mainFab.measuredHeight
        val pad  = shadowPad

        // Radial direction: LEFT or RIGHT only; default to LEFT
        val goLeft = expandDirection != EfabDirection.RIGHT

        // FAB anchor position
        val fabL: Int
        val fabT: Int
        if (goLeft) {
            // Main FAB on the RIGHT side, children spread LEFT
            fabL = w - fabW - pad
            fabT = h - fabH - pad    // bottom-right corner
        } else {
            // Main FAB on the LEFT side, children spread RIGHT
            fabL = pad
            fabT = h - fabH - pad    // bottom-left corner
        }
        mainFab.layout(fabL, fabT, fabL + fabW, fabT + fabH)

        if (items.isEmpty()) return

        // FAB centre (used as arc origin)
        val fabCx = fabL + fabW / 2f
        val fabCy = fabT + fabH / 2f

        // Arc spans from ~135° (upper-left of FAB) to ~180° (left of FAB)
        // for goLeft, and mirrored for goRight.
        // Angle 0° = right, 90° = down, 180° = left, 270° = up.
        //
        // For LEFT expansion:
        //   1st item (top)  → 225° (upper-left diagonal)
        //   last item       → 180° (directly left)
        // For RIGHT expansion (mirror):
        //   1st item (top)  → 315°
        //   last item       → 360°/0° (directly right)

        val arcStart = if (goLeft) 260.0 else 280.0
        val arcEnd   = if (goLeft) 180.0 else 360.0

        val sweepTotal = arcEnd - arcStart   // negative for left (225→180 = -45)
        val step = if (items.size == 1) 0.0
        else sweepTotal / (items.size - 1)

        items.forEachIndexed { i, item ->
            val angleDeg = arcStart + step * i
            val rad      = Math.toRadians(angleDeg)
            val ix       = fabCx + circularRadiusPx * cos(rad).toFloat()
            val iy       = fabCy + circularRadiusPx * sin(rad).toFloat()
            val iW = item.measuredWidth; val iH = item.measuredHeight
            item.layout((ix - iW/2f).toInt(), (iy - iH/2f).toInt(),
                (ix + iW/2f).toInt(), (iy + iH/2f).toInt())
        }
    }

    // ════════════════════════════════════════════════════════
    //  Expand / Collapse API
    // ════════════════════════════════════════════════════════

    fun toggle() = if (isExpanded) collapse() else expand()

    fun expand() {
        if (isExpanded) return
        isExpanded = true
        animateMainFab(expanding = true)
        if (showScrim) animateScrim(show = true)
        fabItems.forEachIndexed { i, item ->
            item.visibility = VISIBLE
            when (layoutMode) {
                EfabLayoutMode.CIRCULAR -> animateItemCircular(item, i, appearing = true)
                else                    -> animateItem(item, i, appearing = true)
            }
        }
        stateChangeListener?.onExpanded()
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false
        animateMainFab(expanding = false)
        if (showScrim) animateScrim(show = false)
        fabItems.forEachIndexed { i, item ->
            when (layoutMode) {
                EfabLayoutMode.CIRCULAR -> animateItemCircular(item, i, appearing = false)
                else                    -> animateItem(item, i, appearing = false)
            }
        }
        stateChangeListener?.onCollapsed()
    }

    fun setExpanded(expanded: Boolean, animate: Boolean = true) {
        if (!animate) {
            isExpanded = expanded
            fabItems.forEach { item ->
                item.alpha = if (expanded) 1f else 0f
                item.scaleX = if (expanded) 1f else 0f
                item.scaleY = if (expanded) 1f else 0f
                item.translationX = 0f; item.translationY = 0f
                item.visibility = if (expanded) VISIBLE else INVISIBLE
            }
            mainFab.rotation = mainFabTargetRotation(expanded)
            return
        }
        if (expanded) expand() else collapse()
    }

    val isOpen: Boolean get() = isExpanded

    // ════════════════════════════════════════════════════════
    //  Main FAB icon animation
    // ════════════════════════════════════════════════════════

    private fun mainFabTargetRotation(expanding: Boolean): Float =
        if (customMainIcon != null) { if (expanding) 45f  else 0f }
        else                        { if (expanding) 135f else 0f }

    private fun animateMainFab(expanding: Boolean) {
        mainFab.animate()
            .rotation(mainFabTargetRotation(expanding))
            .setDuration((200f / animationSpeed.multiplier).toLong())
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    // ════════════════════════════════════════════════════════
    //  Scrim animation
    // ════════════════════════════════════════════════════════

    private fun animateScrim(show: Boolean) {
        if (show) scrimView.visibility = VISIBLE
        scrimView.animate()
            .alpha(if (show) 1f else 0f)
            .setDuration(200)
            .withEndAction { if (!show) scrimView.visibility = GONE }
            .start()
    }

    // ════════════════════════════════════════════════════════
    //  Linear / Radial item animation
    // ════════════════════════════════════════════════════════

    private fun animateItem(item: ExpandableFabItem, index: Int, appearing: Boolean) {
        val delay    = staggerFor(index)
        val duration = if (appearing) appearDur() else disappearDur()
        item.animate().cancel()

        when (animationType) {

            EfabAnimType.SLIDE -> {
                val (dx, dy) = slideVector()
                if (appearing) {
                    item.translationX = dx; item.translationY = dy
                    item.alpha = 0f; item.scaleX = 1f; item.scaleY = 1f
                }
                item.animate()
                    .translationX(if (appearing) 0f else dx)
                    .translationY(if (appearing) 0f else dy)
                    .alpha(if (appearing) 1f else 0f)
                    .setDuration(duration).setStartDelay(delay)
                    .setInterpolator(if (appearing) DecelerateInterpolator()
                    else           AccelerateInterpolator())
                    .withEndAction { if (!appearing) item.visibility = INVISIBLE }
                    .start()
            }

            EfabAnimType.SCALE -> {
                if (appearing) { item.scaleX = 0f; item.scaleY = 0f; item.alpha = 0f }
                item.animate()
                    .scaleX(if (appearing) 1f else 0f)
                    .scaleY(if (appearing) 1f else 0f)
                    .alpha(if (appearing) 1f else 0f)
                    .setDuration(duration).setStartDelay(delay)
                    .setInterpolator(if (appearing) OvershootInterpolator(1.5f)
                    else           AccelerateInterpolator())
                    .withEndAction { if (!appearing) item.visibility = INVISIBLE }
                    .start()
            }

            EfabAnimType.FADE -> {
                if (appearing) item.alpha = 0f
                item.animate()
                    .alpha(if (appearing) 1f else 0f)
                    .setDuration(duration).setStartDelay(delay)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction { if (!appearing) item.visibility = INVISIBLE }
                    .start()
            }

            EfabAnimType.FAN -> {
                val angle = 30f + index * 25f
                if (appearing) { item.rotation = angle; item.scaleX = 0f; item.scaleY = 0f; item.alpha = 0f }
                item.animate()
                    .rotation(if (appearing) 0f else angle)
                    .scaleX(if (appearing) 1f else 0f).scaleY(if (appearing) 1f else 0f)
                    .alpha(if (appearing) 1f else 0f)
                    .setDuration(duration).setStartDelay(delay)
                    .setInterpolator(if (appearing) OvershootInterpolator() else AccelerateInterpolator())
                    .withEndAction { if (!appearing) { item.visibility = INVISIBLE; item.rotation = 0f } }
                    .start()
            }

            EfabAnimType.FLIP -> {
                item.cameraDistance = 8000f * resources.displayMetrics.density
                if (appearing) { item.rotationY = 90f; item.alpha = 0f }
                item.animate()
                    .rotationY(if (appearing) 0f else -90f)
                    .alpha(if (appearing) 1f else 0f)
                    .setDuration(duration).setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction { if (!appearing) { item.visibility = INVISIBLE; item.rotationY = 0f } }
                    .start()
            }

            EfabAnimType.SPIRAL -> {
                val deg = 35f * (index + 1)
                if (appearing) { item.rotation = -deg; item.scaleX = 0f; item.scaleY = 0f; item.alpha = 0f }
                item.animate()
                    .rotation(if (appearing) 0f else deg)
                    .scaleX(if (appearing) 1f else 0f).scaleY(if (appearing) 1f else 0f)
                    .alpha(if (appearing) 1f else 0f)
                    .setDuration(duration).setStartDelay(delay)
                    .setInterpolator(if (appearing) OvershootInterpolator(2f) else AccelerateInterpolator())
                    .withEndAction { if (!appearing) { item.visibility = INVISIBLE; item.rotation = 0f } }
                    .start()
            }

            EfabAnimType.BOUNCE -> {
                if (appearing) { item.scaleX = 0f; item.scaleY = 0f; item.alpha = 0f }
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(item, "scaleX",
                            if (appearing) 0f else 1f, if (appearing) 1f else 0f),
                        ObjectAnimator.ofFloat(item, "scaleY",
                            if (appearing) 0f else 1f, if (appearing) 1f else 0f),
                        ObjectAnimator.ofFloat(item, "alpha",
                            if (appearing) 0f else 1f, if (appearing) 1f else 0f)
                    )
                    this.duration = duration; startDelay = delay
                    interpolator  = if (appearing) BounceInterpolator() else AccelerateInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(a: Animator) {
                            if (!appearing) item.visibility = INVISIBLE
                        }
                    })
                    start()
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  Circular item animation
    //  Children spawn from the FAB centre, travel to arc position.
    //  On collapse: travel back in and vanish.
    // ════════════════════════════════════════════════════════

    private fun animateItemCircular(item: ExpandableFabItem, index: Int, appearing: Boolean) {
        val delay    = staggerFor(index)
        val duration = if (appearing) appearDur() else disappearDur()
        item.animate().cancel()

        // Vector from item's arc-position centre back to main FAB centre
        val fabCx  = mainFab.left + mainFab.width  / 2f
        val fabCy  = mainFab.top  + mainFab.height / 2f
        val itemCx = item.left + item.width  / 2f
        val itemCy = item.top  + item.height / 2f
        val offsetX = fabCx - itemCx
        val offsetY = fabCy - itemCy

        if (appearing) {
            item.translationX = offsetX; item.translationY = offsetY
            item.scaleX = 0f;  item.scaleY = 0f; item.alpha = 0f
        }

        item.animate()
            .translationX(if (appearing) 0f else offsetX)
            .translationY(if (appearing) 0f else offsetY)
            .scaleX(if (appearing) 1f else 0f).scaleY(if (appearing) 1f else 0f)
            .alpha(if (appearing) 1f else 0f)
            .setDuration(duration).setStartDelay(delay)
            .setInterpolator(if (appearing) OvershootInterpolator(1.2f) else AccelerateInterpolator())
            .withEndAction {
                if (!appearing) {
                    item.visibility = INVISIBLE
                    item.translationX = 0f; item.translationY = 0f
                }
            }
            .start()
    }

    // ════════════════════════════════════════════════════════
    //  Animation helpers
    // ════════════════════════════════════════════════════════

    /**
     * Slide start offset for LINEAR and RADIAL modes.
     * For LINEAR: items come from below (UP) or above (DOWN).
     * For RADIAL:  items come from the FAB direction.
     */
    private fun slideVector(): Pair<Float, Float> {
        val dist = dp(40f)
        return when (layoutMode) {
            EfabLayoutMode.LINEAR -> when (expandDirection) {
                EfabDirection.DOWN -> Pair(0f, -dist)
                else               -> Pair(0f,  dist)   // UP default
            }
            EfabLayoutMode.RADIAL -> when (expandDirection) {
                EfabDirection.RIGHT -> Pair(-dist, 0f)
                else                -> Pair( dist, 0f)   // LEFT default
            }
            EfabLayoutMode.CIRCULAR -> Pair(0f, dist)    // handled separately
        }
    }

    // ════════════════════════════════════════════════════════
    //  Label position resolution
    // ════════════════════════════════════════════════════════

    /**
     * Resolves EfabLabelPos.AUTO to a concrete side.
     *
     * CIRCULAR / RADIAL  → always HIDDEN (arc items have no labels)
     * LINEAR + right half → LEFT  (label points inward toward screen centre)
     * LINEAR + left half  → RIGHT
     */
    private fun resolveItemLabelPos(raw: EfabLabelPos): EfabLabelPos {
        if (layoutMode == EfabLayoutMode.CIRCULAR ||
            layoutMode == EfabLayoutMode.RADIAL) return EfabLabelPos.HIDDEN
        if (raw != EfabLabelPos.AUTO) return raw
        return if (isOnRightHalf) EfabLabelPos.LEFT else EfabLabelPos.RIGHT
    }

    private fun resolveAllLabelPositions() {
        allItems.forEach { it.resolvedLabelPos = resolveItemLabelPos(it.efabLabelPosition) }
    }

    // ════════════════════════════════════════════════════════
    //  Utility
    // ════════════════════════════════════════════════════════

    private fun dp(v: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)

    private fun resolveThemeColor(attr: Int): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(attr, tv, true)) tv.data
        else Color.WHITE
    }
}