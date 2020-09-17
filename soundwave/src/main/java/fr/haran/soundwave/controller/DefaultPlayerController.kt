package fr.haran.soundwave.controller

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import fr.haran.soundwave.ui.ControllingView
import java.io.IOException

private const val TAG = "PlayerController"
private const val INTERVAL: Long = 90
class DefaultPlayerController(var controllingView: ControllingView):
    PlayerController {

    private val mediaPlayer = MediaPlayer()
    private val handler = Handler()
    private var isPrepared = false
    private var runnable: Runnable = object: Runnable {
        override fun run() {
            controllingView.updatePlayerPercent(
                mediaPlayer.duration,
                mediaPlayer.currentPosition
            )
            playerListener.onDurationProgress(
                this@DefaultPlayerController,
                mediaPlayer.duration,
                mediaPlayer.currentPosition
            )

            if (mediaPlayer.isPlaying)
                handler.postDelayed(this, INTERVAL)
        }
    }
    private lateinit var playerListener: PlayerListener

    override fun preparePlayer(){
        controllingView.attachPlayerController(this)
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener{ isPrepared = true }
        mediaPlayer.setOnCompletionListener {
            playerListener.onComplete(this)
            handler.removeCallbacks(runnable)
        }
    }

    fun attachPlayerController(){
        controllingView.attachPlayerController(this)
    }

    override fun isPlaying(): Boolean{
        return mediaPlayer.isPlaying
    }

    override fun setPosition(position: Float){
        mediaPlayer.seekTo((position*mediaPlayer.duration).toInt())
    }

    override fun setPlayerListener(playerListener: PlayerListener): DefaultPlayerController {
        this.playerListener = playerListener
        return this
    }

    override fun play() {
        if (isPrepared) {
            mediaPlayer.start()
            playerListener.onPlay(this)
            handler.post(runnable)
        } else {
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener{
                isPrepared = true
                play()
            }
        }
    }

    override fun <T>setTitle(title: T){
        controllingView.setText(title)
    }

    override fun pause() {
        if (mediaPlayer.isPlaying)
            mediaPlayer.pause()
        playerListener.onPause(this)
    }

    override fun toggle() {
        if (mediaPlayer.isPlaying) pause()
        else play()
    }

    override fun destroyPlayer() {
        resetMediaPlayer()
        mediaPlayer.release()
        handler.removeCallbacks(runnable)
    }

    private fun resetMediaPlayer() {
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
        mediaPlayer.reset()
    }

    @Throws(IOException::class)
    fun addAudioFileUri(context: Context, uri: Uri){
        resetMediaPlayer()
        mediaPlayer.setDataSource(context, uri)
        preparePlayer()
    }

    @Throws(IOException::class)
    fun addAudioFileUri(context: Context, uri: Uri, amplitudes: Array<Double>){
        resetMediaPlayer()
        mediaPlayer.setDataSource(context, uri)
        preparePlayer()
        controllingView.setAmplitudes(amplitudes)
    }

    @Throws(IOException::class)
    fun addAudioUrl(url: String, amplitudes: Array<Double>){
        resetMediaPlayer()
        mediaPlayer.setDataSource(url)
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
