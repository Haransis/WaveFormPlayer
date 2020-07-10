package fr.biophonie.soundwave

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.FontRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.getFontOrThrow
import androidx.core.content.res.getStringOrThrow
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.IOException


private const val TAG = "PlayerView"
open class PlayerView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs){

    var playerController: DefaultPlayerController = DefaultPlayerController()
    private var isScrolling: Boolean = false
    private lateinit var soundWaveView: SoundWaveView
    private lateinit var play: FloatingActionButton
    private lateinit var pause: ImageButton
    private lateinit var timer: TextView
    private lateinit var title: TextView

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PlayerView,
            0, 0).apply {
            try {
                mainColor = getColor(R.styleable.PlayerView_mainColor, ContextCompat.getColor(context, R.color.colorPrimary))
                secondaryColor = getColor(R.styleable.PlayerView_secondaryColor, ContextCompat.getColor(context, R.color.colorPrimaryDark))
                font = getString(R.styleable.PlayerView_fontName)
                text = getString(R.styleable.PlayerView_title)
            } finally {
                recycle()
            }
        }
        playerController.setListener(
            play = {
                //fab.setImageResource(R.drawable.ic_pause)
                play.visibility = View.GONE
                pause.visibility = View.VISIBLE
            },
            pause = {
                //fab.setImageResource(R.drawable.ic_play)
                play.visibility = View.VISIBLE
                pause.visibility = View.GONE
            },
            complete = {
                play.setImageResource(R.drawable.ic_reload)
                timer.text = Utils.millisToString(0)
                play.visibility = View.VISIBLE
                pause.visibility = View.GONE
            },
            durationProgress = {duration,currentTimeStamp ->
                if (!isScrolling)
                    soundWaveView.updatePlayerPercent(currentTimeStamp / duration.toFloat())
                timer.text = Utils.millisToString(duration - currentTimeStamp)
            }
        )
        initView(context)
    }

    @SuppressLint("InflateParams") // This is the correct way to do it
    private fun initView(context: Context) {
        val view: View = LayoutInflater.from(context).inflate(R.layout.player_view, null)
        val font: Typeface = Typeface.createFromAsset(context.assets, font)
        soundWaveView = view.findViewById<SoundWaveView>(R.id.sound_wave_view).apply{
            playedColor = secondaryColor
            nonPlayedColor = mainColor
            setOnTouchListener { v, event ->
                //event.setLocation(event.rawX, event.rawY)
                //tracker.addMovement(event)
                if (playerController.isPlaying()){
                    when(event.actionMasked){
                        MotionEvent.ACTION_DOWN -> {
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            isScrolling = true
                            soundWaveView.updatePlayerPercent(event.x / v.width)
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            playerController.setPosition(event.x / v.width)
                            v.performClick()
                            isScrolling = false
                            true
                        }
                        else -> false
                    }
                }
                else{
                    false
                }
            }
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
        timer = view.findViewById<TextView>(R.id.duration).apply{
            typeface = font
            setTextColor(ColorStateList.valueOf(mainColor))
        }
        title = view.findViewById<TextView>(R.id.sound_title).apply {
            typeface = Typeface.create(font, Typeface.BOLD)
            text = this@PlayerView.text
            setTextColor(ColorStateList.valueOf(mainColor))
        }
        this.addView(view)
    }

    private fun setSoundWaveColor(){
        if (this::soundWaveView.isInitialized){
            soundWaveView.playedColor = mainColor
            soundWaveView.nonPlayedColor = secondaryColor
        }
    }

    @Throws(IOException::class)
    fun addAudioFileUri(uri: Uri, amplitudes: Array<Double>){
        playerController.setAudioSource(context, uri)
        soundWaveView.amplitudes = amplitudes
    }

    @Throws(IOException::class)
    fun addAudioUrl(url: String, amplitudes: Array<Double>){
        playerController.setAudioSource(url)
        soundWaveView.amplitudes = amplitudes
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

    var font: String?
        set(value){
            field = value
            invalidate()
        }

    var text: String?
        set(value){
            field = value
            invalidate()
        }
}
