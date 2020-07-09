package fr.biophonie.soundwave

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import java.io.IOException

private const val TAG = "PlayerController"
private const val INTERVAL: Long = 90
class DefaultPlayerController: PlayerController {

    private val mediaPlayer = MediaPlayer()
    private val handler = Handler()
    private lateinit var runnable: Runnable
    private lateinit var playerListener: PlayerListener

    //TODO(dismiss mediaplayer)
    override fun preparePlayer(){
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener{ playerListener.onPrepared(this) }
        runnable = object: Runnable {
            override fun run() {
                playerListener.onDurationProgress(
                    this@DefaultPlayerController,
                    mediaPlayer.duration,
                    mediaPlayer.currentPosition.toLong()
                )
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

    fun setPosition(position: Float){
        mediaPlayer.seekTo((position*mediaPlayer.duration).toInt())
    }

    override fun setPlayerListener(playerListener: PlayerListener): DefaultPlayerController{
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

    override fun play() {
        mediaPlayer.start()
        playerListener.onPlay(this)

        handler.post(runnable)
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

    inline fun setListener(
        crossinline prepare: () -> Unit = {},
        crossinline play: () -> Unit = {},
        crossinline pause: () -> Unit = {},
        crossinline complete: () -> Unit = {},
        crossinline durationProgress: (Int, Long) -> Unit = { _, Long -> }
    ){
        setPlayerListener(object: PlayerListener{
            override fun onPrepared(playerController: DefaultPlayerController?) {
                prepare()
            }

            override fun onPlay(playerController: DefaultPlayerController?) {
                play()
            }

            override fun onPause(playerController: DefaultPlayerController?) {
                pause()
            }

            override fun onComplete(playerController: DefaultPlayerController?) {
                complete()
            }

            override fun onDurationProgress(
                playerController: DefaultPlayerController?,
                duration: Int,
                currentTimeStamp: Long
            ) {
                durationProgress(duration,currentTimeStamp)
            }
        })
    }
}
