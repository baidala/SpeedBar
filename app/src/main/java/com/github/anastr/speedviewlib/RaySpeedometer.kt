package com.github.anastr.speedviewlib

import android.content.Context
import android.graphics.*
import android.os.Build
import android.text.TextPaint
import android.util.AttributeSet
import com.github.anastr.speedviewlib.components.Section
import com.github.anastr.speedviewlib.components.indicators.Indicator
import ua.baidala.speedbar.R

/**
 * this Library build By Anas Altair
 * see it on [GitHub](https://github.com/anastr/SpeedView)
 */
open class RaySpeedometer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : Speedometer(context, attrs, defStyleAttr) {

    private val markPath = Path()
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val activeMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val speedBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var circleBounds = RectF()
    private var withEffects = true
    private var degreeBetweenMark = 5
    private var _speedLimit = 100f
    private var speedLimitKgBitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private var speedLimitKgCanvas: Canvas? = null
    private var speedLimitKgPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var speedLimitKgInterval = dpTOpx(3f)
    private var titleTextBitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private var titleTextCanvas: Canvas? = null

    protected var titleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    protected var titleTextSize = 20

    protected var kilogramText = "Kg"
    protected var kilogramTextSize = 20


    var titleText = ""
        set(titleText) {
            field = titleText
            if (isAttachedToWindow)
                invalidate()
        }

