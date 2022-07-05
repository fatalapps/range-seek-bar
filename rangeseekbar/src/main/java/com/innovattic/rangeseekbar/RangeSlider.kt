package com.innovattic.rangeseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min

open class RangeSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val helperRectF = RectF()

    private var selectedThumb: Int = THUMB_NONE

    private var offset: Int = 0

    /**
     * The thickness of the horizontal track.
     */
    var trackThickness: Int

    /**
     * The thickness of the selected range of horizontal track.
     */
    var trackSelectedThickness: Int

    /**
     * Color of horizontal track.
     */
    var trackColor: Int

    /**
     * Color of the selected range of horizontal track.
     */
    var trackSelectedColor: Int

    var touchRadius: Int

    var minThumbDrawable: Drawable

    var maxThumbDrawable: Drawable

    var sidePadding: Int

    var trackRoundedCaps: Boolean = true

    var trackSelectedRoundedCaps: Boolean = false

    /**
     * Pixel offset of the min thumb
     */
    var minThumbOffset: Point

    /**
     * Pixel offset of the max thumb
     */
    var maxThumbOffset: Point

    /**
     * The minimum range to be selected. It should at least be 1.
     */
    var minRange: Int = 1
        set(value) {
            field = max(value, 1)
        }

    /**
     * The maximum value of thumbs.
     */
    var max: Int = 100
        set(value) {
            if(value > min) {
                field = value
                minThumbValue = min
                maxThumbValue = field
            }
        }

    /**
     * The minimum value of thumbs.
     */
    var min: Int = 0
        set(value) {
            if(value < max) {
                field = value
                minThumbValue = value
                maxThumbValue = max
            }
        }

    /**
     * Value of min thumb.
     */
    private var minThumbValue: Int = 0
    set(value) {
        if(value >= min) {
            field = value
        }
    }

    /**
     * Value of max thumb.
     */
    private var maxThumbValue: Int = 0
        set(value) {
            if(value <= max) {
                field = value
            }
        }

    private var lastMinThumbValue = minThumbValue

    private var lastMaxThumbValue = maxThumbValue

    private val extraHeight: Int

    /**
     * Text size of the labels.
     */
    private var labelTextSize = 30

    /**
     * Text size of the labels.
     */
    private var labelTextColor: Int

    /**
     * A callback receiver for view changes.
     */
    var seekBarChangeListener: SeekBarChangeListener? = null

    /**
     * A callback receiver for view changes.
     */
    var valueChangeListener: ValueChangeListener? = null

    /**
     * A callback receiver for label formatting.
     */
    var labelFormatterListener: LabelFormatter? = null

    init {
        val res = context.resources
        val defaultTrackThickness = res.getDimensionPixelSize(R.dimen.rs_trackDefaultThickness)
        val defaultSidePadding = res.getDimensionPixelSize(R.dimen.rs_defaultSidePadding)
        val defaultTouchRadius = res.getDimensionPixelSize(R.dimen.rs_touchRadius)
        val defaultTrackColor = ContextCompat.getColor(context, R.color.rsb_trackDefaultColor)
        val defaultSelectedTrackColor = ContextCompat.getColor(context, R.color.rsb_trackSelectedDefaultColor)
        val defaultMinThumb = ContextCompat.getDrawable(context, R.drawable.thumb_circle)!!
        val defaultMaxThumb = ContextCompat.getDrawable(context, R.drawable.thumb_circle)!!
        val defaultTextSize = res.getDimensionPixelSize(R.dimen.rs_defaultLabelTextSize)
        val defaultTextColor = ContextCompat.getColor(context, R.color.rs_defaultTextColor)

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.RangeSlider, 0, 0)
        try {
            extraHeight = res.getDimensionPixelSize(R.dimen.rs_defaultExtraHeight)
            max = extractMaxValue(a)
            minRange = extractMinRange(a)
            labelTextSize = extractTextSize(a, defaultTextSize)
            labelTextColor = extractTextColor(a, defaultTextColor)
            sidePadding = extractSidePadding(a, defaultSidePadding)
            touchRadius = extractTouchRadius(a, defaultTouchRadius)
            trackThickness = extractTrackThickness(a, defaultTrackThickness)
            trackSelectedThickness = extractTrackSelectedThickness(a, defaultTrackThickness)
            trackColor = extractTrackColor(a, defaultTrackColor)
            trackSelectedColor = extractTrackSelectedColor(a, defaultSelectedTrackColor)
            minThumbDrawable = extractMinThumbDrawable(a, defaultMinThumb)
            maxThumbDrawable = extractMaxThumbDrawable(a, defaultMaxThumb)
            minThumbOffset = extractMinThumbOffset(a)
            maxThumbOffset = extractMaxThumbOffset(a)
            trackRoundedCaps = extractTrackRoundedCaps(a)
            trackSelectedRoundedCaps = extractTrackSelectedRoundedCaps(a)
            val initialMinThumbValue = extractInitialMinThumbValue(a)
            val initialMaxThumbValue = extractInitialMaxThumbValue(a)
            if (initialMinThumbValue != -1) {
                minThumbValue = max(0, initialMinThumbValue)
                keepMinWindow(THUMB_MIN)
            }
            if (initialMaxThumbValue != -1) {
                maxThumbValue = min(max, initialMaxThumbValue)
                keepMinWindow(THUMB_MAX)
            }
        } finally {
            a.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paddingLeft = this.paddingLeft + sidePadding
        val paddingRight = this.paddingRight + sidePadding
        val width = width - paddingLeft - paddingRight
        val verticalCenter = height - maxThumbDrawable.intrinsicHeight.toFloat()
        val minimumX = paddingLeft + ((minThumbValue - min) / (max.toFloat() - min)) * width
        val maximumX = paddingLeft + ((maxThumbValue - min) / (max.toFloat() - min)) * width

        // Draw full track
        trackPaint.color = trackColor
        canvas.drawTrack(
            paddingLeft + 0f,
            paddingLeft + width.toFloat(),
            verticalCenter,
            trackThickness.toFloat(),
            trackPaint,
            trackRoundedCaps
        )

        // Draw selected range of the track
        trackPaint.color = trackSelectedColor
        canvas.drawTrack(
            minimumX,
            maximumX,
            verticalCenter,
            trackSelectedThickness.toFloat(),
            trackPaint,
            trackSelectedRoundedCaps
        )

        // Draw thumb at minimumX position
        minThumbDrawable.drawAtPosition(canvas, minimumX.toInt() - maxThumbDrawable.intrinsicWidth / 3, minThumbOffset)

        // Draw thumb at maximumX position
        maxThumbDrawable.drawAtPosition(canvas, maximumX.toInt() - maxThumbDrawable.intrinsicWidth / 3, maxThumbOffset)

        drawLabels(canvas, minimumX - maxThumbDrawable.intrinsicWidth / 3, maximumX - maxThumbDrawable.intrinsicWidth / 3)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }

        var changed = false
        val paddingLeft = this.paddingLeft + sidePadding
        val paddingRight = this.paddingRight + sidePadding
        val width = width - paddingLeft - paddingRight
        val mx = when {
            event.x < paddingLeft -> 0
            paddingLeft <= event.x && event.x <= (this.width - paddingRight) -> ((event.x - paddingLeft) / width * (max-min)).toInt()
            else -> max - min
        }
        val leftThumbX = (paddingLeft + ((minThumbValue - min) / (max.toFloat() - min) * width)).toInt()
        val rightThumbX = (paddingLeft + ((maxThumbValue - min) / (max.toFloat() - min) * width)).toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isInsideRadius(event, leftThumbX, (height - (maxThumbDrawable.intrinsicHeight)).toInt(), touchRadius)) {
                    selectedThumb = THUMB_MIN
                    offset = mx - minThumbValue
                    changed = true
                    parent.requestDisallowInterceptTouchEvent(true)
                    seekBarChangeListener?.onStartedSeeking()
                    isPressed = true
                } else if (isInsideRadius(event, rightThumbX, (height - (maxThumbDrawable.intrinsicHeight)).toInt(), touchRadius)) {
                    selectedThumb = THUMB_MAX
                    offset = maxThumbValue - mx
                    changed = true
                    parent.requestDisallowInterceptTouchEvent(true)
                    seekBarChangeListener?.onStartedSeeking()
                    isPressed = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedThumb == THUMB_MIN) {
                    minThumbValue = max(min(mx - offset, max - minRange), min)
                    changed = true
                } else if (selectedThumb == THUMB_MAX) {
                    maxThumbValue = min(max(mx + offset, minRange), max)
                    changed = true
                }
            }
            MotionEvent.ACTION_UP -> {
                selectedThumb = THUMB_NONE
                seekBarChangeListener?.onStoppedSeeking()
                isPressed = false
            }
        }
        keepMinWindow(selectedThumb)

        if (!changed) {
            return false
        }

        invalidate()
        if (lastMinThumbValue != minThumbValue || lastMaxThumbValue != maxThumbValue) {
            lastMinThumbValue = minThumbValue
            lastMaxThumbValue = maxThumbValue
            valueChangeListener?.onChange(minThumbValue, maxThumbValue)
        }
        return true
    }

    /**
     * Updates the value of minimum thumb and redraws the view.
     */
    fun setMinThumbVal(value: Int) {
        minThumbValue = max(value, min)
        keepMinWindow(THUMB_MIN)
        invalidate()
    }

    fun getMinThumbValue() = minThumbValue

    /**
     * Updates the value of maximum thumb and redraws the view.
     */
    fun setMaxThumbVal(value: Int) {
        maxThumbValue = min(value, max)
        keepMinWindow(THUMB_MAX)
        invalidate()
    }

    /**
     * Returns the current maximum value of selected range.
     */
    fun getMaxThumbValue() = maxThumbValue

    /**
     * Makes sure that while changing the value of a thumb, the other thumb's
     * value will also be changed if necessary to keep the min window for range.
     */
    private fun keepMinWindow(base: Int) {
        if (base == THUMB_MAX) {
            if (maxThumbValue <= minThumbValue + minRange) {
                minThumbValue = maxThumbValue - minRange
            }
        } else if (base == THUMB_MIN) {
            if (minThumbValue > maxThumbValue - minRange) {
                maxThumbValue = minThumbValue + minRange
            }
        }
    }

    private fun isInsideRadius(event: MotionEvent, cx: Int, cy: Int, radius: Int): Boolean {
        val dx = event.x - cx
        val dy = event.y - cy
        return (dx * dx) + (dy * dy) < (radius * radius)
    }

    @SuppressLint("SwitchIntDef")
    private fun measureHeight(measureSpec: Int): Int {
        val maxHeight = max(minThumbDrawable.intrinsicHeight, maxThumbDrawable.intrinsicHeight)
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (specMode) {
            MeasureSpec.EXACTLY -> specSize + extraHeight
            else -> maxHeight + sidePadding + extraHeight
        }
    }

    private fun drawLabels(canvas: Canvas, minX: Float, maxX: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = labelTextColor
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = labelTextSize.toFloat()
        var minPos = minX + maxThumbDrawable.intrinsicWidth / 2
        val maxPos = maxX + maxThumbDrawable.intrinsicWidth / 2
        if (minPos in maxPos - labelTextSize..maxPos + labelTextSize) minPos -= labelTextSize
        var minText = minThumbValue.toString()
        var maxText = maxThumbValue.toString()
        labelFormatterListener?.let {
            minText = it.onLabelSet(minThumbValue)
            maxText = it.onLabelSet(maxThumbValue)
        }
        val marginY = context.resources.getDimensionPixelSize(R.dimen.rs_labelMarginBottom)
        canvas.drawText(minText, minPos, extraHeight - maxThumbDrawable.intrinsicHeight.toFloat()/2 + labelTextSize - marginY, paint)
        canvas.drawText(maxText, maxPos, extraHeight - maxThumbDrawable.intrinsicHeight.toFloat()/2 + labelTextSize - marginY, paint)
    }


    private fun Drawable.drawAtPosition(canvas: Canvas, position: Int, offset: Point = Point(0, 0)) {
        val left = position + offset.x
        val top = (height - (intrinsicHeight) * 1.5f).toInt() + offset.y
        setBounds(left, top, left + intrinsicWidth, top + intrinsicHeight)
        draw(canvas)
    }

    private fun Canvas.drawTrack(left: Float, right: Float, cy: Float, thickness: Float, paint: Paint, round: Boolean) {
        val ht = thickness / 2
        val top = cy - ht
        val bottom = cy + ht

        if (round) {
            val l = left - ht
            val r = right + ht
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                drawRoundRect(l, top, r, bottom, thickness, thickness, paint)
            } else {
                helperRectF.set(l, top, r, bottom)
                drawRoundRect(helperRectF, thickness, thickness, paint)
            }
        } else {
            drawRect(left, top, right, bottom, paint)
        }
    }

    private fun extractMaxThumbDrawable(a: TypedArray, defaultValue: Drawable): Drawable {
        return a.getDrawable(R.styleable.RangeSlider_rs_maxThumbDrawable) ?: defaultValue
    }

    private fun extractMinThumbDrawable(a: TypedArray, defaultValue: Drawable): Drawable {
        return a.getDrawable(R.styleable.RangeSlider_rs_minThumbDrawable) ?: defaultValue
    }

    private fun extractTrackSelectedColor(a: TypedArray, defaultValue: Int): Int {
        return a.getColor(R.styleable.RangeSlider_rs_trackSelectedColor, defaultValue)
    }

    private fun extractTrackColor(a: TypedArray, defaultValue: Int): Int {
        return a.getColor(R.styleable.RangeSlider_rs_trackColor, defaultValue)
    }

    private fun extractTrackSelectedThickness(a: TypedArray, defaultValue: Int): Int {
        return a.getDimensionPixelSize(R.styleable.RangeSlider_rs_trackSelectedThickness, defaultValue)
    }

    private fun extractTrackThickness(a: TypedArray, defaultValue: Int): Int {
        return a.getDimensionPixelSize(R.styleable.RangeSlider_rs_trackThickness, defaultValue)
    }

    private fun extractTouchRadius(a: TypedArray, defaultValue: Int): Int {
        return a.getDimensionPixelSize(R.styleable.RangeSlider_rs_touchRadius, defaultValue)
    }

    private fun extractSidePadding(a: TypedArray, defaultValue: Int): Int {
        return a.getDimensionPixelSize(R.styleable.RangeSlider_rs_sidePadding, defaultValue)
    }

    private fun extractTrackRoundedCaps(a: TypedArray): Boolean {
        return a.getBoolean(R.styleable.RangeSlider_rs_trackRoundedCaps, true)
    }

    private fun extractTrackSelectedRoundedCaps(a: TypedArray): Boolean {
        return a.getBoolean(R.styleable.RangeSlider_rs_trackSelectedRoundedCaps, false)
    }

    private fun extractMinRange(a: TypedArray): Int {
        return a.getInteger(R.styleable.RangeSlider_rs_minRange, 1)
    }

    private fun extractMaxValue(a: TypedArray): Int {
        return a.getInteger(R.styleable.RangeSlider_rs_max, 100)
    }

    private fun extractTextSize(a: TypedArray, defaultValue: Int): Int {
        return a.getDimensionPixelSize(R.styleable.RangeSlider_rs_textSize, defaultValue)
    }

    private fun extractTextColor(a: TypedArray, defaultValue: Int): Int {
        return a.getColor(R.styleable.RangeSlider_rs_textColor, defaultValue)
    }

    private fun extractInitialMinThumbValue(a: TypedArray): Int {
        return a.getInteger(R.styleable.RangeSlider_rs_initialMinThumbValue, -1)
    }

    private fun extractInitialMaxThumbValue(a: TypedArray): Int {
        return a.getInteger(R.styleable.RangeSlider_rs_initialMaxThumbValue, -1)
    }

    private fun extractMinThumbOffset(a: TypedArray): Point {
        val x = a.getDimensionPixelSize(R.styleable.RangeSlider_rs_minThumbOffsetHorizontal, 0)
        val y = a.getDimensionPixelSize(R.styleable.RangeSlider_rs_minThumbOffsetVertical, 0)
        return Point(x, y)
    }

    private fun extractMaxThumbOffset(a: TypedArray): Point {
        val x = a.getDimensionPixelSize(R.styleable.RangeSlider_rs_maxThumbOffsetHorizontal, 0)
        val y = a.getDimensionPixelSize(R.styleable.RangeSlider_rs_maxThumbOffsetVertical, 0)
        return Point(x, y)
    }

    companion object {

        private const val THUMB_NONE = 0
        private const val THUMB_MIN = 1
        private const val THUMB_MAX = 2
    }


    interface SeekBarChangeListener {
        /**
         * Called when the user has started dragging min or max thumbs
         */
        fun onStartedSeeking()

        /**
         * Called when the user has stopped dragging min or max thumb
         */
        fun onStoppedSeeking()
    }

    /**
     * Called during the dragging of min or max value
     */
    fun interface ValueChangeListener {
        fun onChange(min: Int, max: Int)
    }

    /**
     * Called during the setting of labels' text values.
     */
    fun interface LabelFormatter {
        fun onLabelSet(value: Int): String
    }
}
