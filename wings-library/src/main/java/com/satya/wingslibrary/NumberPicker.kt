package com.satya.wingslibrary

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

class NumberPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val decreaseButton: ImageButton
    private val increaseButton: ImageButton
    private val countTextView: TextView

    private val handler = Handler(Looper.getMainLooper())

    private var autoIncrement = false
    private var autoDecrement = false

    private var count = 1
    private var minCount = 0
    private var maxCount = 10
    private var countJumpInterval = 1

    private val holdDelay = 300L
    private val repeatDelay = 120L

    var onValueChange: ((Int) -> Unit)? = null

    init {

        orientation = HORIZONTAL
        LayoutInflater.from(context)
            .inflate(R.layout.view_number_picker, this, true)

        decreaseButton = findViewById(R.id.decreaseBtn)
        increaseButton = findViewById(R.id.increaseBtn)
        countTextView = findViewById(R.id.countTv)

        val a = context.obtainStyledAttributes(attrs, R.styleable.NumberPicker)

        count = a.getInt(R.styleable.NumberPicker_count, 1)
        minCount = a.getInt(R.styleable.NumberPicker_minCount, 0)
        maxCount = a.getInt(R.styleable.NumberPicker_maxCount, 10)
        countJumpInterval = a.getInt(
            R.styleable.NumberPicker_countJumpInterval,
            1
        )

        val textColor = a.getColor(
            R.styleable.NumberPicker_countTextColor,
            ContextCompat.getColor(context, android.R.color.black)
        )

        val textSize = a.getDimension(
            R.styleable.NumberPicker_countTextSize,
            spToPx(16f)
        )

        val fontFamily = a.getResourceId(
            R.styleable.NumberPicker_countTextFontFamily,
            0
        )

        val textStyle = a.getInt(
            R.styleable.NumberPicker_countTextStyle,
            Typeface.NORMAL
        )

        val buttonSize = a.getDimensionPixelSize(
            R.styleable.NumberPicker_buttonSize,
            dpToPx(32)
        )

        val decreaseTint = a.getColor(
            R.styleable.NumberPicker_decreaseButtonTint,
            ContextCompat.getColor(context, android.R.color.holo_red_dark)
        )

        val increaseTint = a.getColor(
            R.styleable.NumberPicker_increaseButtonTint,
            ContextCompat.getColor(context, android.R.color.holo_green_dark)
        )

        a.recycle()

        decreaseButton.layoutParams.apply {
            width = buttonSize
            height = buttonSize
        }

        increaseButton.layoutParams.apply {
            width = buttonSize
            height = buttonSize
        }

        decreaseButton.backgroundTintList =
            ColorStateList.valueOf(decreaseTint)

        increaseButton.backgroundTintList =
            ColorStateList.valueOf(increaseTint)

        countTextView.setTextColor(textColor)
        countTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)

        if (fontFamily != 0) {
            val tf = ResourcesCompat.getFont(context, fontFamily)
            countTextView.typeface = Typeface.create(tf, textStyle)
        } else {
            countTextView.setTypeface(null, textStyle)
        }

        updateUI(false)

        setupTouchControls()
    }

    private fun setupTouchControls() {

        increaseButton.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    increase(true)

                    autoIncrement = true
                    handler.postDelayed(
                        autoIncrementRunnable,
                        holdDelay
                    )
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    autoIncrement = false
                    handler.removeCallbacks(autoIncrementRunnable)
                }
            }

            true
        }

        decreaseButton.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    decrease(true)

                    autoDecrement = true
                    handler.postDelayed(
                        autoDecrementRunnable,
                        holdDelay
                    )
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    autoDecrement = false
                    handler.removeCallbacks(autoDecrementRunnable)
                }
            }

            true
        }
    }

    private fun increase(animate: Boolean) {

        if (count >= maxCount) {
            autoIncrement = false
            return
        }

        count = (count + countJumpInterval)
            .coerceAtMost(maxCount)

        updateUI(animate)
    }

    private fun decrease(animate: Boolean) {

        if (count <= minCount) {
            autoDecrement = false
            return
        }

        count = (count - countJumpInterval)
            .coerceAtLeast(minCount)

        updateUI(animate)
    }

    private fun updateUI(animate: Boolean) {

        if (animate) {
            animateTextChange(count)
        } else {
            countTextView.text = count.toString()
        }

        updateButtons()

        onValueChange?.invoke(count)
    }

    private fun updateButtons() {

        increaseButton.isEnabled = count < maxCount
        decreaseButton.isEnabled = count > minCount

        increaseButton.alpha =
            if (increaseButton.isEnabled) 1f else 0.4f

        decreaseButton.alpha =
            if (decreaseButton.isEnabled) 1f else 0.4f
    }

    private val autoIncrementRunnable = object : Runnable {
        override fun run() {

            if (!autoIncrement) return

            increase(false)

            handler.postDelayed(this, repeatDelay)
        }
    }

    private val autoDecrementRunnable = object : Runnable {
        override fun run() {

            if (!autoDecrement) return

            decrease(false)

            handler.postDelayed(this, repeatDelay)
        }
    }

    private fun animateTextChange(value: Int) {

        countTextView.animate().cancel()

        countTextView.animate()
            .translationY(-20f)
            .alpha(0f)
            .setDuration(120)
            .withEndAction {

                countTextView.text = value.toString()

                countTextView.translationY = 20f

                countTextView.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }

    fun setCount(value: Int) {
        count = value.coerceIn(minCount, maxCount)
        updateUI(false)
    }

    fun getCount(): Int = count

    fun setRange(min: Int, max: Int) {
        minCount = min
        maxCount = max
        setCount(count)
    }

    private fun dpToPx(dp: Int) =
        (dp * resources.displayMetrics.density).toInt()

    private fun spToPx(sp: Float) =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            resources.displayMetrics
        )
}