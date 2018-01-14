package cn.yiiguxing.plugin.translate.tts

import com.intellij.openapi.Disposable

/**
 * TTSPlayer
 *
 * Created by Yii.Guxing on 2017-10-28 0028.
 */
interface TTSPlayer {

    val disposable: Disposable
    val isPlaying: Boolean

    fun start()
    fun stop()
}