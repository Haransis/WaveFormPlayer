package fr.haran.soundwave.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import fr.haran.soundwave.R
import kotlin.math.roundToInt
import kotlin.properties.Delegates

private const val TAG = "RecView"
private const val MAX_AMPLITUDE = -Short.MIN_VALUE*2
class RecView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var recordPaint: Paint
    private var availableWidth by Delegates.notNull<Int>()
    private var availableHeight by Delegates.notNull<Int>()
    private var origin by Delegates.notNull<Int>()
    private var barWidth by Delegates.notNull<Float>()
    private var barHeight by Delegates.notNull<Float>()
    private val waveForm: Path = Path()

    var samples: Int? = null
        set(value){
            field = value
            invalidate()
            requestLayout()
        }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SoundWaveView,
            0, 0).apply {

            try {
                recordColor = getColor(
                    R.styleable.RecView_recview_color, ContextCompat.getColor(context,
                        R.color.colorPrimary
                    ))
            } finally {
                recycle()
            }
        }
        recordPaint = Paint()
        recordPaint.apply {
            color = recordColor
            flags = Paint.ANTI_ALIAS_FLAG
            strokeWidth = 20F
            style = Paint.Style.STROKE
        }
    }

    @ColorRes
    var recordColor: Int
        set(value){
            field = value
            invalidate()
            requestLayout()
        }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        recordPaint.apply {
            color = recordColor
            flags = Paint.ANTI_ALIAS_FLAG
            strokeWidth = 3F
            Paint.Style.STROKE
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        samples?.let {
            val xpad = (paddingLeft + paddingRight).toFloat()
            val ypad = (paddingTop + paddingBottom).toFloat()
            availableWidth = (w.toFloat() - xpad).roundToInt()
            availableHeight = (h.toFloat() - ypad).roundToInt()
            origin = availableHeight / 2
            barWidth = availableWidth.toFloat() / it
            barHeight = availableHeight.toFloat() / MAX_AMPLITUDE
            waveForm.rewind()
            waveForm.moveTo(0F, origin.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            drawPath(waveForm, recordPaint)
        }
    }

    fun addAmplitude(dy: Int){
        waveForm.rLineTo(barWidth, dy*barHeight)
        invalidate()
    }

    fun resetAmplitudes() {
        waveForm.reset()
        waveForm.moveTo(0F, origin.toFloat())
        invalidate()
    }

    fun endLine(){
        waveForm.lineTo(availableWidth.toFloat(), origin.toFloat())
        invalidate()
    }
}