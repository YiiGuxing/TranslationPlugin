package cn.yiiguxing.plugin.translate.tts.sound.source

import com.intellij.openapi.progress.ProcessCanceledException

/**
 * The default [PushablePlaybackSource] implementation.
 */
class DefaultPlaybackSource(
    private val loader: PlaybackLoader
) : PushablePlaybackSource() {

    override fun onPrepare() {
        val loader = loader
        try {
            loader.start()
            loader.checkCanceled()
            while (loader.hasNext()) {
                val data = loader.loadNext()
                loader.checkCanceled()
                push(data)
            }
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            throw ProcessCanceledException(e)
        }
    }

    override fun close() {
        super.close()
        loader.cancel()
    }
}