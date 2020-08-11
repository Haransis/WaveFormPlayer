package fr.haran.soundwave.ui

import fr.haran.soundwave.controller.PlayerController

interface PlayingView {
    fun attachPlayerController(playerController: PlayerController)
    fun updatePlayerPercent(duration: Int, currentPosition: Int)
    fun <T>setText(title: T)
    fun setAmplitudes(amplitudes: Array<Double>)
    fun onPlay()
    fun onPause()
    fun onComplete()
}