package fr.haran.soundwave.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import fr.haran.soundwave.R
import kotlin.math.roundToInt
import kotlin.properties.Delegates

private const val TAG = "SoundWaveView"
open class SoundWaveView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var playedPaint: Paint
    private var nonPlayedPaint: Paint
    private var progression: Float = 0F
    private var availableWidth by Delegates.notNull<Int>()
    private var availableHeight by Delegates.notNull<Int>()
    private var origin by Delegates.notNull<Int>()
    private var barWidth by Delegates.notNull<Float>()
    private var barHeight by Delegates.notNull<Float>()
    private val aCoordinates = floatArrayOf(0f, 0f)
    private val measure = PathMeasure()
    var amplitudes = listOf(0.0)
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
                playedColor = getColor(
                    R.styleable.SoundWaveView_playedColor, ContextCompat.getColor(context,
                        R.color.colorPrimary
                    ))
                nonPlayedColor = getColor(
                    R.styleable.SoundWaveView_nonPlayedColor, ContextCompat.getColor(context,
                        R.color.colorPrimaryDark
                    ))
                isDb = getBoolean(R.styleable.SoundWaveView_db, false)
            } finally {
                recycle()
            }
        }
        nonPlayedPaint = Paint()
        playedPaint = Paint()
    }

    private fun Path.buildPlayedPath(array: List<Double>){
        this.reset()
        this.moveTo(0f, origin.toFloat())
        if (!isDb) {
            for (i in array.indices) {
                this.lineTo(i * barWidth, (origin + array[i] * barHeight).toFloat())
            }
        } else {
            for (i in array.indices) {
                this.lineTo(i*barWidth, (origin - array[i] * barHeight).toFloat())
            }
            for (i in amplitudes.indices.reversed()){
                this.lineTo(i*barWidth, (origin*2 + array[i] * barHeight).toFloat())
            }
        }
    }

    private fun Path.buildNonPlayedPath(array: List<Double>){
        this.reset()
        this.moveTo(availableWidth.toFloat(), origin.toFloat())
        if (!isDb) {
            for (i in array.indices) {
                this.lineTo(availableWidth - i * barWidth, (origin + array[array.size-1-i] * barHeight).toFloat())
            }
        } else {
            for (i in array.indices.reversed()) {
                this.lineTo(availableWidth - i * barWidth, (origin - array[i] * barHeight).toFloat())
            }
            for (i in amplitudes.indices){
                this.lineTo(availableWidth - i * barWidth, (origin * 2 + array[i] * barHeight).toFloat())
            }
        }
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

    var isDb: Boolean
        set(value){
            field = value
            invalidate()
            requestLayout()
        }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        playedPaint.configurePaint(playedColor)
        nonPlayedPaint.configurePaint(nonPlayedColor)
    }

    private fun Paint.configurePaint(colorInt: Int){
        color = colorInt
        flags = ANTI_ALIAS_FLAG
        strokeWidth = 3F
        style = if (!isDb)
            Paint.Style.STROKE
        else
            Paint.Style.FILL_AND_STROKE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val xpad = (paddingLeft + paddingRight).toFloat()
        val ypad = (paddingTop + paddingBottom).toFloat()
        availableWidth = (w.toFloat() - xpad).roundToInt()
        availableHeight = (h.toFloat() - ypad).roundToInt()
        origin = availableHeight/2
        barWidth = availableWidth.toFloat() / amplitudes.size
        barHeight = availableHeight.toFloat() / MAX_AMPLITUDE
    }

    fun updateProgression(percent: Float) {
        progression = percent
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            when (val index = (amplitudes.size*progression).roundToInt()) {
                0, 1 -> {
                    waveForm.buildNonPlayedPath(amplitudes)
                    drawPath(waveForm, nonPlayedPaint)
                }
                in 2..(amplitudes.size-6) -> {
                    val firstPart = amplitudes.subList(0, index+5)
                    val secondPart = amplitudes.subList(index, amplitudes.size)
                    waveForm.buildPlayedPath(firstPart)
                    drawPath(waveForm, playedPaint)
                    waveForm.buildNonPlayedPath(secondPart)
                    drawPath(waveForm, nonPlayedPaint)
                }
                in (amplitudes.size-5) until amplitudes.size -> {
                    val firstPart = amplitudes.subList(0, index)
                    val secondPart = amplitudes.subList(index-5, amplitudes.size)
                    waveForm.buildNonPlayedPath(secondPart)
                    drawPath(waveForm, nonPlayedPaint)
                    waveForm.buildPlayedPath(firstPart)
                    drawPath(waveForm, playedPaint)
                }
                else -> {
                    waveForm.buildPlayedPath(amplitudes)
                    drawPath(waveForm, playedPaint)
                }
            }
        }
    }
}