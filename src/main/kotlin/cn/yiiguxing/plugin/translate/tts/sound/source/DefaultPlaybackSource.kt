package cn.yiiguxing.plugin.translate.tts.sound.source

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.concurrency.AppExecutorUtil

/**
 * The default [PushablePlaybackSource] implementation.
 */
class DefaultPlaybackSource(
    private val loader: PlaybackLoader
) : PushablePlaybackSource() {

    override fun prepare() {
        AppExecutorUtil.getAppExecutorService().execute {
            try {
                onPrepare()
            } finally {
                finish()
            }
        }
    }

    private fun onPrepare() {
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
            setPrepareCanceled(e)
        } catch (e: Throwable) {
            setPrepareCanceled(ProcessCanceledException(e))
        }
    }

    override fun close() {
        super.close()
        loader.cancel()
    }
}