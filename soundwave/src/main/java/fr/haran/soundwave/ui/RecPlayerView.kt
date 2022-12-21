package fr.haran.soundwave.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.icu.text.SimpleDateFormat
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import fr.haran.soundwave.R
import fr.haran.soundwave.controller.PlayerController
import fr.haran.soundwave.controller.RecorderController
import java.util.*
import kotlin.math.abs


private const val TAG = "RecPlayerView"
private const val PERMISSION_CODE = 0
private const val PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
open class RecPlayerView (context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs),
ControllingView, View.OnTouchListener{

    private val mTouchSlop: Int = android.view.ViewConfiguration.get(context).scaledTouchSlop
    lateinit var countDown: CountDownTimer
    private var firstEventX: Float? = null
    private var isScrolling: Boolean = false
    private lateinit var timerTv: TextView
    private lateinit var recView: RecView
    private lateinit var recordFab: FloatingActionButton
    private lateinit var stopFab: FloatingActionButton
    private lateinit var stopText: TextView
    private lateinit var playFab: FloatingActionButton
    private lateinit var recordAgainFab: FloatingActionButton
    private lateinit var controlButtons: Group
    private lateinit var loader: ImageView
    private lateinit var recorder: RecorderController
    private lateinit var player: PlayerController
    private var alreadyRecorded = false

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.RecPlayerView,
            0, 0).apply {
            try {
                recordColor = getColor(
                    R.styleable.RecPlayerView_rec_color, ContextCompat.getColor(context,
                        R.color.colorPrimaryDark
                    ))
                playColor = getColor(
                    R.styleable.RecPlayerView_rec_playedColor, ContextCompat.getColor(context,
                        R.color.colorPrimary
                    ))
                duration = getInteger(R.styleable.RecPlayerView_rec_duration, 2*60*1000)
                interval = getInteger(R.styleable.RecPlayerView_rec_interval, 150)
            } finally {
                recycle()
            }
        }
        initView(context)
    }

    @SuppressLint("InflateParams")
    private fun initView(context: Context) {
        val view = LayoutInflater.from(context).inflate(R.layout.rec_player_view, this, true)
        recView = view.findViewById<RecView>(
            R.id.recview
        ).apply{
            recordColor = this@RecPlayerView.recordColor
            playColor = this@RecPlayerView.playColor
            setOnTouchListener(this@RecPlayerView)
        }
        recordFab = view.findViewById<FloatingActionButton>(R.id.record).apply{
            imageTintList = ColorStateList.valueOf(recordColor)
            foregroundTintList = ColorStateList.valueOf(recordColor)
            setOnClickListener {
                if (alreadyRecorded) {
                    toggleRecordAgain(false)
                    recorder.validate()
                }
                else if (checkPermission(context)){
                    recorder.toggle()
                }
            }
        }
        stopFab = view.findViewById<FloatingActionButton>(R.id.stop).apply{
            imageTintList = ColorStateList.valueOf(recordColor)
            setOnClickListener {
                recorder.stopRecording(false)
            }
        }
        stopText = view.findViewById(R.id.stop_text)
        controlButtons = view.findViewById(R.id.control_buttons)
        recordAgainFab = view.findViewById<FloatingActionButton>(R.id.record_again).apply {
            imageTintList = ColorStateList.valueOf(recordColor)
            foregroundTintList = ColorStateList.valueOf(recordColor)
            setOnClickListener {
                alreadyRecorded = false
                if (::player.isInitialized) player.destroyPlayer()
                recorder.toggle()
            }
        }
        playFab = view.findViewById<FloatingActionButton>(R.id.play).apply {
            imageTintList = ColorStateList.valueOf(recordColor)
            foregroundTintList = ColorStateList.valueOf(recordColor)
            setOnClickListener {
                if (::player.isInitialized){
                    player.toggle()
                    toggleRecordAgain(player.isPlaying())
                }
            }
        }
        timerTv = view.findViewById<TextView>(R.id.timer).apply {
            text = SimpleDateFormat("mm:ss", Locale.FRENCH).format(Date(duration.toLong()))
            setTextColor(recordColor)
        }
        loader = view.findViewById(R.id.loader)
        setRecViewColor()
        setRecViewSamples()
    }

    private fun toggleRecordAgain(deactivate: Boolean) {
        recordAgainFab.imageTintList = ColorStateList.valueOf(if (deactivate) playColor else recordColor)
        recordAgainFab.foregroundTintList = ColorStateList.valueOf(if (deactivate) playColor else recordColor)
        recordAgainFab.isClickable = !deactivate
    }

    private fun setRecViewColor() {
        if (this::recView.isInitialized){
            recView.recordColor = this.recordColor
            recView.playColor = this.playColor
        }
    }

    private fun checkPermission(context: Context): Boolean{
        return if (ActivityCompat.checkSelfPermission(context, PERMISSION_RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
            true
        } else {
            ActivityCompat.requestPermissions(context as Activity,
                arrayOf(PERMISSION_RECORD_AUDIO),
                PERMISSION_CODE)
            false
        }
    }

    fun attachRecorderController(controller: RecorderController){
        recorder = controller
    }

    override fun attachPlayerController(playerController: PlayerController){
        player = playerController
    }

    override fun updatePlayerPercent(duration: Int, currentPosition: Int) {
        if(!isScrolling)
            recView.updateProgression(currentPosition / duration.toFloat())
    }

    override fun <T> setText(title: T) {
    }

    override fun setAmplitudes(amplitudes: List<Float>) {
    }

    fun setAmplitudes(amplitudes: Array<Int>) {
        recView.drawAmplitudes(amplitudes)
    }

    override fun onPlay() {
        playFab.setImageResource(R.drawable.ic_pause)
        recView.isPlaying = true
        recView.updateProgression(0F)
    }

    override fun onPause() {
        playFab.setImageResource(R.drawable.ic_play)
        recView.isPlaying = false
    }

    override fun onComplete() {
        playFab.setImageResource(R.drawable.ic_play)
        recView.isPlaying = false
        toggleRecordAgain(false)
    }

    override fun onError() {
        onComplete()
    }

    fun addAmplitude(y: Int) {
        recView.addAmplitude(y)
    }

    fun addLoader(){
        recordFab.visibility = View.INVISIBLE
        controlButtons.visibility = View.GONE
        loader.visibility = View.VISIBLE
        val animated = AnimatedVectorDrawableCompat.create(context, R.drawable.loader)
        animated?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                loader.post { animated.start() }
            }
        })
    }

    fun onRecordComplete() {
        if (::countDown.isInitialized)
            countDown.cancel()
        recordFab.setImageResource(R.drawable.ic_check)
        stopFab.visibility = View.GONE
        stopText.visibility = View.GONE
        recordFab.visibility = View.VISIBLE
        controlButtons.visibility = View.VISIBLE
        loader.visibility = View.GONE
        alreadyRecorded = true
    }

    fun onStart() {
        recordFab.visibility = View.INVISIBLE
        controlButtons.visibility = View.GONE
        stopFab.visibility = View.VISIBLE
        stopText.visibility = View.VISIBLE
    }

    fun startCountDown(){
        val mTimeFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        countDown = object : CountDownTimer(duration.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTv.text = mTimeFormat.format(Date(millisUntilFinished))
            }

            override fun onFinish() {
                recView.endLine()
                timerTv.text = mTimeFormat.format(Date(0))
                recorder.stopRecording(false)
            }
        }.start()
    }

    @ColorRes
    var recordColor: Int
        set(value){
            field = value
            setRecViewColor()
        }

    @ColorRes
    var playColor: Int
        set(value){
            field = value
            setRecViewColor()
        }

    var duration: Int
        set(value){
            field = value
            setRecViewSamples()
        }

    var interval: Int
        set(value){
            if (value != 0){
                field = value
                setRecViewSamples()
            }
        }

    private fun setRecViewSamples() {
        if (interval != 0 && this::recView.isInitialized)
            recView.samples = duration / interval
    }

    fun resetAmplitudes() {
        recView.resetAmplitudes()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return if (::player.isInitialized && player.isPlaying()){
            when(event.actionMasked){
                MotionEvent.ACTION_DOWN -> true
                MotionEvent.ACTION_MOVE -> {
                    if (isScrolling) {
                        recView.updateProgression(event.x / recView.getRealWidth())
                        true
                    } else {
                        val xDiff = calculateDistanceX(event)
                        if (abs(xDiff) > mTouchSlop) {
                            this.parent.requestDisallowInterceptTouchEvent(true)
                            isScrolling = true
                            recView.updateProgression(event.x / recView.getRealWidth())
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
                    player.setPosition(event.x / recView.getRealWidth())
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