package cn.yiiguxing.plugin.translate.tts.sound

import cn.yiiguxing.plugin.translate.util.Observable

/** Playback controller. */
interface PlaybackController {

    /** The status binding of the playback. */
    val statusBinding: Observable<PlaybackStatus>

    /** The current status of the playback. */
    val status get() = statusBinding.value

    /** Whether the playback is currently playing. */
    val isPlaying: Boolean get() = status == PlaybackStatus.PLAYING

    /** Whether the playback is currently completed. */
    val isCompleted: Boolean get() = status.isCompletedState

    /** Start the playback. */
    fun start()

    /** Stop the playback. */
    fun stop()

}