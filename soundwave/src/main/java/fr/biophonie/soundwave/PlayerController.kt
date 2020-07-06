package fr.biophonie.soundwave

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import java.io.IOException

private const val TAG = "PlayerController"
open class PlayerController {
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

    interface PlayerListener {
        fun onPrepared(playerController: PlayerController?) = Unit
        fun onComplete(playerController: PlayerController?) = Unit
        fun onDurationProgress(playerController: PlayerController?, duration: Long, currentTimeStamp: Long) = Unit
        fun onPause(playerController: PlayerController?) = Unit
        fun onPlay(playerController: PlayerController?) = Unit
    }


    inline fun setListener(
        crossinline prepare: () -> Unit = {},
        crossinline play: () -> Unit = {},
        crossinline pause: () -> Unit = {},
        crossinline complete: () -> Unit = {},
        crossinline durationProgress: () -> Unit = {}
    ){
        setPlayerListener(object: PlayerController.PlayerListener{
            override fun onPrepared(playerController: PlayerController?) {
                prepare()
            }

            override fun onPlay(playerController: PlayerController?) {
                play()
            }

            override fun onPause(playerController: PlayerController?) {
                pause()
            }

            override fun onComplete(playerController: PlayerController?) {
                complete()
            }

            override fun onDurationProgress(
                playerController: PlayerController?,
                duration: Long,
                currentTimeStamp: Long
            ) {
                durationProgress()
            }
        })
    }
    /*fun stop()
    fun isPlaying(): Boolean
    fun getDuration(): Long*/
}
