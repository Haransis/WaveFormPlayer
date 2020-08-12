package fr.haran.soundwave.controller

interface RecorderController {
    fun toggle()
    fun startRecording()
    fun stopRecording(delete: Boolean)
    fun isRecording(): Boolean
    fun prepareRecorder()
    fun destroyController()
    fun setRecorderListener(recorderListener: RecorderListener): RecorderController
}