package fr.biophonie.soundwave

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.util.Log
import java.io.IOException

private const val TAG = "PlayerController"
class PlayerController {
    private val mediaPlayer = MediaPlayer()
    private val handler = Handler()
    private lateinit var runnable: Runnable

    private lateinit var onPreparedListener: PlayerOnPreparedListener
    private lateinit var onPlayListener: PlayerOnPlayListener

    fun preparePlayer(){
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener{onPreparedListener.onPrepared(this)}
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

    /*fun setOnPrepariedListener(onPrepariedListener: SoundViewPlayerOnPreparedListener?): PlayerController?
    fun setOnPauseListener(onPauseListener: SoundViewPlayerOnPauseListener?): PlayerController?
    fun setOnDurationListener(onDurationListener: SoundViewPlayerOnDurationListener?): PlayerController?
    fun setOnCompleteListener(onCompleteListener: SoundViewPlayerOnCompleteListener?): PlayerController?*/

    fun setListener(listener: Listener): PlayerController{

    }

    /*fun setOnPlayListener(onPlayListener: PlayerOnPlayListener): PlayerController{
        this.onPlayListener = onPlayListener
        return this
    }

    fun setOnPreparedListener(onPreparedListener: PlayerOnPreparedListener): PlayerController{
        this.onPreparedListener = onPreparedListener
        return this
    }
*/
    @Throws(IOException::class)
    fun setAudioSource(context: Context, uri: Uri) {
        mediaPlayer.setDataSource(context, uri)

        preparePlayer()
    }

    /*@Throws(IOException::class)
    fun setAudioSource(url: String?)*/

    private fun play() {
        mediaPlayer.start()
        onPlayListener.onPlay(this)
    }

    private fun pause() {
        mediaPlayer.pause()
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