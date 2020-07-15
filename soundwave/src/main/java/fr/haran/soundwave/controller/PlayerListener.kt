package fr.haran.soundwave.controller

interface PlayerListener {
    fun onComplete(playerController: PlayerController)
    fun onPause(playerController: PlayerController)
    fun onPlay(playerController: PlayerController)
    fun onPrepared(playerController: PlayerController)
    fun onDurationProgress(playerController: PlayerController, duration: Int, currentTimeStamp: Int)
}