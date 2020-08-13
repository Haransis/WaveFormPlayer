package fr.haran.soundwave.controller

import android.icu.text.SimpleDateFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.net.Uri
import android.os.Handler
import android.os.Process.*
import android.os.SystemClock
import android.util.Log
import fr.haran.soundwave.ui.RecPlayerView
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.ForkJoinPool
import kotlin.properties.Delegates

private const val TAG = "DefaultRecorderController"
private const val RECORDER_SAMPLERATE = 44100
private const val RECORDER_CHANNELS: Int = android.media.AudioFormat.CHANNEL_IN_MONO
private const val RECORDER_AUDIO_ENCODING: Int = android.media.AudioFormat.ENCODING_PCM_16BIT
class DefaultRecorderController(var recPlayerView: RecPlayerView, var defaultPath: String, var retriever: InformationRetriever? = null) : RecorderController {

    private var delta = 0
    private var recordedBefore = false
    private var isRecording = false
    private var recorder: AudioRecord? = null
    private var recordingThread = Thread { writeAudioDataToFile() }
    private var bufferSize by Delegates.notNull<Int>()
    val amplitudes = mutableListOf(0)
    private val handler = Handler()
    private lateinit var pcmPath: String
    lateinit var wavPath: String
    private lateinit var recorderListener: RecorderListener
    private var playerController: DefaultPlayerController = DefaultPlayerController(recPlayerView).apply {
        setPlayerListener()
    }
    private var runnable = object: Runnable {
        override fun run() {
            Log.d(TAG, "run: $delta")
            val currentTime = SystemClock.uptimeMillis()
            recPlayerView.addAmplitude(amplitudes.last())
            if (isRecording)
                handler.postAtTime(this, currentTime+recPlayerView.interval)
        }
    }

    override fun toggle() {
        if (isRecording)
            stopRecording(false)
        else {
            if (recordedBefore) {
                amplitudes.clear()
                amplitudes += 0
                recPlayerView.resetAmplitudes()
                deleteExpiredRecordings()
            }
            startRecording()
        }
    }

    private fun deleteExpiredRecordings() {
        ForkJoinPool.commonPool().submit {
            val pcmFile = File(pcmPath)
            if (pcmFile.exists())
                pcmFile.canonicalFile.delete()
            val wavFile = File(wavPath)
            if (wavFile.exists())
                pcmFile.canonicalFile.delete()
        }
    }

    override fun isRecording(): Boolean {
        return isRecording
    }

