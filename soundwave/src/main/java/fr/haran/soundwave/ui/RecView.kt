package fr.haran.soundwave.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
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
private const val STROKE_WIDTH = 3F
class RecView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var iteration = 0
    private var progression: Float = 0F
    private val recordPaint: Paint = Paint()
    private val playPaint: Paint = Paint()
    var isPlaying = false
    private var availableWidth by Delegates.notNull<Int>()
    private var availableHeight by Delegates.notNull<Int>()
    private var origin by Delegates.notNull<Int>()
    private var barWidth by Delegates.notNull<Float>()
    private var barHeight by Delegates.notNull<Float>()
    private val waveForm: Path = Path()
    private val measure = PathMeasure()
    private val aCoordinates = floatArrayOf(0f, 0f)

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
                        R.color.colorPrimaryDark
                    ))
                playColor = getColor(
                    R.styleable.RecView_recview_playedColor, ContextCompat.getColor(context,
                        R.color.colorPrimary
                    ))
            } finally {
                recycle()
            }
        }
        initializePaint(recordPaint, recordColor)
        initializePaint(playPaint, playColor)
    }

    private fun initializePaint(paint: Paint, paintColor: Int) {
        paint.apply {
            color = paintColor
            flags = Paint.ANTI_ALIAS_FLAG
            strokeWidth = STROKE_WIDTH
            style = Paint.Style.STROKE
        }
    }

    @ColorRes
    var recordColor: Int
        set(value){
            field = value
            requestLayout()
        }

    @ColorRes
    var playColor: Int
        set(value){
            field = value
            requestLayout()
        }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        recordPaint.color = recordColor
        playPaint.color = playColor
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
            if (isPlaying){
                measure.setPath(waveForm, false)
                measure.getPosTan(measure.length, aCoordinates, null)
                clipRect((aCoordinates[0]*progression).toInt(),0,availableWidth,availableHeight)
                drawPath(waveForm, playPaint)
            }
        }
    }

    fun addAmplitude(y: Int){
        waveForm.lineTo(iteration*barWidth, origin+y*barHeight)
        iteration++
        invalidate()
    }

    fun resetAmplitudes() {
        waveForm.reset()
        waveForm.moveTo(0F, origin.toFloat())
        iteration = 0
        invalidate()
    }

    fun endLine(){
        waveForm.lineTo(availableWidth.toFloat(), origin.toFloat())
        invalidate()
    }

    fun updateProgression(percent: Float) {
        progression = percent
        invalidate()
    }

    fun getRealWidth(): Float{
        measure.setPath(waveForm, false)
        measure.getPosTan(measure.length, aCoordinates, null)
        return aCoordinates[0]
    }
}