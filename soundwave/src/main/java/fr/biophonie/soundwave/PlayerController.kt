package fr.biophonie.soundwave

interface PlayerController {
    fun preparePlayer()
    fun isPlaying(): Boolean
    fun play()
    fun pause()
    fun toggle()
    fun destroyPlayer()
    fun setPlayerListener(playerListener: PlayerListener): PlayerController
}