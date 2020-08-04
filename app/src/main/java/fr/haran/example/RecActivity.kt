package fr.haran.example

import android.Manifest
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.haran.soundwave.ui.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.experimental.and
import kotlin.properties.Delegates

private const val RECORDER_SAMPLERATE = 44100
private const val RECORDER_CHANNELS: Int = android.media.AudioFormat.CHANNEL_IN_MONO
private const val RECORDER_AUDIO_ENCODING: Int = android.media.AudioFormat.ENCODING_PCM_16BIT
private const val BUFFER_ELEMENT_TO_REC = 1024 // want to play 2048 (2K) since 2 bytes we use only 1024
private const val BYTES_PER_ELEMENT = 2 // 2 bytes in 16bit format

private const val PERMISSION_CODE = 0
private const val PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
private const val INTERVAL = 32L
private const val TAG = "RecActivity"
class RecActivity : AppCompatActivity() {

    private lateinit var runnable: Runnable
    private val handler = Handler()

    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private var bufferSize by Delegates.notNull<Int>()

    private lateinit var recView: RecView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rec)
        recView = findViewById(R.id.recview)
        setOnClickListeners()
        runnable = object: Runnable {
            override fun run() {
                val sData = ShortArray(bufferSize)
                recorder!!.read(sData, 0, bufferSize)
                recView.addAmplitude(sData.average().toInt())

                if (isRecording)
                    handler.postDelayed(this, INTERVAL)
            }
        }
        bufferSize = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING
        )
    }

    private fun setOnClickListeners(){
        findViewById<FloatingActionButton>(R.id.rec).apply{
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
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, bufferSize * BYTES_PER_ELEMENT
        )
        recorder!!.startRecording()
        isRecording = true
        recordingThread = Thread(Runnable { writeAudioDataToFile() }, "AudioRecorder Thread")
        recordingThread!!.start()
        handler.post(runnable)
    }

    private fun writeAudioDataToFile() {
        // Write the output audio in byte
        val filePath = applicationContext.externalCacheDir?.absolutePath + "/voice8K16bitmono.pcm"
        val sData = ByteArray(bufferSize)
        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(filePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        while (isRecording) {
            recorder!!.read(sData, 0, bufferSize)
            try {
                // writes the data to file from buffer
                os?.write(sData, 0, bufferSize)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        try {
            os?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun shortToByte(sData: ShortArray): ByteArray? {
        val shortArrsize = sData.size
        val bytes = ByteArray(shortArrsize * 2)
        for (i in 0 until shortArrsize) {
            bytes[i * 2] = (sData[i] and 0x00FF).toByte()
            bytes[i * 2 + 1] = (sData[i].toInt() shr 8).toByte()
            sData[i] = 0
        }
        return bytes
    }

    private fun stopRecording() {
        recorder?.let {
            isRecording = false
            it.stop()
            it.release()
            recorder = null
            recordingThread = null
        }
        handler.removeCallbacks(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }
}