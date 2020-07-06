package fr.biophonie.soundwave

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import java.io.IOException

private const val TAG = "PlayerController"
class PlayerController {
    private val mediaPlayer = MediaPlayer()
    private val handler = Handler()
    private lateinit var runnable: Runnable

    private lateinit var playerListener: PlayerListener

    fun preparePlayer(){
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener{ playerListener.onPrepared(this) }
        /*runnable = Runnable {
            onDurationListener.onDurationProgress(
                this@DefaultSoundViewPlayer,
                getDuration(),
                durationCounter.addAndGet(INTERVAL)
            )
            if (mediaPlayer.isPlaying()) {
                handler.postDelayed(this, INTERVAL)
            }
        }*/
    }

    fun setPlayerListener(playerListener: PlayerListener): PlayerController{
        this.playerListener = playerListener
        return this
    }

    @Throws(IOException::class)
    fun setAudioSource(context: Context, uri: Uri) {
        mediaPlayer.setDataSource(context, uri)

        preparePlayer()
    }

    /*@Throws(IOException::class)
    fun setAudioSource(url: String?)*/

    private fun play() {
        mediaPlayer.start()
        playerListener.onPlay(this)
    }

    private fun pause() {
        mediaPlayer.pause()
        playerListener.onPause(this)
    }

    fun toggle() {
        if(mediaPlayer.isPlaying){
            pause()
        } else {
            play()
        }
    }
    /*fun stop()
    fun isPlaying(): Boolean
    fun getDuration(): Long*/
}