package fr.haran.example

import android.Manifest
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.haran.soundwave.ui.RecView
import java.io.File
import java.util.*

private const val PERMISSION_CODE = 0
private const val PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
private const val INTERVAL = 32L
private const val TAG = "RecActivity"
class RecActivity : AppCompatActivity() {

    private lateinit var runnable: Runnable
    private val handler = Handler()

    private var recorder: MediaRecorder? = null
    private var isRecording = false

    private lateinit var recView: RecView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rec)
        recView = findViewById(R.id.recview)
        setOnClickListeners()
    }

    private fun setOnClickListeners(){
        val rec = findViewById<FloatingActionButton>(R.id.rec).apply{
            setOnClickListener {
                if (isRecording){
                    stopRecording()
                    setImageResource(R.drawable.ic_mic)
                }
                else {
                    if (checkPermission()) {
                        startRecording()
                        setImageResource(R.drawable.ic_stop)
                    } else {
                        Toast.makeText(
                            context,
                            "Please grant permission to record",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun checkPermission(): Boolean{
        return if (ActivityCompat.checkSelfPermission(applicationContext, PERMISSION_RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
            true
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(PERMISSION_RECORD_AUDIO),
                PERMISSION_CODE)
            false
        }
    }

    private fun startRecording() {
        prepareRecorder()
        recorder!!.start()
        handler.post(runnable)
        isRecording = true
    }

    private fun prepareRecorder() {
        val storageDir = applicationContext.externalCacheDir?.absolutePath
        //val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(Date())
        val name: String = "recording"
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(storageDir + File.separator + "3GP_${name}_" + ".3gp")
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            prepare()
        }
        runnable = object: Runnable {
            override fun run() {
                recView.addAmplitude(recorder!!.maxAmplitude)
                if (isRecording)
                    handler.postDelayed(this, INTERVAL)
            }
        }
    }

    private fun stopRecording() {
        recorder?.stop()
        recorder?.reset()
        recorder?.release()
        recorder = null
        handler.removeCallbacks(runnable)
        isRecording = false
    }
}