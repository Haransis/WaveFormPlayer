package fr.biophonie.soundwave

interface PlayerListener {
    fun onComplete(playerController: DefaultPlayerController?)
    fun onPause(playerController: DefaultPlayerController?)
    fun onPlay(playerController: DefaultPlayerController?)
}