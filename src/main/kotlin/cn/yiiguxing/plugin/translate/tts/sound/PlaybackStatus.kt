package cn.yiiguxing.plugin.translate.tts.sound

/**
 * Enumeration describing the different status values of an [AudioPlayer].
 * ```
 * IDLE -> PREPARING
 * PREPARING -> PLAYING
 * PREPARING -> STOPPED
 * PREPARING -> ERROR
 * PLAYING -> PREPARING
 * PLAYING -> STOPPED
 * PLAYING -> ERROR
 * ```
 */
enum class PlaybackStatus {
    /** The player is idle. */
    IDLE,

    /** The player is preparing to play. */
    PREPARING,

    /** The player is playing. */
    PLAYING,

    /** The player has stopped playing. */
    STOPPED,

    /** An error occurred in the player. */
    ERROR
}

/**
 * Whether the playback state is a completed state.
 */
val PlaybackStatus.isCompletedState: Boolean
    get() = this == PlaybackStatus.STOPPED || this == PlaybackStatus.ERROR