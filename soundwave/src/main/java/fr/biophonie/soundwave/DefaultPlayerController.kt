package fr.biophonie.soundwave

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.util.Log
import java.io.IOException

private const val TAG = "PlayerController"
private const val INTERVAL: Long = 90
class DefaultPlayerController(private var mediaPlayer: MediaPlayer, private var playerView: PlayerView): PlayerController {

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

                if (mediaPlayer.isPlaying) {
                    handler.postDelayed(this, INTERVAL)
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

    override fun setPlayerListener(playerListener: PlayerListener): DefaultPlayerController{
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
}
