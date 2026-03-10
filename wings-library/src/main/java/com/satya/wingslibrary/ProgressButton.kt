package com.satya.wingslibrary

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator

class ProgressButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {

    enum class ExecuteAt { START, MIDDLE, END }

    private val card: MaterialCardView
    private val textView: TextView
    private val progress: CircularProgressIndicator

    private var onClickAction: (() -> Unit)? = null
    private var delayTime: Long = 500L
    private var executeAt = ExecuteAt.END

    init {
        LayoutInflater.from(context).inflate(R.layout.view_progress_button, this, true)

        card = findViewById(R.id.buttonCard)
        textView = findViewById(R.id.btnTxt)
        progress = findViewById(R.id.progress_circular)

        val a = context.obtainStyledAttributes(attrs, R.styleable.ProgressButton)

        val buttonText = a.getString(R.styleable.ProgressButton_buttonText) ?: "Button"
        val buttonHeight = a.getDimensionPixelSize(
            R.styleable.ProgressButton_buttonHeight,
            dpToPx(48)
        )

        val bgColor = a.getColor(
            R.styleable.ProgressButton_buttonColor,
            ContextCompat.getColor(context, R.color.teal_700)
        )

        val textColor = a.getColor(
            R.styleable.ProgressButton_buttonTextColor,
            ContextCompat.getColor(context, R.color.white)
        )

        val textSizePx = a.getDimension(
            R.styleable.ProgressButton_buttonTextSize,
            spToPx(14f)
        )

        val cornerRadius = a.getDimension(
            R.styleable.ProgressButton_cornerRadius,
            dpToPx(24).toFloat()
        )

        val progressSize = a.getDimensionPixelSize(
            R.styleable.ProgressButton_progressSize,
            dpToPx(28)
        )

        val trackThickness = a.getDimensionPixelSize(
            R.styleable.ProgressButton_progressTrackThickness,
            dpToPx(3)
        )

        val indicatorColor = a.getColor(
            R.styleable.ProgressButton_progressIndicatorColor,
            ContextCompat.getColor(context, android.R.color.white)
        )

        val trackColor = a.getColor(
            R.styleable.ProgressButton_progressTrackColor,
            ContextCompat.getColor(context, android.R.color.transparent)
        )

        delayTime = a.getInt(R.styleable.ProgressButton_delayTime, 500).toLong()

        executeAt = when (a.getInt(R.styleable.ProgressButton_executeAt, 2)) {
            0 -> ExecuteAt.START
            1 -> ExecuteAt.MIDDLE
            else -> ExecuteAt.END
        }

        a.recycle()

        // Apply attributes
        textView.text = buttonText
        textView.setTextColor(textColor)
        card.setCardBackgroundColor(bgColor)
        card.radius = cornerRadius

        progress.apply {
            indicatorSize = progressSize
            this.trackThickness = trackThickness
            setIndicatorColor(indicatorColor)
            setTrackColor(trackColor)
            isIndeterminate = true
        }

        // IMPORTANT: height fix
        card.layoutParams = card.layoutParams.apply {
            height = buttonHeight
        }

        card.setOnClickListener {
            if (progress.visibility == VISIBLE) return@setOnClickListener
            showProgress(true)
            performClickAction()
        }

        textView.setTextSize(
            android.util.TypedValue.COMPLEX_UNIT_PX,
            textSizePx
        )
    }

    private fun performClickAction() {
        when (executeAt) {
            ExecuteAt.START -> {
                onClickAction?.invoke()
                postDelayed({ showProgress(false) }, delayTime)
            }
            ExecuteAt.MIDDLE -> {
                postDelayed({
                    onClickAction?.invoke()
                    postDelayed({ showProgress(false) }, delayTime / 2)
                }, delayTime / 2)
            }
            ExecuteAt.END -> {
                postDelayed({
                    onClickAction?.invoke()
                    showProgress(false)
                }, delayTime)
            }
        }
    }

    fun showProgress(show: Boolean) {
        val duration = 180L

        if (show) {
            progress.visibility = VISIBLE
            progress.alpha = 1f

            textView.animate()
                .alpha(0f)
                .setDuration(duration)
                .withEndAction {
                    textView.visibility = GONE
                }
                .start()

        } else {
            textView.visibility = VISIBLE
            textView.alpha = 0f

            textView.animate()
                .alpha(1f)
                .setDuration(duration)
                .start()

            progress.visibility = GONE
        }

        isEnabled = !show
    }

    fun setOnButtonClick(action: () -> Unit) {
        onClickAction = action
    }

    fun initiateClick() {
        card.performClick()
    }

    fun setText(text: String) {
        textView.text = text
    }

    fun setButtonColor(color: Int) {
        card.setCardBackgroundColor(color)
    }

    fun setButtonTextColor(color: Int) {
        textView.setTextColor(color)
    }

    fun setDelayTime(time: Long) {
        delayTime = time
    }

    fun setExecuteAt(type: ExecuteAt) {
        executeAt = type
    }

    fun setProgressIndicatorColor(color: Int) {
        progress.setIndicatorColor(color)
    }

    fun setProgressTrackColor(color: Int) {
        progress.setTrackColor(color)
    }

    fun setProgressSize(sizePx: Int) {
        progress.indicatorSize = sizePx
    }

    fun setProgressThickness(thicknessPx: Int) {
        progress.trackThickness = thicknessPx
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun spToPx(sp: Float): Float {
        return android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            sp,
            resources.displayMetrics
        )
    }
}