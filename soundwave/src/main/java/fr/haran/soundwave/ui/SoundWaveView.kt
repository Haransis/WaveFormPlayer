package fr.haran.soundwave.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import fr.haran.soundwave.R
import timber.log.Timber
import kotlin.math.roundToInt
import kotlin.properties.Delegates

open class SoundWaveView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var playedPaint: Paint
    private var nonPlayedPaint: Paint
    private var progression: Float = 0F
    private var availableWidth by Delegates.notNull<Int>()
    private var availableHeight by Delegates.notNull<Int>()
    private var origin by Delegates.notNull<Float>()
    private var barWidth by Delegates.notNull<Float>()
    private var barHeight by Delegates.notNull<Float>()
    var maxAmplitude = 0f
    var amplitudes = listOf(0f)
        set(value){
            field = value
            maxAmplitude = field.maxOrNull() ?: 1f
            measureAmplitudesDimensions(width,height)
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
                shouldReflect = getBoolean(R.styleable.SoundWaveView_reflection, false)
            } finally {
                recycle()
            }
        }
        nonPlayedPaint = Paint()
        playedPaint = Paint()
    }

    private fun Path.buildPath(start: Int, finish: Int){
        this.reset()
        this.moveTo(start*barWidth, origin)
        if (!shouldReflect) {
            for (i in (start until finish)) {
                this.lineTo(i * barWidth, (origin + amplitudes[i] * barHeight))
            }
        } else {
            for (i in (start until finish)) {
                this.lineTo(i*barWidth, (amplitudes[i] * barHeight))
                this.lineTo(i*barWidth, (2 * origin - amplitudes[i] * barHeight))
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

    var shouldReflect: Boolean
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
        measureAmplitudesDimensions(w,h)
    }

    private fun measureAmplitudesDimensions(width: Int, height: Int) {
        val xpad = (paddingLeft + paddingRight).toFloat()
        val ypad = (paddingTop + paddingBottom).toFloat()
        availableWidth = (width.toFloat() - xpad).roundToInt()
        availableHeight = (height.toFloat() - ypad).roundToInt()
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
        Timber.d("$maxAmplitude")
        canvas?.apply {
            when (val index = (amplitudes.size*progression).toInt()) {
                0, 1 -> {
                    waveForm.buildPath(0,amplitudes.size)
                    drawPath(waveForm, nonPlayedPaint)
                }
                in 2..(amplitudes.size-6) -> {
                    waveForm.buildPath(0,index+5)
                    drawPath(waveForm, playedPaint)
                    waveForm.buildPath(index,amplitudes.size)
                    drawPath(waveForm, nonPlayedPaint)
                }
                in (amplitudes.size-5) until amplitudes.size -> {
                    waveForm.buildPath(0,index)
                    drawPath(waveForm, nonPlayedPaint)
                    waveForm.buildPath(index-5, amplitudes.size)
                    drawPath(waveForm, playedPaint)
                }
                else -> {
                    waveForm.buildPath(0,amplitudes.size)
                    drawPath(waveForm, playedPaint)
                }
            }
        }
    }
}