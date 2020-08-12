package fr.haran.example

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.haran.soundwave.controller.DefaultRecorderController

private const val TAG = "RecActivity"
class RecActivity : AppCompatActivity(), DefaultRecorderController.InformationRetriever {

    private var recorderController: DefaultRecorderController? = null
    private lateinit var soundPath: String
    private lateinit var soundAmplitudes: List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rec)
        recorderController = applicationContext.externalCacheDir?.absolutePath?.let { it ->
            DefaultRecorderController(findViewById(R.id.rec_player_view),
                it, this
            ).apply {
                setRecorderListener(
                    start = {
                        Toast.makeText(
                            this@RecActivity,
                            "Rec Clicked !",
                            Toast.LENGTH_SHORT
                        ).show()}
                    )
            }
        }
        recorderController?.prepareRecorder()
    }

    override fun onDestroy() {
        super.onDestroy()
        recorderController?.destroyController()
        recorderController = null
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