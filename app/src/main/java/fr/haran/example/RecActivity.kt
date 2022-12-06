package fr.haran.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.haran.soundwave.controller.AacRecorderController
import java.io.File

private const val TAG = "RecActivity"
class RecActivity : AppCompatActivity(), AacRecorderController.InformationRetriever {

    private var recorderController: AacRecorderController? = null
    private lateinit var soundPath: String
    private lateinit var soundAmplitudes: List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rec)
        recorderController = applicationContext.externalCacheDir?.absolutePath?.let { it ->
            AacRecorderController(findViewById(R.id.rec_player_view),
                "/storage/emulated/0/Music", this
            )
        }
        recorderController?.setRecorderListener(
            start = { window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) },
            complete = { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)},
            validate = {
                Toast.makeText(
                    this@RecActivity,
                    "Sound Recorded !",
                    Toast.LENGTH_SHORT
                ).show()}
        )
        recorderController?.prepareRecorder()
    }

    override fun onDestroy() {
        super.onDestroy()
        recorderController?.let {
            val soundFile = it.getFileLocation()?.let { path -> File(path) }
            if (soundFile?.exists() == true)
                soundFile.canonicalFile.delete()
            it.destroyController()
            recorderController = null
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun setPath(path: String) {
        soundPath = path
    }

    override fun setAmplitudes(amplitudes: List<Int>) {
        soundAmplitudes = amplitudes
    }
}