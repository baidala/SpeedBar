package com.github.anastr.speedviewlib

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
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

//    /**
//     * this Speedometer doesn't use this method.
//     * @return `Color.TRANSPARENT` always.
//     */
//    override var indicatorColor: Int
//        @Deprecated("")
//        get() = 0
//        @Deprecated("")
//        set(indicatorColor) {
//        }

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

        if (Build.VERSION.SDK_INT >= 11)
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        isWithEffects = withEffects
    }


    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)

        updateMarkPath()
        updateBounds()
        updateBackgroundBitmap()
    }


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
            if (currentSection != null)
                activeMarkPaint.color = currentSection!!.color
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

        drawSpeedUnitText(canvas)
        /*drawIndicator(canvas)*/
        /*drawNotes(canvas)*/
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

    private fun updateMarkPath() {
        markPath.reset()
        markPath.moveTo(size * .5f, padding.toFloat() + rimPaint.strokeWidth * 2)
        markPath.lineTo(size * .5f, speedometerWidth + padding + rimPaint.strokeWidth)
    }

    private fun updateBounds() {
        circleBounds = RectF(
                viewLeft + rimPaint.strokeWidth/2,
                viewTop + rimPaint.strokeWidth/2,
                viewRight - rimPaint.strokeWidth/2,
                viewBottom - rimPaint.strokeWidth/2)
    }



    override fun setIndicator(indicator: Indicator.Indicators) {
        super.setIndicator(indicator)
        this.indicator.withEffects(withEffects)
    }

    fun getDegreeBetweenMark(): Int {
        return degreeBetweenMark
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
