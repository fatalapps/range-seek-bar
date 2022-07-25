package com.innovattic.rangeseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin


class WindPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private companion object {

        val CIRCLE_RANGE = 0..360
        const val GAP_DEGREE = 5
    }

    private var activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    private val oval = RectF()
    private val ovalSmall = RectF()

    private val directionsToDraw = Direction.values()
    private val sectorsCount = directionsToDraw.size

    private val degreeStep = CIRCLE_RANGE.last / sectorsCount

    private val sectors: Array<WindSector>

    /**
     * Color of the selected sector.
     */
    private var activeColor: Int

    /**
     * Color of a sector.
     */
    private var inactiveColor: Int

    /**
     * Drawable for an arrow icon.
     */
    private var arrowBitmap: Bitmap

    /**
     * Color for text.
     */
    private var textColor: Int

    /**
     * Size of text.
     */
    private var textSize: Int

    private var center = 0f

    private var bigInnerRadius = 0f

    /**
     * Height of the square's side.
     */
    private var sideHeight: Int = 0
        set(value) {
            if (field != value) {
                field = value
                makeAllocations()
            }
        }

    init {
        val res = context.resources
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.WindPicker, 0, 0)

        val defaultActiveColor = ContextCompat.getColor(context, R.color.wind_defaultActive)
        val defaultInactiveColor = ContextCompat.getColor(context, R.color.wind_defaultInactive)
        val defaultArrow = ContextCompat.getDrawable(context, R.drawable.direction_arrow)
        val defaultTextColor = ContextCompat.getColor(context, R.color.wind_defaultITextColor)
        val defaultTextSize = res.getDimensionPixelSize(R.dimen.wind_textSizeDefault)

        try {
            activeColor = extractActiveColor(a, defaultActiveColor)
            inactiveColor = extractInactiveColor(a, defaultInactiveColor)
            arrowBitmap = drawableToBitmap(extractArrowDrawable(a, defaultArrow!!))
            textColor = extractTextColor(a, defaultTextColor)
            textSize = extractTextSize(a, defaultTextSize)

        } finally {
            a.recycle()
        }

        activePaint.apply {
            color = activeColor
        }

        inactivePaint.apply {
            color = inactiveColor
        }

        bgPaint.apply {
            color = textColor
        }

        textPaint.apply {
            color = textColor
            this.textSize = this@WindPicker.textSize.toFloat()
        }

        sectors = directionsToDraw.mapIndexed { i, d ->
            val startAngle = degreeStep * i
            val sweepAngle = degreeStep - GAP_DEGREE

            WindSector(
                direction = d,
                startAngle,
                sweepAngle,
                Path(),
                WindSector.TextLocation(0f, 0f, 0f, Rect()),
                WindSector.Arrow(0f, 0f, 0f, Rect(), Rect(0, 0, arrowBitmap.width, arrowBitmap.height)),
                Region(),
                paint = inactivePaint,
                isSelected = false
            )
        }.toTypedArray()

    }

    fun setDirections(set: Set<Direction>) {
        sectors.forEachIndexed { index, sector ->
            if (set.contains(sector.direction)) {
                sectors[index].apply {
                    paint = activePaint
                    isSelected = true
                }
            }
        }
        invalidate()
    }

    fun getSelection() = sectors.filter { it.isSelected }.map { it.direction }.toSet()

    private fun makeAllocations() {

        center = sideHeight / 2f
        bigInnerRadius = center

        oval.set(
            0f,
            0f,
            sideHeight.toFloat(),
            sideHeight.toFloat()
        )

        val smallRadius = bigInnerRadius / 4f
        ovalSmall.set(
            center - smallRadius,
            center - smallRadius,
            center + smallRadius,
            center + smallRadius
        )


        sectors.forEachIndexed { i, sector ->
            sectors[i].apply {

                this.path.apply {
                    arcTo(oval, sector.startAngle - sector.sweepAngle / 2f, sector.sweepAngle.toFloat(), true)
                    arcTo(ovalSmall, sector.startAngle + sector.sweepAngle / 2f - GAP_DEGREE, (sector.sweepAngle * (3 / 4f)).unaryMinus())
                    close()
                }

                this.textLocation.apply {

                    val text = sector.direction.name
                    textPaint.getTextBounds(text, 0, text.length, this.bounds)
                    var rotationNew = sector.startAngle
                    val rad = Math.toRadians(rotationNew.toDouble())
                    val x = (center + sideHeight / (12 / 5f) * cos(rad))
                    val y = (center + sideHeight / (12 / 5f) * sin(rad))

                    rotationNew = when {
                        rotationNew % 90 == 0 -> 0
                        rotationNew in 0..180 -> rotationNew - 90
                        rotationNew in 180..360 -> rotationNew + 90
                        else -> rotationNew
                    }

                    this.x = x.toFloat()
                    this.y = y.toFloat()
                    this.rotation = rotationNew.toFloat()
                }

                this.arrowLocation.apply {
                    val rad = Math.toRadians(sector.startAngle.toDouble())
                    val x = (center + sideHeight / 3f * cos(rad)).toFloat()
                    val y = (center + sideHeight / 3f * sin(rad)).toFloat()
                    this.x = x
                    this.y = y
                    this.rotation = sector.startAngle - 180f

                    val w = this.rect.width() / 2
                    val h = this.rect.height() / 2
                    this.dst = Rect(x.toInt() - w, y.toInt() - h, x.toInt() + w, y.toInt() + h)
                }

                this.touchRegion.apply {
                    setPath(path, Region(0, 0, sideHeight, sideHeight))
                }

                this.paint = if (isSelected) activePaint else inactivePaint
            }
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = width.coerceAtMost(height)
        sideHeight = size
        setMeasuredDimension(sideHeight, sideHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.run {
            drawCircle(center, center, bigInnerRadius - 1, bgPaint)

            sectors.forEach { sector ->
                val textLocation = sector.textLocation
                val arrow = sector.arrowLocation
                drawPath(sector.path, sector.paint)
                save()
                rotate(textLocation.rotation, textLocation.x, textLocation.y)
                drawText(sector.direction.name, textLocation.x, textLocation.y - textLocation.bounds.exactCenterY(), textPaint)
                restore()
                save()
                rotate(arrow.rotation, arrow.x, arrow.y)
                drawBitmap(arrowBitmap, arrow.rect, arrow.dst, bgPaint)
                restore()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val (x, y) = listOf(event.x.toInt(), event.y.toInt())
                val clicked = sectors.find { it.onClicked(x, y, activePaint, inactivePaint) }
                clicked?.let { invalidate() }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun extractArrowDrawable(a: TypedArray, defaultValue: Drawable): Drawable {
        return a.getDrawable(R.styleable.WindPicker_wind_arrowDrawable) ?: defaultValue
    }

    private fun extractActiveColor(a: TypedArray, defaultValue: Int): Int {
        return a.getColor(R.styleable.WindPicker_wind_activeColor, defaultValue)
    }

    private fun extractInactiveColor(a: TypedArray, defaultValue: Int): Int {
        return a.getColor(R.styleable.WindPicker_wind_inactiveColor, defaultValue)
    }

    private fun extractTextColor(a: TypedArray, defaultValue: Int): Int {
        return a.getColor(R.styleable.WindPicker_wind_textColor, defaultValue)
    }

    private fun extractTextSize(a: TypedArray, defaultValue: Int): Int {
        return a.getDimensionPixelSize(R.styleable.WindPicker_wind_textSize, defaultValue)
    }

    enum class Direction(private val string: String) {
        E("E"),
        SE("SE"),
        S("S"),
        SW("SW"),
        W("W"),
        NW("NW"),
        N("N"),
        NE("NE");

        override fun toString(): String {
            return string
        }

        companion object {
            fun fromString(str: String): Direction? {
                for (wd in values()) {
                    if (wd.string == str) {
                        return wd
                    }
                }
                return null
            }
        }
    }

    data class WindSector(
        val direction: Direction,
        var startAngle: Int,
        var sweepAngle: Int,
        var path: Path,
        var textLocation: TextLocation,
        var arrowLocation: Arrow,
        var touchRegion: Region,
        var paint: Paint,
        var isSelected: Boolean
    ) {
        data class TextLocation(var x: Float, var y: Float, var rotation: Float, val bounds: Rect)
        data class Arrow(var x: Float, var y: Float, var rotation: Float, var dst: Rect, var rect: Rect)

        fun onClicked(x: Int, y: Int, activePaint: Paint, inactivePaint: Paint): Boolean {
            val isClicked = touchRegion.contains(x, y)
            if (isClicked) {
                isSelected = !isSelected
                paint = if (isSelected) activePaint else inactivePaint
            }
            return isClicked
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}