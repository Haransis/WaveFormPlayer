package fr.biophonie.soundwave

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "PlayerController"
private const val INTERVAL: Long = 32
open class PlayerController {
    private val mediaPlayer = MediaPlayer()
    private val handler = Handler()
    private lateinit var runnable: Runnable
    private val durationCounter = AtomicLong()

    private lateinit var playerListener: PlayerListener

    private fun preparePlayer(){
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener{ playerListener.onPrepared(this) }
        runnable = object: Runnable {
            override fun run() {
                playerListener.onDurationProgress(
                    this@PlayerController,
                    mediaPlayer.duration,
                    durationCounter.addAndGet(INTERVAL)
                )
                if (mediaPlayer.isPlaying) {
                    handler.postDelayed(this, INTERVAL)
                }
            }
        }

        mediaPlayer.setOnCompletionListener {
            playerListener.onComplete(this)
            durationCounter.set(0)
            handler.removeCallbacks(runnable)
        }
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

    //TODO
    /*@Throws(IOException::class)
    fun setAudioSource(url: String?)*/

    private fun play() {
        mediaPlayer.start()
        playerListener.onPlay(this)

        handler.postDelayed(runnable, INTERVAL)
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
        fun onDurationProgress(playerController: PlayerController?, duration: Int, currentTimeStamp: Long) = Unit
        fun onPause(playerController: PlayerController?) = Unit
        fun onPlay(playerController: PlayerController?) = Unit
    }


    inline fun setListener(
        crossinline prepare: () -> Unit = {},
        crossinline play: () -> Unit = {},
        crossinline pause: () -> Unit = {},
        crossinline complete: () -> Unit = {},
        crossinline durationProgress: (Int, Long) -> Unit = { _, _ -> }
    ){
        setPlayerListener(object: PlayerListener{
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
                duration: Int,
                currentTimeStamp: Long
            ) {
                durationProgress(duration,currentTimeStamp)
            }
        })
    }
    /*fun stop()
    fun isPlaying(): Boolean
    fun getDuration(): Long*/
}