    var isWithEffects: Boolean
        get() = withEffects
        set(withEffects) {
            this.withEffects = withEffects
            if (isInEditMode)
                return
            indicator.withEffects(withEffects)
            if (withEffects) {
                rimPaint.maskFilter = null
                /*rimPaint.maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.SOLID)*/
                activeMarkPaint.maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.SOLID)
                speedBackgroundPaint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.SOLID)
            } else {
                rimPaint.maskFilter = null
                activeMarkPaint.maskFilter = null
                speedBackgroundPaint.maskFilter = null
            }
            invalidateGauge()
        }

    var speedBackgroundColor: Int
        get() = speedBackgroundPaint.color
        set(speedBackgroundColor) {
            speedBackgroundPaint.color = speedBackgroundColor
            invalidateGauge()
        }

    var markWidth: Float
        get() = markPaint.strokeWidth
        set(markWidth) {
            markPaint.strokeWidth = markWidth
            activeMarkPaint.strokeWidth = markWidth
            if (isAttachedToWindow)
                invalidate()
        }

    var rimColor: Int
        get() = rimPaint.color
        set(rimColor) {
            rimPaint.color = rimColor
            invalidateGauge()
        }

    /**
     * the normal range in speedometer, `default = 100`.
     *
     * @throws IllegalArgumentException if `minSpeed >= speedLimit`
     */
    var speedLimit: Float
        get() = _speedLimit
        set(value) = setSpeedLimits(minSpeed, value, maxSpeed)

    var speedLimitKgPosition = Position.BOTTOM_HALF_CENTER

    var titleTextPosition = Position.TOP_HALF_CENTER



    init {
        init()
        initAttributeSet(context, attrs)
    }

    override fun defaultGaugeValues() {
        super.textColor = 0xFFFFFFFF.toInt()
    }

    override fun defaultSpeedometerValues() {
        super.backgroundCircleColor = 0xff212121.toInt()
        super.markColor = 0xFF000000.toInt()
    }

    private fun initAttributeSet(context: Context, attrs: AttributeSet?) {
        if (attrs == null)
            return
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.RaySpeedometer, 0, 0)
        val titleText = a.getString(R.styleable.RaySpeedometer_sv_titleText)
        this.titleText = titleText ?: this.titleText
        speedLimit = a.getFloat(R.styleable.RaySpeedometer_sv_speedLimit, speedLimit)
        rimPaint.color = a.getColor(R.styleable.RaySpeedometer_sv_rimColor, rimPaint.color)
        val degreeBetweenMark = a.getInt(R.styleable.RaySpeedometer_sv_degreeBetweenMark, this.degreeBetweenMark)
        val markWidth = a.getDimension(R.styleable.RaySpeedometer_sv_markWidth, markPaint.strokeWidth)
        markPaint.strokeWidth = markWidth
        activeMarkPaint.strokeWidth = markWidth
        speedBackgroundPaint.color = a.getColor(R.styleable.RaySpeedometer_sv_speedBackgroundColor, speedBackgroundPaint.color)
        withEffects = a.getBoolean(R.styleable.RaySpeedometer_sv_withEffects, withEffects)
        a.recycle()
        isWithEffects = withEffects
        if (degreeBetweenMark in 1..20)
            this.degreeBetweenMark = degreeBetweenMark
    }

    private fun init() {
        markPaint.style = Paint.Style.STROKE
        markPaint.strokeWidth = dpTOpx(3f)
        activeMarkPaint.style = Paint.Style.STROKE
        activeMarkPaint.strokeWidth = dpTOpx(3f)
        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = dpTOpx(3f)
        rimPaint.color = 0xFFFFFFFF.toInt()
        speedBackgroundPaint.color = 0xFFFFFFFF.toInt()
        updatePaints()

        if (Build.VERSION.SDK_INT >= 11)
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        isWithEffects = withEffects
    }


    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)

        updateMarkPath()
        updatePaints()
        updateBounds()
        updateBackgroundBitmap()
        setSpeedLimitKgBitmap()
        setTitleTextBitmap()
    }

    private fun updatePaints() {
        titleTextPaint.color = 0xFF000000.toInt()
        titleTextPaint.style = Paint.Style.FILL
        titleTextPaint.isAntiAlias = true
        titleTextPaint.textSize = dpTOpx(titleTextSize.toFloat())

        speedLimitKgPaint.color = 0xFF000000.toInt()
        speedLimitKgPaint.style = Paint.Style.FILL
        speedLimitKgPaint.isAntiAlias = true
        speedLimitKgPaint.textSize = dpTOpx(kilogramTextSize.toFloat())
    }

    private fun setSpeedLimitKgBitmap() {
        if (widthPa > 0 && heightPa > 0)
            speedLimitKgBitmap = Bitmap.createBitmap(widthPa, heightPa, Bitmap.Config.ARGB_8888)
        speedLimitKgCanvas = Canvas(speedLimitKgBitmap)
    }

    private fun setTitleTextBitmap() {
        if (widthPa > 0 && heightPa > 0)
            titleTextBitmap = Bitmap.createBitmap(widthPa, heightPa, Bitmap.Config.ARGB_8888)
        titleTextCanvas = Canvas(titleTextBitmap)
    }

    private fun getSpeedLimitText() = speedTextListener.invoke(speedLimit)


    /***
     *  Speedometer.kt
     *  getStartDegree() -> startDegree = 115
     *  getEndDegree() -> endDegree = 115 + 310
     */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.rotate(getStartDegree() + 90f, size * .5f, size * .5f)
        var i = getStartDegree()
        while (i < getEndDegree()) {
            if (degree <= i) {
                markPaint.color = markColor
                canvas.drawPath(markPath, markPaint)
                canvas.rotate(degreeBetweenMark.toFloat(), size * .5f, size * .5f)
                i += degreeBetweenMark
                continue
            }
            var section = getSectionByDegree(i)
            if (section != null)
                activeMarkPaint.color = section!!.color
            else
                activeMarkPaint.color = 0 // transparent color

            canvas.drawPath(markPath, activeMarkPaint)
            canvas.rotate(degreeBetweenMark.toFloat(), size * .5f, size / 2f)
            i += degreeBetweenMark
        }
        canvas.restore()

        //Draw the rim
        canvas.drawArc(circleBounds, 360f, 360f, false, rimPaint)

        val speedBackgroundRect = getSpeedUnitTextBounds()
        speedBackgroundRect.left -= 2f
        speedBackgroundRect.right += 2f
        speedBackgroundRect.bottom += 2f
        canvas.drawRect(speedBackgroundRect, speedBackgroundPaint)

        drawTitleText(canvas)
        drawSpeedUnitText(canvas)
        drawSpeedLimitKgText(canvas)
    }

    override fun updateBackgroundBitmap() {
        val c = createBackgroundBitmapCanvas()

        updateMarkPath()
        updateBounds()

        if (tickNumber > 0)
            drawTicks(c)
        /*else
            drawDefMinMaxSpeedPosition(c)*/
    }

    private fun getSectionByDegree(degree: Int): Section? {
        sections.forEach {
            if ((getEndDegree() - getStartDegree()) * it.startOffset + getStartDegree() <= degree
                    && (getEndDegree() - getStartDegree()) * it.endOffset + getStartDegree() >= degree)
                return it
        }
        return null
    }

    private fun updateMarkPath() {
        markPath.reset()
        markPath.moveTo(size * .5f, padding.toFloat() + rimPaint.strokeWidth * 2)
        markPath.lineTo(size * .5f, speedometerWidth + padding + rimPaint.strokeWidth)
    }

    private fun updateBounds() {
        circleBounds = RectF(
                viewLeft + rimPaint.strokeWidth * .5f,
                viewTop + rimPaint.strokeWidth * .5f,
                viewRight - rimPaint.strokeWidth * .5f,
                viewBottom - rimPaint.strokeWidth * .5f)
    }

    protected fun drawSpeedLimitKgText(canvas: Canvas) {
        val r = getSpeedLimitKgTextBounds()
        updateSpeedLimitKgBitmap(getSpeedLimitText().toString())
        canvas.drawBitmap(speedLimitKgBitmap, r.left - speedLimitKgBitmap.width * .5f + r.width() * .5f
                , r.top - speedLimitKgBitmap.height * .5f + r.height() * .5f, speedLimitKgPaint)
    }

    protected fun drawTitleText(canvas: Canvas) {
        val r = getTitleTextBounds()
        updateTitleBitmap(titleText)
        canvas.drawBitmap(titleTextBitmap, r.left - titleTextBitmap.width * .5f + r.width() * .5f
                , r.top - titleTextBitmap.height * .5f + r.height() * .5f, titleTextPaint)
    }

    /**
     * speedLimit-kg text position and size.
     * @return rect RectF.
     */
    private fun getSpeedLimitKgTextBounds(): RectF {
        val left = widthPa * speedLimitKgPosition.x - translatedDx + padding -
                getSpeedLimitKgWidth() * speedLimitKgPosition.width + speedLimitKgPosition.paddingH
        val top = heightPa * speedLimitKgPosition.y - translatedDy + padding -
                getSpeedLimitKgHeight() * speedLimitKgPosition.height + speedLimitKgPosition.paddingV
        return RectF(left, top, left + getSpeedLimitKgWidth(), top + getSpeedLimitKgHeight())
    }

    private fun updateSpeedLimitKgBitmap(speedLimitText: String) {
        speedLimitKgBitmap.eraseColor(0)

        val speedLimitX: Float = speedLimitKgBitmap.width * .5f - getSpeedLimitKgWidth() * .5f
        val kilogramX: Float = speedLimitX + speedLimitKgPaint.measureText(speedLimitText) + speedLimitKgInterval
        val h = speedLimitKgBitmap.height * .5f + getSpeedLimitKgHeight() * .5f
        speedLimitKgCanvas?.drawText(speedLimitText, speedLimitX, h, speedLimitKgPaint)
        speedLimitKgCanvas?.drawText(kilogramText, kilogramX, h, speedLimitKgPaint)
    }

    /**
     * @return the width of speedLimit & Kg text at runtime.
     */
    private fun getSpeedLimitKgWidth(): Float =
            speedLimitKgPaint.measureText(getSpeedText().toString()) + speedLimitKgPaint.measureText(kilogramText) + speedLimitKgInterval

    /**
     * @return the height of speedLimit & Kg text at runtime.
     */
    private fun getSpeedLimitKgHeight(): Float = speedLimitKgPaint.textSize


    private fun getTitleTextBounds(): RectF {
        val left = widthPa * titleTextPosition.x - translatedDx + padding -
                getTitleTextWidth() * titleTextPosition.width + titleTextPosition.paddingH
        val top = heightPa * titleTextPosition.y - translatedDy + padding -
                getTitleTextHeight() * titleTextPosition.height + titleTextPosition.paddingV
        return RectF(left, top, left + getTitleTextWidth(), top + getTitleTextHeight())
    }

    private fun getTitleTextWidth(): Float =
            titleTextPaint.measureText(titleText)

    private fun getTitleTextHeight(): Float = titleTextPaint.textSize

    private fun updateTitleBitmap(titleText: String) {
        titleTextBitmap.eraseColor(0)

        val titleTextX: Float = titleTextBitmap.width * .5f - getTitleTextWidth() * .5f
        val h = titleTextBitmap.height * .5f + getTitleTextHeight() * .5f
        titleTextCanvas?.drawText(titleText, titleTextX, h, titleTextPaint)
    }

    override fun setIndicator(indicator: Indicator.Indicators) {
        super.setIndicator(indicator)
        this.indicator.withEffects(withEffects)
    }

    fun getDegreeBetweenMark(): Int {
        return degreeBetweenMark
    }

    fun setSpeedLimits(minSpeed: Float, speedLimit: Float, maxSpeed: Float) {
        require(minSpeed < speedLimit) { "minSpeed must be smaller than speedLimit !!" }
        require(speedLimit < maxSpeed) { "speedLimit must be smaller than maxSpeed !!" }
        cancelSpeedAnimator()
        _speedLimit = speedLimit
        invalidateGauge()
        if (isAttachedToWindow)
            setSpeedAt(speed)
    }

    /**
     * The spacing between the marks
     *
     * it should be between (0-20] ,else well be ignore.
     *
     * @param degreeBetweenMark degree between two marks.
     */
    fun setDegreeBetweenMark(degreeBetweenMark: Int) {
        if (degreeBetweenMark <= 0 || degreeBetweenMark > 20)
            return
        this.degreeBetweenMark = degreeBetweenMark
        if (isAttachedToWindow)
            invalidate()
    }
}
