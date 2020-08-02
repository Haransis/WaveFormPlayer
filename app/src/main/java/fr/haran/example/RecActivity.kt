package fr.haran.example

import android.Manifest
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.*

private const val PERMISSION_CODE = 0
private const val PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
class RecActivity : AppCompatActivity() {

    private var recorder: MediaRecorder? = null
    private lateinit var recordFile: File
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rec)
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
        val storageDir = applicationContext.externalCacheDir?.absolutePath
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(Date())
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(storageDir + File.separator + "JPEG_${timeStamp}_"+".3gp")
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            prepare()
        }
        recorder!!.start()
        isRecording = true
    }

    private fun stopRecording() {
        recorder?.stop()
        recorder?.release()
        recorder = null
        isRecording = false
    }
}