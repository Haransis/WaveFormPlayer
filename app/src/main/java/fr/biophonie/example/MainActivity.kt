package fr.biophonie.example

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import fr.biophonie.soundwave.PlayerView
import java.io.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val view = findViewById<PlayerView>(R.id.player_view)
        val uri = Uri.parse("android.resource://$packageName/raw/france")
        try {
            view.addAudioFileUri(uri)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        //view.nonPlayedColorView = ContextCompat.getColor(this@MainActivity, R.color.colorAccent)
    }
}