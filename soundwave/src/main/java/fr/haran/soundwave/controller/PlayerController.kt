package fr.haran.soundwave.controller

interface PlayerController {
    fun preparePlayer()
    fun isPlaying(): Boolean
    fun setPosition(position: Float)
    fun play()
    fun pause()
    fun toggle()
    fun destroyPlayer()
    fun setPlayerListener(playerListener: PlayerListener): PlayerController
    fun <T>setTitle(title: T)
}