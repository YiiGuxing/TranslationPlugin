@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.tts.sound.source

import java.io.InputStream
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * A [PlaybackSource] for input stream playback.
 */
class InputStreamPlaybackSource(private val stream: InputStream) : PlaybackSourceWithContext() {

    override fun getAudioInputStreamWithContext(): AudioInputStream {
        return AudioSystem.getAudioInputStream(stream)
    }

    override fun close() {
        stream.close()
    }
}