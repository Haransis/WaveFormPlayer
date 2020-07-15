package fr.haran.soundwave.controller

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.text.SpannableString
import android.text.SpannableStringBuilder
import fr.haran.soundwave.ui.PlayerView
import java.io.IOException

private const val TAG = "PlayerController"
private const val INTERVAL: Long = 90
class DefaultPlayerController(var playerView: PlayerView):
    PlayerController {

    private val mediaPlayer = MediaPlayer()
    private val handler = Handler()
    private var isPrepared = false
    private lateinit var runnable: Runnable
    private lateinit var playerListener: PlayerListener

    override fun preparePlayer(){
        playerView.attachController(this)
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener{ isPrepared = true }
        runnable = object: Runnable {
            override fun run() {
                playerView.updatePlayerPercent(mediaPlayer.duration,
                    mediaPlayer.currentPosition.toLong())
                playerListener.onDurationProgress(this@DefaultPlayerController, mediaPlayer.duration,
                mediaPlayer.currentPosition.toLong())

                if (mediaPlayer.isPlaying) {
                    handler.postDelayed(this,
                        INTERVAL
                    )
                }
            }
        }
        mediaPlayer.setOnCompletionListener {
            playerListener.onComplete(this)
            handler.removeCallbacks(runnable)
        }
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
        playerView.setText(title)
    }

    override fun pause() {
        mediaPlayer.pause()
        playerListener.onPause(this)
    }

    override fun toggle() {
        if(mediaPlayer.isPlaying){
            pause()
        } else {
            play()
        }
    }

    override fun destroyPlayer() {
        mediaPlayer.stop()
        mediaPlayer.release()
        handler.removeCallbacks(runnable)
    }

    @Throws(IOException::class)
    fun addAudioFileUri(context: Context, uri: Uri, amplitudes: Array<Double>){
        mediaPlayer.setDataSource(context, uri)
        preparePlayer()
        playerView.setAmplitudes(amplitudes)
    }

    @Throws(IOException::class)
    fun addAudioUrl(url: String, amplitudes: Array<Double>){
        mediaPlayer.setDataSource(url)
        preparePlayer()
        playerView.setAmplitudes(amplitudes)
    }

    inline fun setListener(
        crossinline prepare: () -> Unit = {},
        crossinline play: () -> Unit = {},
        crossinline pause: () -> Unit = {},
        crossinline complete: () -> Unit = {},
        crossinline progress: (Int, Long) -> Unit = { _, _ -> }
    ){
        setPlayerListener(object: PlayerListener {
            override fun onPrepared(playerController: PlayerController) {
                prepare()
            }

            override fun onPlay(playerController: PlayerController) {
                playerView.onPlay()
                play()
            }

            override fun onPause(playerController: PlayerController) {
                playerView.onPause()
                pause()
            }

            override fun onComplete(playerController: PlayerController) {
                playerView.onComplete()
                complete()
            }

            override fun onDurationProgress(
                playerController: PlayerController,
                duration: Int,
                currentTimeStamp: Long
            ) {
                progress(duration,currentTimeStamp)
            }
        })
    }
}
