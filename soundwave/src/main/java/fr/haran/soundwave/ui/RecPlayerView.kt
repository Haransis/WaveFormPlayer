package fr.haran.soundwave.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.icu.text.SimpleDateFormat
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.haran.soundwave.R
import fr.haran.soundwave.controller.PlayerController
import fr.haran.soundwave.controller.RecorderController
import java.util.*


private const val TAG = "RecPlayerView"
private const val PERMISSION_CODE = 0
private const val PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
open class RecPlayerView (context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs),
ControllingView{

    lateinit var countDown: CountDownTimer
    private lateinit var timerTv: TextView
    private lateinit var recView: RecView
    private lateinit var recordFab: FloatingActionButton
    private lateinit var playFab: FloatingActionButton
    private lateinit var recordAgainFab: FloatingActionButton
    private lateinit var controlButtons: Group
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
        }
        recordFab = view.findViewById<FloatingActionButton>(R.id.record).apply{
            imageTintList = ColorStateList.valueOf(recordColor)
            foregroundTintList = ColorStateList.valueOf(recordColor)
            setOnClickListener {
                if (alreadyRecorded)
                    recorder.validate()
                else if (checkPermission(context))
                    recorder.toggle()
            }
        }
        controlButtons = view.findViewById(R.id.control_buttons)
        recordAgainFab = view.findViewById<FloatingActionButton>(R.id.record_again).apply {
            imageTintList = ColorStateList.valueOf(recordColor)
            foregroundTintList = ColorStateList.valueOf(recordColor)
            setOnClickListener {
                recorder.toggle()
                alreadyRecorded = false
            }
        }
        playFab = view.findViewById<FloatingActionButton>(R.id.play).apply {
            imageTintList = ColorStateList.valueOf(recordColor)
            foregroundTintList = ColorStateList.valueOf(recordColor)
            setOnClickListener { player.toggle() }
        }
        timerTv = view.findViewById<TextView>(R.id.timer).apply {
            text = SimpleDateFormat("mm:ss", Locale.FRENCH).format(Date(duration.toLong()))
            setTextColor(recordColor)
        }
        setRecViewColor()
        setRecViewSamples()
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
        recView.updateProgression(currentPosition / duration.toFloat())
    }

    override fun <T> setText(title: T) {
    }

    override fun setAmplitudes(amplitudes: Array<Double>) {
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
    }

    fun addAmplitude(dy: Int) {
        recView.addAmplitude(dy)
    }

    fun onRecordComplete() {
        countDown.cancel()
        recordFab.setImageResource(R.drawable.ic_check)
        controlButtons.visibility = View.VISIBLE
        alreadyRecorded = true
    }

    fun onStart() {
        val mTimeFormat = SimpleDateFormat("mm:ss", Locale.FRENCH)
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
        recordFab.setImageResource(R.drawable.ic_stop)
        controlButtons.visibility = View.GONE
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
}