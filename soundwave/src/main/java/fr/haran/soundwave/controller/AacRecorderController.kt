package fr.haran.soundwave.controller

import android.media.*
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import fr.haran.soundwave.ui.RecPlayerView
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.experimental.or
import kotlin.properties.Delegates

private const val TAG = "DefaultRecorderController"
private const val RECORDER_SAMPLERATE = 44100
private const val RECORDER_SAMPLERATE_INDEX = 4
private const val RECORDER_BITRATE = 128000
private const val RECORDER_CHANNELS: Int = 1
private const val RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
class AacRecorderController(var recPlayerView: RecPlayerView, var defaultPath: String, var retriever: InformationRetriever? = null) : RecorderController {

    private var delta = 0
    private var start = 0L
    private var recordedBefore = false
    private var isRecording = false
    private var recorder: AudioRecord? = null
    private var codec: MediaCodec? = null
    private var bufferSize by Delegates.notNull<Int>()
    val amplitudes = mutableListOf(0)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var aacPath: String
    private lateinit var recorderListener: RecorderListener
    private var playerController: DefaultPlayerController = DefaultPlayerController(recPlayerView).apply {
        setPlayerListener()
    }

    private var periodicCallback = object: Runnable {
        override fun run() {
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
        val aacFile = File(aacPath)
        if (aacFile.exists())
            aacFile.canonicalFile.delete()
    }

    override fun isRecording(): Boolean {
        return isRecording
    }

    override fun prepareRecorder() {
        bufferSize = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE,
            AudioFormat.CHANNEL_IN_MONO, RECORDER_AUDIO_ENCODING
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
        handler.removeCallbacks(periodicCallback)
    }

    override fun setRecorderListener(recorderListener: RecorderListener): RecorderController {
        this.recorderListener = recorderListener
        return this
    }

    override fun validate() {
        playerController.pause()
        recorderListener.onValidate(this)
    }

    override fun startRecording() {
        val date = DateTimeFormatter
            .ofPattern("ddMMyyyy-ssmmhh")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.of("UTC"))
            .format(Instant.now())
        aacPath = "$defaultPath/$date.aac"

        try {
            codec = createMediaCodec(bufferSize)
            recorder = createAudioRecord(bufferSize)
        } catch (ex: SecurityException) {
            throw ex
        }
        codec!!.start()
        recorder!!.startRecording()
        isRecording = true

        CoroutineScope(Dispatchers.IO).launch {
            writeAudioDataToFile()
        }
        recordedBefore = true
        recorderListener.onStart(this)
    }

