package fr.biophonie.example

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import fr.biophonie.soundwave.PlayerView
import java.io.IOException

private const val TAG = "MainActivity"
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
    }
}