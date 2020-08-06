package fr.haran.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import fr.haran.soundwave.controller.DefaultRecorderController
import fr.haran.soundwave.ui.RecPlayerView

private const val TAG = "RecActivity"
class RecActivity : AppCompatActivity() {

    private var recorderController: DefaultRecorderController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rec)
        recorderController = applicationContext.externalCacheDir?.absolutePath?.let {
            DefaultRecorderController(findViewById(R.id.rec_player_view),
                it
            ).apply { setListener(play = {
                Toast.makeText(
                    this@RecActivity,
                    "Rec Clicked !",
                    Toast.LENGTH_SHORT
                ).show()}) }
        }
        recorderController?.prepareRecorder()
    }

    override fun onDestroy() {
        super.onDestroy()
        recorderController?.destroyRecorder()
        recorderController = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }
}