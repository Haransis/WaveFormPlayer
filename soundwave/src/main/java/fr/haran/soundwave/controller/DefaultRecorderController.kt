package fr.haran.soundwave.controller

import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Process.*
import android.os.SystemClock
import android.util.Log
import androidx.core.os.postDelayed
import fr.haran.soundwave.ui.RecPlayerView
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ForkJoinPool
import kotlin.experimental.and
import kotlin.properties.Delegates

private const val TAG = "DefaultRecorderControll"
private const val BUFFER_ELEMENTS_TO_REC = 1024
private const val RECORDER_SAMPLERATE = 44100
private const val RECORDER_CHANNELS: Int = android.media.AudioFormat.CHANNEL_IN_MONO
private const val RECORDER_AUDIO_ENCODING: Int = android.media.AudioFormat.ENCODING_PCM_16BIT
private const val BYTES_PER_ELEMENT = 2 // 2 bytes in 16bit format
class DefaultRecorderController(var recPlayerView: RecPlayerView, var defaultPath: String, var retriever: InformationRetriever? = null) : RecorderController {

    private var delta = 0
    private var recordedBefore = false
    private var isRecording = false
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var bufferSize by Delegates.notNull<Int>()
    val amplitudes = mutableListOf(0)
    private lateinit var runnable: Runnable
    private lateinit var runnableReal: Runnable
    private val handler = Handler()
    private lateinit var pcmPath: String
    lateinit var wavPath: String
    private lateinit var recorderListener: RecorderListener

    override fun toggle() {
        if (isRecording)
            stopRecording(false)
        else {
            if (recordedBefore) {
                amplitudes.clear()
                amplitudes += 0
                recPlayerView.resetAmplitudes()
                deleteOldRecording()
            }
            startRecording()
        }
    }

    private fun deleteOldRecording() {
        ForkJoinPool.commonPool().submit {
            val fileToDelete = File(pcmPath)
            if (fileToDelete.exists())
                fileToDelete.canonicalFile.delete()
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
        val sData = ShortArray(bufferSize)
        /*runnable = object: Runnable {
            override fun run() {
                shouldSample = true
                if (isRecording)
                    handler.postDelayed(this, recPlayerView.interval.toLong())
                *//*setThreadPriority(THREAD_PRIORITY_LOWEST)
                shouldSample = true
                val currentTime = SystemClock.uptimeMillis()
                recorder!!.read(sData, 0, bufferSize)
                val iData = sData.map { it.toInt() }
                val newAmplitude = iData.maxBy { kotlin.math.abs(it) } ?: 0
                recPlayerView.addAmplitude(amplitudes.last() - newAmplitude)
                amplitudes += newAmplitude
                if (isRecording)
                    handler.postAtTime(this, currentTime+recPlayerView.interval)*//*
            }
        }*/
        runnableReal = Runnable { recPlayerView.addAmplitude(delta) }

    }

    override fun destroyRecorder() {
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

    override fun startRecording() {
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, bufferSize
        )
        setAudioEffects()
        recorder!!.startRecording()
        isRecording = true
        recordingThread = Thread { writeAudioDataToFile() }
        recordingThread!!.start()
        //handler.post(runnable)
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
        setThreadPriority(THREAD_PRIORITY_AUDIO)
        // Write the output audio in byte
        //TODO unique filename
        pcmPath = "$defaultPath/test.pcm"
        wavPath = "$defaultPath/test.wav"
        var start = SystemClock.elapsedRealtime()
        var now: Long
        val bData = ByteArray(bufferSize)
        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(pcmPath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
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
                //recPlayerView.addAmplitude(amplitudes.last() - newAmplitude)
                amplitudes += newAmplitude
                start = now
                handler.post(runnableReal)
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
    }

    private fun shortToByte(sData: ShortArray): ByteArray {
        val shortArrsize: Int = sData.size
        val bytes = ByteArray(shortArrsize * 2)
        for (i in 0 until shortArrsize) {
            bytes[i * 2] = (sData[i] and 0x00FF).toByte()
            bytes[i * 2 + 1] = (sData[i].toInt() shr 8).toByte()
            sData[i] = 0
        }
        return bytes
    }

    override fun stopRecording(delete: Boolean) {
        if (delete)
            deleteOldRecording()
        rawToWave(File(pcmPath), File(wavPath))
        if (isRecording)
            recorderListener.onComplete(this)

        recorder?.let {
            isRecording = false
            it.stop()
            it.release()
            recorder = null
            recordingThread = null
        }
        //handler.removeCallbacks(runnable)
    }

    @Throws(IOException::class)
    private fun rawToWave(rawFile: File, waveFile: File) {
        val rawData = ByteArray(rawFile.length().toInt())
        var input: DataInputStream? = null
        try {
            input = DataInputStream(FileInputStream(rawFile))
            input.read(rawData)
        } finally {
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
        crossinline complete: () -> Unit = {}
    ){
        setRecorderListener(object: RecorderListener {

            override fun onComplete(recorderController: RecorderController) {
                recPlayerView.onRecordComplete()
                retriever?.setPath(wavPath)
                retriever?.setAmplitudes(amplitudes)
                complete()
            }

            override fun onStart(recorderController: RecorderController) {
                recPlayerView.onStart()
                start()
            }
        })
    }
}