package cn.yiiguxing.plugin.translate.tts.sound.source

import com.intellij.openapi.progress.ProcessCanceledException
import javax.sound.sampled.AudioInputStream

interface PlaybackSource : AutoCloseable {

    /**
     * Prepare the source for playback.
     * @see waitForReady
     */
    fun prepare() {}

    /**
     * Wait for the source to be ready. If [prepare] method is asynchronous,
     * this method should block until the source is ready.
     * @see prepare
     * @throws ProcessCanceledException if the preparation process is canceled.
     */
    fun waitForReady() {}

    /**
     * Called the specified [action] when the source is stalled.
     */
    fun onStalled(action: () -> Unit) {}

    /**
     * Returns the audio input stream.
     */
    fun getAudioInputStream(): AudioInputStream

    /**
     * Close the source.
     */
    override fun close() {}

}