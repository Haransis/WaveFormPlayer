package fr.haran.soundwave.ui

import fr.haran.soundwave.controller.PlayerController

interface ControllingView {
    fun attachPlayerController(playerController: PlayerController)
    fun updatePlayerPercent(duration: Int, currentPosition: Int)
    fun <T> setText(title: T)
    fun setAmplitudes(amplitudes: List<Float>)
    fun onPlay()
    fun onPause()
    fun onComplete()
    fun onError()
}