    private suspend fun writeAudioDataToFile() {
        withContext(Dispatchers.IO) {
            start = SystemClock.elapsedRealtime()
            val bufferInfo = MediaCodec.BufferInfo()
            var os: FileOutputStream? = null
            try {
                os = FileOutputStream(aacPath)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            handler.post { recPlayerView.startCountDown() }
            handler.post(periodicCallback)
            while (isRecording) {
                val success = handleCodecInput(recorder!!, codec!!)
                if (success) {
                    try {
                        handleCodecOutput(codec!!, bufferInfo, os!!)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            try {
                os?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            handler.post { recPlayerView.onRecordComplete() }
            playerController.addAudioFileUri(recPlayerView.context, Uri.parse("file://$aacPath"))
        }
    }

    override fun stopRecording(delete: Boolean) {
        if (delete)
            deleteExpiredRecordings()

        if (isRecording)
            recorderListener.onComplete(this)

        recorder?.let {
            isRecording = false
            it.stop()
            it.release()
            recorder = null
        }
        handler.removeCallbacks(periodicCallback)
    }

    @Throws(IOException::class)
    private fun handleCodecInput(
        audioRecord: AudioRecord,
        mediaCodec: MediaCodec
    ): Boolean {
        val audioRecordData = ByteArray(bufferSize)
        val length = audioRecord.read(audioRecordData, 0, audioRecordData.size)
        if (length == AudioRecord.ERROR_BAD_VALUE || length == AudioRecord.ERROR_INVALID_OPERATION || length != bufferSize) {
            if (length != bufferSize) {
                return false
            }
        }

        val codecInputBufferIndex = mediaCodec.dequeueInputBuffer((10 * 1000).toLong())
        if (codecInputBufferIndex >= 0) {
            val inputBuffer = mediaCodec.getInputBuffer(codecInputBufferIndex)
            inputBuffer?.clear()
            inputBuffer?.put(audioRecordData)
            mediaCodec.queueInputBuffer(
                codecInputBufferIndex,
                0,
                length,
                0,
                0
            )
        }

        val now = SystemClock.elapsedRealtime()
        if (now-start > recPlayerView.interval){
            val sData = ShortArray(audioRecordData.size / 2) {
                (audioRecordData[it * 2] + (audioRecordData[(it * 2) + 1].toInt() shl 8)).toShort()
            }
            val iData = sData.map { it.toInt() }
            val newAmplitude = iData.maxByOrNull { kotlin.math.abs(it) } ?: 0
            delta = amplitudes.last() - newAmplitude
            amplitudes += newAmplitude
            start = now
        }
        return true
    }

    @Throws(IOException::class)
    private fun handleCodecOutput(
        mediaCodec: MediaCodec,
        bufferInfo: MediaCodec.BufferInfo,
        outputStream: FileOutputStream
    ) {
        var bufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,0)
        while (bufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (bufferIndex >= 0) {
                mediaCodec.getOutputBuffer(bufferIndex)?.let {
                    it.position(bufferInfo.offset)
                    it.limit(bufferInfo.offset + bufferInfo.size)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        val header = createAdtsHeader(bufferInfo.size - bufferInfo.offset)
                        outputStream.write(header)
                        val data = ByteArray(it.remaining())
                        it[data]
                        outputStream.write(data)
                    }
                    it.clear()
                    mediaCodec.releaseOutputBuffer(bufferIndex, false)
                }
            }
            bufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
        }
    }

    private fun createAdtsHeader(length: Int): ByteArray {
        val frameLength = length + 7
        val adtsHeader = ByteArray(7)
        adtsHeader[0] = 0xFF.toByte() // Sync Word
        adtsHeader[1] = 0xF1.toByte() // MPEG-4, Layer (0), No CRC
        adtsHeader[2] = (MediaCodecInfo.CodecProfileLevel.AACObjectLC - 1 shl 6).toByte()
        adtsHeader[2] = adtsHeader[2] or (RECORDER_SAMPLERATE_INDEX shl 2).toByte()
        adtsHeader[2] = adtsHeader[2] or (RECORDER_CHANNELS shr 2).toByte()
        adtsHeader[3] = (RECORDER_CHANNELS and 3 shl 6 or (frameLength shr 11 and 0x03)).toByte()
        adtsHeader[4] = (frameLength shr 3 and 0xFF).toByte()
        adtsHeader[5] = (frameLength and 0x07 shl 5 or 0x1f).toByte()
        adtsHeader[6] = 0xFC.toByte()
        return adtsHeader
    }

    @Throws(IOException::class)
    private fun createMediaCodec(bufferSize: Int): MediaCodec {
        val mediaFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", RECORDER_SAMPLERATE, RECORDER_CHANNELS) .apply {
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
            setInteger(MediaFormat.KEY_BIT_RATE, RECORDER_BITRATE)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        }
        val codecName = MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(mediaFormat)
        return MediaCodec.createByCodecName(codecName)
            .apply { configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE) }
    }

    private fun createAudioRecord(bufferSize: Int): AudioRecord {
        val audioRecord: AudioRecord?
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE,
                AudioFormat.CHANNEL_IN_MONO,
                RECORDER_AUDIO_ENCODING,
                bufferSize * 10
            )
        } catch (ex: SecurityException) {
            throw ex
        }
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Timber.e("createAudioRecord: Unable to initialize AudioRecord")
            throw RuntimeException("Unable to initialize AudioRecord")
        }
        if (NoiseSuppressor.isAvailable())
            NoiseSuppressor.create(audioRecord.audioSessionId)?.let { it.enabled = true }
        if (AutomaticGainControl.isAvailable())
            AutomaticGainControl.create(audioRecord.audioSessionId)?.let { it.enabled = true }
        return audioRecord
    }

    fun restoreStateOnNewRecView(){
        playerController.controllingView = recPlayerView
        playerController.attachPlayerController()
        recPlayerView.setAmplitudes(amplitudes.toTypedArray())
        recPlayerView.onRecordComplete()
    }

    fun getFileLocation(): String? = if (::aacPath.isInitialized) aacPath else null

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
                retriever?.setPath(getFileLocation() ?: "")
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
