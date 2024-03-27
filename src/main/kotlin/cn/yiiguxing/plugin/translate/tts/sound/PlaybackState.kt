package cn.yiiguxing.plugin.translate.tts.sound

/**
 * Playback state.
 */
enum class PlaybackState {
    IDLE, PREPARING, PLAYING, STOPPED, ERROR
}

/**
 * Whether the playback state is a completed state.
 */
val PlaybackState.isCompletedState: Boolean
    get() = this == PlaybackState.STOPPED || this == PlaybackState.ERROR