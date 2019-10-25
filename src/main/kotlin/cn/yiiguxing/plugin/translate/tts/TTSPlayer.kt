package cn.yiiguxing.plugin.translate.tts

import com.intellij.openapi.Disposable

/**
 * TTS player.
 */
interface TTSPlayer {

    val disposable: Disposable
    val isPlaying: Boolean

    fun start()
    fun stop()
}