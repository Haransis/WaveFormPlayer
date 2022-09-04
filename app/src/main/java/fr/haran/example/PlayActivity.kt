package fr.haran.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.haran.soundwave.controller.DefaultPlayerController
import fr.haran.soundwave.ui.PlayerView
import java.io.IOException


class PlayActivity : AppCompatActivity() {
    private val amplitudes = mutableListOf<Float>()
    private lateinit var playerController: DefaultPlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerController = DefaultPlayerController(playerView).apply {
            setPlayerListener(play = {
                Toast.makeText(
                    this@PlayActivity,
                    "Play Clicked !",
                    Toast.LENGTH_SHORT
                ).show()})
        }
        readAmplitudes()
        val uri = Uri.parse("android.resource://$packageName/raw/france")
        try {
            playerController.addAudioFileUri(applicationContext, uri, amplitudes)
            //view.addAudioUrl(url,amplitudes)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun readAmplitudes() {
        this.assets.open("amplitudesDB.csv").bufferedReader().useLines { lines ->
            lines.forEach { amplitudes.add(it.toFloat()) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerController.destroyPlayer()
    }
}