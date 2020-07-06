package fr.biophonie.soundwave

interface Listener {
    fun onPrepared(playerController: PlayerController?)
    fun onComplete(playerController: PlayerController?)
    fun onDurationProgress(playerController: PlayerController?, duration: Long, currentTimeStamp: Long)
    fun onPause(playerController: PlayerController?)
    fun onPlay(playerController: PlayerController?)
}

/*
interface PlayerOnPreparedListener {
    fun onPrepared(playerController: PlayerController?)
}

interface PlayerOnCompleteListener {
    fun onComplete(playerController: PlayerController?)
}

interface PlayerOnDurationListener {
    fun onDurationProgress(playerController: PlayerController?, duration: Long, currentTimeStamp: Long)
}

interface PlayerOnPauseListener {
    fun onPause(playerController: PlayerController?)
}

interface PlayerOnPlayListener {
    fun onPlay(playerController: PlayerController?)
}*/
