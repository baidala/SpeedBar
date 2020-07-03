package com.github.anastr.speedviewlib

import android.content.Context
import android.graphics.*
import android.os.Build
import android.text.TextPaint
import android.util.AttributeSet
import com.github.anastr.speedviewlib.components.Section
import com.github.anastr.speedviewlib.components.indicators.Indicator
import ua.baidala.speedbar.R


open class DistributionSpeedometer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : Speedometer(context, attrs, defStyleAttr) {

    private val markPath = Path()
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val activeMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var circleBounds = RectF()
    private var withEffects = false
    private var degreeBetweenMark = 5
    private var _speedLimit = 100f
    private var speedLimitKgBitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private var speedLimitKgCanvas: Canvas? = null
    private var speedLimitKgPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var speedLimitKgInterval = dpTOpx(3f)
    private var speedLimitKgPosition = Position.BOTTOM_HALF_CENTER
    private var titleTextBitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private var titleTextCanvas: Canvas? = null
    private var titleTextPosition = Position.TOP_HALF_CENTER
    private var titleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var kilogramTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val _startDegree = 90
    private val _endDegree = 415
    private val _overloadDegree = 125

    var kilogramText = "kg"
        set(kilogramText) {
            field = kilogramText
            if (isAttachedToWindow)
                invalidate()
        }


    var titleText = ""
        set(titleText) {
            field = titleText
            if (isAttachedToWindow)
                invalidate()
        }

