package fr.haran.soundwave.controller

import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import fr.haran.soundwave.ui.RecPlayerView
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.properties.Delegates

private const val TAG = "DefaultRecorderControll"
private const val RECORDER_SAMPLERATE = 44100
private const val RECORDER_CHANNELS: Int = android.media.AudioFormat.CHANNEL_IN_MONO
private const val RECORDER_AUDIO_ENCODING: Int = android.media.AudioFormat.ENCODING_PCM_16BIT
private const val BYTES_PER_ELEMENT = 2 // 2 bytes in 16bit format
class DefaultRecorderController(var recPlayerView: RecPlayerView, var defaultPath: String) : RecorderController {

    private var recordedBefore = false
    private var isRecording = false
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var bufferSize by Delegates.notNull<Int>()
    val amplitudes = mutableListOf(0)
    private lateinit var runnable: Runnable
    private val handler = Handler()
    private lateinit var filePath: String
    private lateinit var recorderListener: RecorderListener

    override fun toggle() {
        if (isRecording)
            stopRecording()
        else {
            if (recordedBefore) {
                deleteOldRecording()
            }
            startRecording()
        }
    }

    private fun deleteOldRecording() {
        amplitudes.clear()
        amplitudes += 0
        recPlayerView.resetAmplitudes()
        val fileToDelete = File(filePath)
        if (fileToDelete.exists())
            fileToDelete.canonicalFile.delete()
    }

    override fun isRecording(): Boolean {
        return isRecording
    }

    override fun prepareRecorder() {
        recPlayerView.attachController(this)
        runnable = object: Runnable {
            override fun run() {
                val currentTime = SystemClock.uptimeMillis()
                val sData = ShortArray(bufferSize)
                recorder!!.read(sData, 0, bufferSize)
                val iData = sData.map { it.toInt() }
                val newAmplitude = iData.maxBy { kotlin.math.abs(it) } ?: 0
                recPlayerView.addAmplitude(amplitudes.last() - newAmplitude)
                amplitudes += newAmplitude
                if (isRecording)
                    handler.postAtTime(this, currentTime+recPlayerView.interval)
            }
        }
        bufferSize = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING
        )
    }

    override fun destroyRecorder() {
        stopRecording()
        handler.removeCallbacks(runnable)
    }

    override fun setRecorderListener(recorderListener: RecorderListener): RecorderController {
        this.recorderListener = recorderListener
        return this
    }

    override fun startRecording() {
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, bufferSize * BYTES_PER_ELEMENT
        )
        setAudioEffects()
        recorder!!.startRecording()
        isRecording = true
        recordingThread = Thread(Runnable { writeAudioDataToFile() }, "AudioRecorder Thread")
        recordingThread!!.start()
        handler.post(runnable)
        recordedBefore = true
        recorderListener.onStart(this)
    }

    private fun setAudioEffects() {
        if (AutomaticGainControl.isAvailable()) {
            val agc = AutomaticGainControl.create(recorder!!.audioSessionId)
            //agc.g
            Log.d("AudioRecord", "AGC is " + if (agc.enabled) "enabled" else "disabled")
            agc.enabled = true
            Log.d(
                "AudioRecord",
                "AGC is " + if (agc.enabled) "enabled" else "disabled" + " after trying to enable"
            )
        } else {
            Log.d("AudioRecord", "AGC is unavailable")
        }
        if (NoiseSuppressor.isAvailable()) {
            val ns = NoiseSuppressor.create(recorder!!.audioSessionId)
            Log.d("AudioRecord", "NS is " + if (ns.enabled) "enabled" else "disabled")
            ns.enabled = true
            Log.d(
                "AudioRecord",
                "NS is " + if (ns.enabled) "enabled" else "disabled" + " after trying to disable"
            )
        } else {
            Log.d("AudioRecord", "NS is unavailable")
        }
        if (AcousticEchoCanceler.isAvailable()) {
            val aec = AcousticEchoCanceler.create(recorder!!.audioSessionId)
            Log.d("AudioRecord", "AEC is " + if (aec.enabled) "enabled" else "disabled")
            aec.enabled = true
            Log.d(
                "AudioRecord",
                "AEC is " + if (aec.enabled) "enabled" else "disabled" + " after trying to disable"
            )
        } else {
            Log.d("AudioRecord", "aec is unavailable")
        }
    }

    private fun writeAudioDataToFile() {
        // Write the output audio in byte
        filePath = "$defaultPath/test.pcm"
        val sData = ByteArray(bufferSize * BYTES_PER_ELEMENT)
        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(filePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        while (isRecording) {
            recorder!!.read(sData, 0, bufferSize * BYTES_PER_ELEMENT)
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

    override fun stopRecording() {
        recorder?.let {
            isRecording = false
            it.stop()
            it.release()
            recorder = null
            recordingThread = null
        }
        handler.removeCallbacks(runnable)
        recorderListener.onComplete(this)
    }

    inline fun setListener(
        crossinline play: () -> Unit = {},
        crossinline complete: () -> Unit = {}
    ){
        setRecorderListener(object: RecorderListener {

            override fun onComplete(recorderController: RecorderController) {
                recPlayerView.onComplete()
                complete()
            }

            override fun onStart(recorderController: RecorderController) {
                recPlayerView.onStart()
                play()
            }
        })
    }
}