package fr.haran.soundwave.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.haran.soundwave.controller.PlayerController
import fr.haran.soundwave.R
import fr.haran.soundwave.utils.Utils
import kotlin.math.abs


private const val TAG = "PlayerView"
open class PlayerView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs), View.OnTouchListener, ControllingView{

    private val mTouchSlop: Int = android.view.ViewConfiguration.get(context).scaledTouchSlop
    private var isScrolling: Boolean = false
    private var firstEventX: Float? = null
    private lateinit var soundWaveView: SoundWaveView
    private lateinit var play: FloatingActionButton
    private lateinit var pause: ImageButton
    private lateinit var timer: TextView
    private lateinit var title: TextView
    private lateinit var playerController: PlayerController

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PlayerView,
            0, 0).apply {
            try {
                mainColor = getColor(
                    R.styleable.PlayerView_mainColor, ContextCompat.getColor(context,
                        R.color.colorPrimary
                    ))
                secondaryColor = getColor(
                    R.styleable.PlayerView_secondaryColor, ContextCompat.getColor(context,
                        R.color.colorPrimaryDark
                    ))
                text = getString(R.styleable.PlayerView_title)
                isDb = getBoolean(R.styleable.PlayerView_waveDb, false)
            } finally {
                recycle()
            }
        }
        initView(context)
    }

    override fun onPlay(){
        play.setImageResource(R.drawable.ic_play)
        play.visibility = View.GONE
        pause.visibility = View.VISIBLE
    }

    override fun onPause(){
        play.visibility = View.VISIBLE
        pause.visibility = View.GONE
    }

    override fun onComplete(){
        play.setImageResource(R.drawable.ic_reload)
        timer.text = Utils.millisToString(0)
        play.visibility = View.VISIBLE
        pause.visibility = View.GONE
    }

    override fun updatePlayerPercent(duration: Int, currentPosition: Int){
        if(!isScrolling)
            soundWaveView.updateProgression(currentPosition / duration.toFloat())
        timer.text = Utils.millisToString(if (duration <= currentPosition) 0
            else (duration - currentPosition).toLong() )
    }

    override fun attachPlayerController(playerController: PlayerController){
        this.playerController = playerController
    }

    @SuppressLint("InflateParams")
    private fun initView(context: Context) {
        val view: View = LayoutInflater.from(context).inflate(R.layout.player_view, this, true)
        soundWaveView = view.findViewById<SoundWaveView>(
            R.id.sound_wave_view
        ).apply{
            playedColor = mainColor
            nonPlayedColor = secondaryColor
            isDb = this@PlayerView.isDb
            setOnTouchListener(this@PlayerView)
        }
        play = view.findViewById<FloatingActionButton>(R.id.play).apply{
            imageTintList = ColorStateList.valueOf(mainColor)
            foregroundTintList = ColorStateList.valueOf(mainColor)
            setOnClickListener { playerController.toggle() } }
        pause = view.findViewById<ImageButton>(R.id.pause).apply{
            backgroundTintList = ColorStateList.valueOf(secondaryColor)
            imageTintList = ColorStateList.valueOf(mainColor)
            foregroundTintList = ColorStateList.valueOf(mainColor)
            setOnClickListener { playerController.toggle() } }
        title = view.findViewById<TextView>(R.id.sound_title).apply {
            text = this@PlayerView.text
        }
        timer = view.findViewById(R.id.duration)
        setSoundWaveColor()
    }

    private fun setSoundWaveColor(){
        if (this::soundWaveView.isInitialized){
            soundWaveView.nonPlayedColor = secondaryColor
            soundWaveView.playedColor = mainColor
        }
    }

    override fun setAmplitudes(amplitudes: Array<Double>){
        soundWaveView.amplitudes = amplitudes.toList()
    }

    override fun <T>setText(title: T){
        this.title.text = title as CharSequence
    }

    @ColorRes
    var mainColor: Int
        set(value){
            field = value
            setSoundWaveColor()
        }

    @ColorRes
    var secondaryColor: Int
        set(value){
            field = value
            setSoundWaveColor()
        }

    var text: String?
        set(value){
            field = value
            invalidate()
        }

    var isDb: Boolean
        set(value){
            field = value
            invalidate()
        }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return if (this::playerController.isInitialized && playerController.isPlaying()){
            when(event.actionMasked){
                MotionEvent.ACTION_DOWN -> true
                MotionEvent.ACTION_MOVE -> {
                    if (isScrolling) {
                        soundWaveView.updateProgression(event.x / v.width)
                        true
                    } else {
                        val xDiff = calculateDistanceX(event)
                        if (abs(xDiff) > mTouchSlop) {
                            this.parent.requestDisallowInterceptTouchEvent(true)
                            isScrolling = true
                            soundWaveView.updateProgression(event.x / v.width)
                            true
                        } else {
                            false
                        }
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    this.parent.requestDisallowInterceptTouchEvent(false)
                    firstEventX = null
                    isScrolling = false
                    false
                }
                MotionEvent.ACTION_UP -> {
                    this.parent.requestDisallowInterceptTouchEvent(false)
                    firstEventX = null
                    isScrolling = false
                    playerController.setPosition(event.x / v.width)
                    v.performClick()
                    true
                }
                else -> false
            }
        } else
            false
    }

    private fun calculateDistanceX(event: MotionEvent): Int{
        return if (firstEventX == null){
            firstEventX = event.x
            0
        } else {
            (event.x - firstEventX!!).toInt()
        }
    }
}
