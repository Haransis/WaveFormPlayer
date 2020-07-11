package fr.haran.soundwave

interface PlayerController {
    fun preparePlayer()
    fun isPlaying(): Boolean
    fun setPosition(position: Float)
    fun play()
    fun pause()
    fun toggle()
    fun destroyPlayer()
    fun setPlayerListener(playerListener: PlayerListener): PlayerController
}