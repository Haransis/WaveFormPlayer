package fr.haran.example

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.haran.soundwave.controller.DefaultPlayerController
import fr.haran.soundwave.controller.DefaultRecorderController
import fr.haran.soundwave.ui.RecPlayerView

private const val TAG = "RecActivity"
class RecActivity : AppCompatActivity(), DefaultRecorderController.InformationRetriever {

    private var recorderController: DefaultRecorderController? = null
    private var playerController: DefaultPlayerController? = null
    private lateinit var soundPath: String
    private lateinit var soundAmplitudes: List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rec)
        playerController = DefaultPlayerController(findViewById<RecPlayerView>(R.id.rec_player_view))
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
                        ).show()},
                    complete = { playerController?.let {
                        it.addAudioFileUri(this@RecActivity,
                            Uri.parse("file://$soundPath"))
                        it.setPlayerListener()
                    }})
            }
        }
        recorderController?.prepareRecorder()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerController?.destroyPlayer()
        playerController = null
        recorderController?.destroyRecorder()
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