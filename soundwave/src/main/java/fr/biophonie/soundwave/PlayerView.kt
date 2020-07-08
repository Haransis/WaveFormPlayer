package fr.biophonie.soundwave

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil


private const val TAG = "PlayerView"
class PlayerView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs){

    private var playerController: PlayerController = PlayerController()
    private lateinit var soundWaveView: SoundWaveView
    private lateinit var fab: FloatingActionButton
    private lateinit var timer: TextView

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PlayerView,
            0, 0).apply {

            try {
                mainColor = getColor(R.styleable.PlayerView_mainColor, ContextCompat.getColor(context, R.color.colorPrimary))
                secondaryColor = getColor(R.styleable.PlayerView_secondaryColor, ContextCompat.getColor(context, R.color.colorPrimaryDark))
            } finally {
                recycle()
            }
        }
        playerController.setListener(
            play = {fab.setImageResource(R.drawable.ic_pause)},
            pause = {fab.setImageResource(R.drawable.ic_play)},
            durationProgress = {duration,currentTimeStamp ->
                soundWaveView.updatePlayerPercent(currentTimeStamp / duration.toFloat())
                timer.text = Utils.millisToString(duration - currentTimeStamp)
            }
        )
        initView(context)
    }

    @SuppressLint("InflateParams") // This is the correct way to do it
    private fun initView(context: Context) {
        val view: View = LayoutInflater.from(context).inflate(R.layout.player_view, null)
        soundWaveView = view.findViewById<SoundWaveView>(R.id.sound_wave_view).apply{
            playedColor = secondaryColor
            nonPlayedColor = mainColor
        }
        fab = view.findViewById<FloatingActionButton>(R.id.floatingActionButton).apply{
            backgroundTintList = ColorStateList.valueOf(secondaryColor)
            imageTintList = ColorStateList.valueOf(mainColor)
            foregroundTintList = ColorStateList.valueOf(mainColor)
        }.apply { setOnClickListener { playerController.toggle() } }
        timer = view.findViewById(R.id.duration)
        this.addView(view)
    }

    private fun setSoundWaveColor(){
        if (this::soundWaveView.isInitialized){
            soundWaveView.playedColor = mainColor
            soundWaveView.nonPlayedColor = secondaryColor
        }
    }

    @Throws(IOException::class)
    fun addAudioFileUri(uri: Uri){
        playerController.setAudioSource(context, uri)
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
}
