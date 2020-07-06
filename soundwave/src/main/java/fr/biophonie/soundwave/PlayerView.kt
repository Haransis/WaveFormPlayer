package fr.biophonie.soundwave

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.IOException

private const val TAG = "PlayerView"
class PlayerView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs),
    PlayerOnPlayListener,
    PlayerOnPauseListener,
    PlayerOnPreparedListener{

    private lateinit var soundWaveView: SoundWaveView
    private var playerController: PlayerController = PlayerController()
    private lateinit var fab: FloatingActionButton

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
        playerController.setOnPlayListener(this)
            .setOnPreparedListener(this)
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

    override fun onPlay(playerController: PlayerController?) {
        Log.d(TAG, "onPlay: ")
        fab.setImageResource(R.drawable.ic_pause)
    }

    override fun onPause(playerController: PlayerController?) {
        fab.setImageResource(R.drawable.ic_play)
    }

    override fun onPrepared(playerController: PlayerController?) {

    }

    /*override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): LayoutParams {
        return LayoutParams(p)
    }

    class LayoutParams : ConstraintLayout.LayoutParams{
        constructor(width: Int, height: Int): super(width, height)
        constructor(context: Context?, attrs: AttributeSet?): super(context, attrs)
        constructor(layoutParams: ViewGroup.LayoutParams?): super(layoutParams)
    }*/
}
