package fr.haran.soundwave.controller

interface RecorderListener {
    fun onStart(recorderController: RecorderController)
    fun onStop(recorderController: RecorderController)
    fun onComplete(recorderController: RecorderController)
    fun onValidate(recorderController: RecorderController)
}