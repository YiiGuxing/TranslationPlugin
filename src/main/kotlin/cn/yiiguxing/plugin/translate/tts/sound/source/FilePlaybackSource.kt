@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.tts.sound.source

import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * A [PlaybackSource] for file playback.
 */
open class FilePlaybackSource(val file: File) : PlaybackSourceWithContext() {

    override fun getAudioInputStreamWithContext(): AudioInputStream {
        return AudioSystem.getAudioInputStream(file)
    }

}