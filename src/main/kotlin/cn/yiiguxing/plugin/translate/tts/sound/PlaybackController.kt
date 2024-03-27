package cn.yiiguxing.plugin.translate.tts.sound

import cn.yiiguxing.plugin.translate.util.Observable

/** Playback controller. */
interface PlaybackController {

    /** The state binding of the playback. */
    val stateBinding: Observable<PlaybackState>

    /** The current state of the playback. */
    val state get() = stateBinding.value

    /** Whether the playback is currently playing. */
    val isPlaying: Boolean get() = state == PlaybackState.PLAYING

    /** Whether the playback is currently completed. */
    val isCompleted: Boolean get() = state.isCompletedState

    /** Start the playback. */
    fun start()

    /** Stop the playback. */
    fun stop()

}