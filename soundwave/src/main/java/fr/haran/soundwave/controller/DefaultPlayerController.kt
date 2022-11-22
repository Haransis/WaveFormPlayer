package fr.haran.soundwave.controller

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import fr.haran.soundwave.ui.ControllingView
import java.io.IOException

private const val TAG = "PlayerController"
private const val INTERVAL: Long = 90
class DefaultPlayerController(var controllingView: ControllingView):
    PlayerController {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isPrepared = false
    private var runnable: Runnable = object: Runnable {
        override fun run() {
            mediaPlayer?.let {
                controllingView.updatePlayerPercent(
                    it.duration,
                    it.currentPosition
                )
                playerListener.onDurationProgress(
                    this@DefaultPlayerController,
                    it.duration,
                    it.currentPosition
                )

                if (it.isPlaying)
                    handler.postDelayed(this, INTERVAL)
            }
        }
    }
    private lateinit var playerListener: PlayerListener

    override fun preparePlayer(){
        mediaPlayer?.let { player ->
            player.prepareAsync()
            player.setOnPreparedListener{
                isPrepared = true
                controllingView.updatePlayerPercent(it.duration,0)
            }
            player.setOnCompletionListener {
                playerListener.onComplete(this)
                handler.removeCallbacks(runnable)
            }
        }
        controllingView.attachPlayerController(this)
    }

    fun attachPlayerController(){
        controllingView.attachPlayerController(this)
    }

    override fun isPlaying(): Boolean{
        return mediaPlayer?.isPlaying ?: false
    }

    override fun setPosition(position: Float){
        mediaPlayer?.let { it.seekTo((position*it.duration).toInt()) }
    }

    override fun setPlayerListener(playerListener: PlayerListener): DefaultPlayerController {
        this.playerListener = playerListener
        return this
    }

    override fun play() {
        mediaPlayer?.let {
            if (isPrepared) {
                it.start()
                playerListener.onPlay(this)
                handler.post(runnable)
            } else {
                it.prepareAsync()
                it.setOnPreparedListener{
                    isPrepared = true
                    play()
                }
            }
        }
    }

    override fun <T>setTitle(title: T){
        controllingView.setText(title)
    }

    override fun pause() {
        if (mediaPlayer?.isPlaying == true)
            mediaPlayer?.pause()
        playerListener.onPause(this)
    }

    override fun toggle() {
        if (mediaPlayer?.isPlaying == true) pause()
        else play()
    }

    override fun destroyPlayer() {
        resetMediaPlayer()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacks(runnable)
    }

    private fun resetMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) mediaPlayer?.stop()
            mediaPlayer?.reset()
        } else {
            mediaPlayer = MediaPlayer()
        }
    }

    @Throws(IOException::class)
    fun addAudioFileUri(context: Context, uri: Uri){
        resetMediaPlayer()
        mediaPlayer!!.setDataSource(context, uri)
        preparePlayer()
    }

    @Throws(IOException::class)
    fun addAudioFileUri(context: Context, uri: Uri, amplitudes: List<Float>){
        resetMediaPlayer()
        mediaPlayer!!.setDataSource(context, uri)
        preparePlayer()
        controllingView.setAmplitudes(amplitudes)
    }

    @Throws(IOException::class)
    fun addAudioUrl(url: String, amplitudes: List<Float>){
        resetMediaPlayer()
        mediaPlayer!!.setDataSource(url)
        preparePlayer()
        controllingView.setAmplitudes(amplitudes)
    }

    inline fun setPlayerListener(
        crossinline prepare: () -> Unit = {},
        crossinline play: () -> Unit = {},
        crossinline pause: () -> Unit = {},
        crossinline complete: () -> Unit = {},
        crossinline progress: (Int, Int) -> Unit = { _, _ -> }
    ){
        setPlayerListener(object: PlayerListener {
            override fun onPrepared(playerController: PlayerController) {
                prepare()
            }

            override fun onPlay(playerController: PlayerController) {
                controllingView.onPlay()
                play()
            }

            override fun onPause(playerController: PlayerController) {
                controllingView.onPause()
                pause()
            }

            override fun onComplete(playerController: PlayerController) {
                controllingView.onComplete()
                complete()
            }

            override fun onDurationProgress(
                playerController: PlayerController,
                duration: Int,
                currentTimeStamp: Int
            ) {
                progress(duration,currentTimeStamp)
            }
        })
    }
}
