package fr.biophonie.soundwave

interface PlayerListener {
    fun onPrepared(playerController: PlayerController?) = Unit
    fun onComplete(playerController: PlayerController?) = Unit
    fun onDurationProgress(playerController: PlayerController?, duration: Long, currentTimeStamp: Long) = Unit
    fun onPause(playerController: PlayerController?) = Unit
    fun onPlay(playerController: PlayerController?) = Unit
}