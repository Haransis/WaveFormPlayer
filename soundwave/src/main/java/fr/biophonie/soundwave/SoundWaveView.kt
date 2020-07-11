package fr.biophonie.soundwave

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import kotlin.properties.Delegates

open class SoundWaveView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var playedPaint: Paint
    private var nonPlayedPaint: Paint
    private var progression: Float = 0F
    private var availableWidth by Delegates.notNull<Int>()
    private var availableHeight by Delegates.notNull<Int>()
    private var origin by Delegates.notNull<Int>()
    private var barWidth by Delegates.notNull<Float>()
    var amplitudes = arrayOf(0.0)
        set(value){
            field = value
            invalidate()
        }
    private val waveForm: Path = Path()

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SoundWaveView,
            0, 0).apply {

            try {
                playedColor = getColor(R.styleable.SoundWaveView_playedColor, ContextCompat.getColor(context, R.color.colorPrimary))
                nonPlayedColor = getColor(R.styleable.SoundWaveView_nonPlayedColor, ContextCompat.getColor(context, R.color.colorPrimaryDark))
            } finally {
                recycle()
            }
        }
        nonPlayedPaint = Paint()
        playedPaint = Paint()
    }

    private fun Path.buildPath(array: Array<Double>){
        this.rewind()
        this.moveTo(0F, origin.toFloat())
        for (i in array.indices){
            this.lineTo(i*barWidth, ((1+array[i])*origin).toFloat())
            //path.lineTo(i*barWidth, ((-amplitudesDB[i])*origin).toFloat())
        }
        /*for (i in amplitudes.indices.reversed()){
                path.lineTo(i*barWidth, ((2+amplitudesDB[i])*origin).toFloat())
            }*/
    }

    @ColorRes var playedColor: Int
        set(value){
            field = value
            invalidate()
            requestLayout()
        }

    @ColorRes var nonPlayedColor: Int
        set(value){
            field = value
            invalidate()
            requestLayout()
        }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        playedPaint.apply {
            color = playedColor
            style = Paint.Style.STROKE
            flags = ANTI_ALIAS_FLAG
            strokeWidth = 3.1F
        }
        nonPlayedPaint.apply {
            color = nonPlayedColor
            style = Paint.Style.STROKE
            flags = ANTI_ALIAS_FLAG
            strokeWidth = 3F
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val xpad = (paddingLeft + paddingRight).toFloat()
        val ypad = (paddingTop + paddingBottom).toFloat()
        availableWidth = (w.toFloat() - xpad).roundToInt()
        availableHeight = (h.toFloat() - ypad).roundToInt()
        origin = availableHeight/2
        barWidth = availableWidth.toFloat() / amplitudes.size
        waveForm.buildPath(amplitudes)
    }

    fun updateProgression(percent: Float) {
        progression = percent
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            drawPath(waveForm, nonPlayedPaint)
            clipRect((availableWidth*progression).toInt(),0,availableWidth,availableHeight)
            drawPath(waveForm, playedPaint)
        }
    }
}