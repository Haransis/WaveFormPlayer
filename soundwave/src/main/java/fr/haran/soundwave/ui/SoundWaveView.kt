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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.properties.Delegates

private const val TAG = "SoundWaveView"
open class SoundWaveView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var playedPaint: Paint
    private var nonPlayedPaint: Paint
    private var progression: Float = 0F
    private var availableWidth by Delegates.notNull<Int>()
    private var availableHeight by Delegates.notNull<Int>()
    private var origin by Delegates.notNull<Float>()
    private var barWidth by Delegates.notNull<Float>()
    private var barHeight by Delegates.notNull<Float>()
    private val aCoordinates = floatArrayOf(0f, 0f)
    private val measure = PathMeasure()
    var maxAmplitude = 0f
    var amplitudes = listOf(0f)
        set(value){
            field = value
            maxAmplitude = field.maxOrNull() ?: 1f
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

    private fun Path.buildPlayedPath(array: List<Float>){
        this.reset()
        this.moveTo(0f, origin)
        if (!isDb) {
            for (i in array.indices) {
                this.lineTo(i * barWidth, (origin + array[i] * barHeight))
            }
        } else {
            for (i in array.indices) {
                this.lineTo(i*barWidth, (array[i] * barHeight))
                this.lineTo(i*barWidth, (2 * origin - array[i] * barHeight))
            }
        }
    }

    private fun Path.buildNonPlayedPath(array: List<Float>){
        this.reset()
        this.moveTo(availableWidth.toFloat(), origin)
        if (!isDb) {
            for (i in array.indices) {
                this.lineTo(availableWidth - i * barWidth, (origin + array[array.size-1-i] * barHeight))
            }
        } else {
            for (i in array.indices) {
                this.lineTo(availableWidth - i * barWidth, (array[i]*barHeight))
                this.lineTo(availableWidth - i * barWidth, (2*origin - array[i] * barHeight))
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
        style = Paint.Style.STROKE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val xpad = (paddingLeft + paddingRight).toFloat()
        val ypad = (paddingTop + paddingBottom).toFloat()
        availableWidth = (w.toFloat() - xpad).roundToInt()
        availableHeight = (h.toFloat() - ypad).roundToInt()
        origin = availableHeight/2f
        barWidth = availableWidth.toFloat() / amplitudes.size
        barHeight = availableHeight.toFloat() / maxAmplitude / 2f
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