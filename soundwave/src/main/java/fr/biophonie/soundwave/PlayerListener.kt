package fr.biophonie.soundwave

interface PlayerListener {
    fun onPrepared(playerController: DefaultPlayerController?)
    fun onComplete(playerController: DefaultPlayerController?)
    fun onDurationProgress(playerController: DefaultPlayerController?, duration: Int, currentTimeStamp: Long)
    fun onPause(playerController: DefaultPlayerController?)
    fun onPlay(playerController: DefaultPlayerController?)
}