    override fun prepareRecorder() {
        bufferSize = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING
        )
        recPlayerView.attachRecorderController(this)
    }

    override fun destroyController() {
        destroyRecorder()
        playerController.destroyPlayer()
    }

    private fun destroyRecorder() {
        if (isRecording)
            stopRecording(true)
        else
            stopRecording(false)
        handler.removeCallbacks(runnable)
    }

    override fun setRecorderListener(recorderListener: RecorderListener): RecorderController {
        this.recorderListener = recorderListener
        return this
    }

    override fun validate() {
        recorderListener.onValidate(this)
    }

    override fun startRecording() {
        val date = Date()
        val dateFormat = SimpleDateFormat("ddMMyyyy-ssmmhh", Locale.getDefault())
        pcmPath = "$defaultPath/${dateFormat.format(date)}.pcm"
        wavPath = "$defaultPath/${dateFormat.format(date)}.wav"
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, bufferSize
        )
        setAudioEffects()
        recorder!!.startRecording()
        isRecording = true
        recordingThread.start()
        recordedBefore = true
        recorderListener.onStart(this)
    }

    private fun setAudioEffects() {
        if (AutomaticGainControl.isAvailable()) {
            val agc = AutomaticGainControl.create(recorder!!.audioSessionId)
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
        setThreadPriority(THREAD_PRIORITY_AUDIO)
        // Write the output audio in byte
        var start = SystemClock.elapsedRealtime()
        var now: Long
        val bData = ByteArray(bufferSize)
        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(pcmPath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        handler.post { recPlayerView.startCountDown() }
        handler.post(runnable)
        while (isRecording) {
            recorder!!.read(bData, 0, bufferSize)
            now = SystemClock.elapsedRealtime()
            if (now-start > recPlayerView.interval){
                val sData = ShortArray(bData.size / 2) {
                    (bData[it * 2] + (bData[(it * 2) + 1].toInt() shl 8)).toShort()
                }
                val iData = sData.map { it.toInt() }
                val newAmplitude = iData.maxBy { kotlin.math.abs(it) } ?: 0
                delta = amplitudes.last() - newAmplitude
                amplitudes += newAmplitude
                start = now
            }
            try {
                // writes the data to file from buffer
                os?.write(bData)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        try {
            os?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        rawToWave(File(pcmPath), File(wavPath))
        playerController.addAudioFileUri(recPlayerView.context, Uri.parse("file://$wavPath"))
        handler.post { recPlayerView.onRecordComplete() }
    }

    override fun stopRecording(delete: Boolean) {
        Log.d(TAG, "stopRecording: $delete")
        if (delete)
            deleteExpiredRecordings()

        if (isRecording)
            recorderListener.onComplete(this)

        //recordingThread.interrupt()

        recorder?.let {
            isRecording = false
            it.stop()
            it.release()
            recorder = null
        }
        handler.removeCallbacks(runnable)
    }

    @Throws(IOException::class)
    private fun rawToWave(rawFile: File, waveFile: File) {
        val rawData = ByteArray(rawFile.length().toInt())
        var input: DataInputStream? = null
        try {
            input = DataInputStream(FileInputStream(rawFile))
            input.read(rawData)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        finally {
            input?.close()
        }
        var output: DataOutputStream? = null
        try {
            output = DataOutputStream(FileOutputStream(waveFile))
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF") // chunk id
            writeInt(output, 36 + rawData.size) // chunk size
            writeString(output, "WAVE") // format
            writeString(output, "fmt ") // subchunk 1 id
            writeInt(output, 16) // subchunk 1 size
            writeShort(output, 1.toShort()) // audio format (1 = PCM)
            writeShort(output, 1.toShort()) // number of channels
            writeInt(output, 44100) // sample rate
            writeInt(output, RECORDER_SAMPLERATE * 2) // byte rate
            writeShort(output, 2.toShort()) // block align
            writeShort(output, 16.toShort()) // bits per sample
            writeString(output, "data") // subchunk 2 id
            writeInt(output, rawData.size) // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            val shorts = ShortArray(rawData.size / 2)
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
            val bytes: ByteBuffer = ByteBuffer.allocate(shorts.size * 2)
            for (s in shorts) {
                bytes.putShort(s)
            }
            output.write(fullyReadFileToBytes(rawFile))
        } finally {
            output?.close()
        }
    }

    @Throws(IOException::class)
    fun fullyReadFileToBytes(f: File): ByteArray {
        val size = f.length().toInt()
        val bytes = ByteArray(size)
        val tmpBuff = ByteArray(size)
        try {
            val fis = FileInputStream(f)
            try {
                var read: Int = fis.read(bytes, 0, size)
                if (read < size) {
                    var remain = size - read
                    while (remain > 0) {
                        read = fis.read(tmpBuff, 0, remain)
                        System.arraycopy(tmpBuff, 0, bytes, size - remain, read)
                        remain -= read
                    }
                }
            } catch (e: IOException) {
                throw e
            } finally {
                fis.close()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return bytes
    }

    @Throws(IOException::class)
    private fun writeInt(output: DataOutputStream, value: Int) {
        output.write(value shr 0)
        output.write(value shr 8)
        output.write(value shr 16)
        output.write(value shr 24)
    }

    @Throws(IOException::class)
    private fun writeShort(output: DataOutputStream, value: Short) {
        output.write(value.toInt() shr 0)
        output.write(value.toInt() shr 8)
    }

    @Throws(IOException::class)
    private fun writeString(output: DataOutputStream, value: String) {
        output.write(value.toByteArray())
    }

    interface InformationRetriever{
        fun setPath(path: String)
        fun setAmplitudes(amplitudes: List<Int>)
    }

    inline fun setRecorderListener(
        crossinline start: () -> Unit = {},
        crossinline complete: () -> Unit = {},
        crossinline validate: () -> Unit = {}
    ){
        setRecorderListener(object: RecorderListener {

            override fun onComplete(recorderController: RecorderController) {
                recPlayerView.addLoader()
                retriever?.setPath(wavPath)
                retriever?.setAmplitudes(amplitudes)
                complete()
            }

            override fun onStart(recorderController: RecorderController) {
                recPlayerView.onStart()
                start()
            }

            override fun onValidate(recorderController: RecorderController) {
                validate()
            }
        })
    }
}