    /**
     * change just title text size.
     *
     * @see dpTOpx
     * @see textSize
     * @see unitTextSize
     */
    var titleTextSize: Float
        get() = titleTextPaint.textSize
        set(titleTextSize) {
            titleTextPaint.textSize = titleTextSize
            if (isAttachedToWindow)
                invalidateGauge()
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
//                speedBackgroundPaint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.SOLID)
            } else {
                rimPaint.maskFilter = null
                activeMarkPaint.maskFilter = null
//                speedBackgroundPaint.maskFilter = null
            }
            invalidateGauge()
        }

    /*var speedBackgroundColor: Int
        get() = speedBackgroundPaint.color
        set(speedBackgroundColor) {
            speedBackgroundPaint.color = speedBackgroundColor
            invalidateGauge()
        }*/

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

    var speedLimitKgTextSize: Float
        get() = speedLimitKgPaint.textSize
        set(speedLimitTextSize) {
            speedLimitKgPaint.textSize = speedLimitTextSize
            if (isAttachedToWindow)
                invalidateGauge()
        }

    var drawSpeedLimit = true
        set(drawSpeedLimit) {
            field = drawSpeedLimit
            if (isAttachedToWindow)
                invalidateGauge()
        }



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
        titleTextPaint.color = a.getColor(R.styleable.RaySpeedometer_sv_titleTextColor, titleTextPaint.color)
        titleTextPaint.textSize = a.getDimension(R.styleable.RaySpeedometer_sv_titleTextSize, titleTextPaint.textSize)
        val kilogramText = a.getString(R.styleable.RaySpeedometer_sv_kilogramText)
        this.kilogramText = kilogramText ?: this.kilogramText
        kilogramTextPaint.color = a.getColor(R.styleable.RaySpeedometer_sv_kilogramTextColor, kilogramTextPaint.color)
        kilogramTextPaint.textSize = a.getDimension(R.styleable.RaySpeedometer_sv_kilogramTextSize, kilogramTextPaint.textSize)
        drawSpeedLimit = a.getBoolean(R.styleable.RaySpeedometer_sv_drawSpeedLimit, true)
        speedLimit = a.getFloat(R.styleable.RaySpeedometer_sv_speedLimit, speedLimit)
        speedLimitKgPaint.color = a.getColor(R.styleable.RaySpeedometer_sv_speedLimitColor, speedLimitKgPaint.color)
        speedLimitKgPaint.textSize = a.getDimension(R.styleable.RaySpeedometer_sv_speedLimitSize, speedLimitKgPaint.textSize)
        rimPaint.color = a.getColor(R.styleable.RaySpeedometer_sv_rimColor, rimPaint.color)
        val degreeBetweenMark = a.getInt(R.styleable.RaySpeedometer_sv_degreeBetweenMark, this.degreeBetweenMark)
        val markWidth = a.getDimension(R.styleable.RaySpeedometer_sv_markWidth, markPaint.strokeWidth)
        markPaint.strokeWidth = markWidth
        activeMarkPaint.strokeWidth = markWidth
        withEffects = a.getBoolean(R.styleable.RaySpeedometer_sv_withEffects, withEffects)
        a.recycle()
        isWithEffects = withEffects
        if (degreeBetweenMark in 1..20)
            this.degreeBetweenMark = degreeBetweenMark
    }

    private fun init() {
        setStartEndDegree(_startDegree, _endDegree)

        markPaint.style = Paint.Style.STROKE
        markPaint.strokeWidth = dpTOpx(3f)

        activeMarkPaint.style = Paint.Style.STROKE
        activeMarkPaint.strokeWidth = dpTOpx(3f)

        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = dpTOpx(5f)
        rimPaint.color = 0xFF368003.toInt()

        titleTextPaint.color = 0xFF000000.toInt()
        titleTextPaint.isAntiAlias = true
        titleTextPaint.textSize = dpTOpx(20f)

        speedLimitKgPaint.color = 0xFF000000.toInt()
        speedLimitKgPaint.isAntiAlias = true
        speedLimitKgPaint.textSize = dpTOpx(20f)

        kilogramTextPaint.color = 0xFF000000.toInt()
        kilogramTextPaint.isAntiAlias = true
        kilogramTextPaint.textSize = dpTOpx(16f)

        if (Build.VERSION.SDK_INT >= 11)
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        isWithEffects = withEffects
    }


    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)

        updateMarkPath()
        updateBounds()
        updateBackgroundBitmap()
        setSpeedLimitKgBitmap()
        setTitleTextBitmap()
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
     *  getStartDegree() -> startDegree = 125
     *  getEndDegree() -> endDegree = 450
     */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw marks
        canvas.save()
        canvas.rotate(getStartDegree() + 90f, size * .5f, size * .5f)

        var i = getStartDegree()
        while (i < getEndDegree()) {
            if (degree <= i && i >= _overloadDegree) {
                activeMarkPaint.color = markColor
            } else if (degree >= _overloadDegree && i <_overloadDegree) {
                activeMarkPaint.color = 0
            } else if (degree > i) {
                var section = getSectionByDegree(i)
                if (section != null)
                    activeMarkPaint.color = section!!.color
                else
                    activeMarkPaint.color = 0 // transparent color
            }
            canvas.drawPath(markPath, activeMarkPaint)
            canvas.rotate(degreeBetweenMark.toFloat(), size * .5f, size / 2f)
            i += degreeBetweenMark
        }
        canvas.restore()

        //Draw the rim
        canvas.drawArc(circleBounds, 360f, 360f, false, rimPaint)

        drawTitleText(canvas)
        drawSpeedUnitText(canvas)
        if (drawSpeedLimit) { drawSpeedLimitKgText(canvas) }
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
                , r.top - speedLimitKgBitmap.height * .5f + r.height() * .5f, textBitmapPaint)
    }

    protected fun drawTitleText(canvas: Canvas) {
        val r = getTitleTextBounds()
        updateTitleBitmap(titleText)
        canvas.drawBitmap(titleTextBitmap, r.left - titleTextBitmap.width * .5f + r.width() * .5f
                , r.top - titleTextBitmap.height * .5f + r.height() * .5f, textBitmapPaint)
    }

    /**
     * speedLimit-kg text position and size.
     * @return rect RectF.
     */
    private fun getSpeedLimitKgTextBounds(): RectF {
        val left = widthPa * speedLimitKgPosition.x - translatedDx + padding -
                getSpeedLimitKgWidth() * speedLimitKgPosition.width + padding * speedLimitKgPosition.paddingH
        val top = heightPa * speedLimitKgPosition.y - translatedDy + padding -
                getSpeedLimitKgHeight() * speedLimitKgPosition.height + padding * speedLimitKgPosition.paddingV
        return RectF(left, top, left + getSpeedLimitKgWidth(), top + getSpeedLimitKgHeight())
    }

    private fun updateSpeedLimitKgBitmap(speedLimitText: String) {
        speedLimitKgBitmap.eraseColor(0)

        var speedLimitX: Float = speedLimitKgBitmap.width * .5f - getSpeedLimitKgWidth() * .5f
        var speedLimitY: Float = speedLimitKgBitmap.height * .5f + speedLimitKgInterval
        speedLimitKgCanvas?.drawText(speedLimitText, speedLimitX, speedLimitY, speedLimitKgPaint)
        speedLimitX = speedLimitKgBitmap.width * .5f - kilogramTextPaint.measureText(kilogramText) * .5f
        speedLimitY += (getSpeedLimitKgHeight() * .5f) + speedLimitKgInterval
        speedLimitKgCanvas?.drawText(kilogramText, speedLimitX, speedLimitY, kilogramTextPaint)
    }

    /**
     * @return the width of speedLimit & Kg text at runtime.
     */
    private fun getSpeedLimitKgWidth(): Float =
            speedLimitKgPaint.measureText(getSpeedLimitText().toString())
    /**
     * @return the height of speedLimit & Kg text at runtime.
     */
    private fun getSpeedLimitKgHeight(): Float = speedLimitKgPaint.descent() - speedLimitKgPaint.ascent()


    private fun getTitleTextBounds(): RectF {
        val left = widthPa * titleTextPosition.x - translatedDx + padding -
                getTitleTextWidth() * titleTextPosition.width + titleTextPosition.paddingH
        val top = heightPa * titleTextPosition.y - translatedDy + padding -
                getTitleTextHeight() * titleTextPosition.height + titleTextPosition.paddingV
        return RectF(left, top, left + getTitleTextWidth(), top + getTitleTextHeight())
    }

    private fun getTitleTextWidth(): Float =
            titleTextPaint.measureText(titleText)

    private fun getTitleTextHeight(): Float = titleTextPaint.descent() - titleTextPaint.ascent()

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
        require(minSpeed <= speedLimit) { "minSpeed must be smaller than speedLimit !!" }
        require(speedLimit <= maxSpeed) { "speedLimit must be smaller than maxSpeed !!" }
        cancelSpeedAnimator()
        _speedLimit = speedLimit
        setMinMaxSpeed(minSpeed, maxSpeed)
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
