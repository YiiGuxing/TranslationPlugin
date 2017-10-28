package cn.yiiguxing.plugin.translate.tts

/**
 * TTSPlayer
 *
 * Created by Yii.Guxing on 2017-10-28 0028.
 */
interface TTSPlayer {
    fun isPlaying(): Boolean
    fun start()
    fun stop()
}