package fr.haran.soundwave.controller

interface RecorderListener {
    fun onComplete(recorderController: RecorderController)
    fun onStart(recorderController: RecorderController)
}