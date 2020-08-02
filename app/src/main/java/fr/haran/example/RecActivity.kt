package fr.haran.example

import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.experimental.and

private const val RECORDER_SAMPLERATE = 44100
private const val RECORDER_CHANNELS: Int = android.media.AudioFormat.CHANNEL_IN_MONO
private const val RECORDER_AUDIO_ENCODING: Int = android.media.AudioFormat.ENCODING_PCM_16BIT
private const val BUFFER_ELEMENT_TO_REC = 1024 // want to play 2048 (2K) since 2 bytes we use only 1024
private const val BYTES_PER_ELEMENT = 2 // 2 bytes in 16bit format


class RecActivity : AppCompatActivity() {

    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rec)

        setOnClickListeners()
        enableButtons(false)

        val bufferSize = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING
        )
    }

    private fun setOnClickListeners() {
        (findViewById<Button>(R.id.start)).setOnClickListener{
            enableButtons(true)
            startRecording()
        }
        (findViewById<Button>(R.id.stop)).setOnClickListener{
            enableButtons(false)
            stopRecording()
        }
    }

    private fun enableButton(id: Int, isEnable: Boolean) {
        (findViewById<View>(id) as Button).isEnabled = isEnable
    }

    private fun enableButtons(isRecording: Boolean) {
        enableButton(R.id.start, !isRecording)
        enableButton(R.id.stop, isRecording)
    }

    private fun startRecording() {
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, BUFFER_ELEMENT_TO_REC * BYTES_PER_ELEMENT
        )
        recorder!!.startRecording()
        isRecording = true
        recordingThread = Thread(Runnable { writeAudioDataToFile() }, "AudioRecorder Thread")
        recordingThread!!.start()
    }

    private fun writeAudioDataToFile() {
        // Write the output audio in byte
        val filePath = Environment.getExternalStorageDirectory().path + "/voice8K16bitmono.pcm"
        val sData = ShortArray(BUFFER_ELEMENT_TO_REC)
        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(filePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder!!.read(sData, 0, BUFFER_ELEMENT_TO_REC)
            println("Short wirting to file$sData")
            try {
                // writes the data to file from buffer
                // stores the voice buffer
                val bData: ByteArray? = shortToByte(sData)
                if (os != null && bData != null) {
                    os.write(bData, 0, BUFFER_ELEMENT_TO_REC * BYTES_PER_ELEMENT)
                }
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
        // stops the recording activity
        recorder?.let {
            isRecording = false
            it.stop()
            it.release()
            recorder = null
            recordingThread = null
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }
}