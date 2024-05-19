package cn.yiiguxing.plugin.translate.tts.sound.source

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

/**
 * The loader for loading playback data.
 */
abstract class PlaybackLoader {

    /**
     * Indicates whether the loading process is started.
     */
    @Volatile
    var isStarted = false
        private set

    /**
     * Indicates whether the loading process is canceled.
     */
    @Volatile
    var isCancelled = false
        private set

    /**
     * Start the loading process.
     */
    internal fun start() {
        isStarted = true
        onStart()
    }

    /**
     * Called when the loading process is started.
     */
    protected open fun onStart() = Unit

    /**
     * Returns `true` if there is more data to load.
     */
    abstract fun hasNext(): Boolean

    @RequiresBackgroundThread
    internal fun loadNext(): ByteArray {
        checkCanceled()

        return try {
            onLoad()
        } catch (e: Throwable) {
            setError(e)
        }
    }

    /**
     * Loads the playback data.
     */
    @RequiresBackgroundThread
    abstract fun onLoad(): ByteArray

    private fun setError(error: Throwable): Nothing {
        if (error is ProcessCanceledException) {
            isCancelled = true
            throw error
        }
        onError(error)
        throw error
    }

    /**
     * Handle the error.
     */
    protected open fun onError(error: Throwable) {
        thisLogger().error("Failed to load playback data.", error)
    }

    /**
     * Cancel the loading process.
     */
    fun cancel() {
        isCancelled = true
        onCanceled()
    }

    fun checkCanceled() {
        if (isCancelled) {
            throw ProcessCanceledException()
        }
    }

    /**
     * Called when the loading process is canceled.
     */
    protected open fun onCanceled() = Unit


    /**
     * A single source data loader.
     */
    @Suppress("unused")
    abstract class SingleSource<T>(protected val src: T) : PlaybackLoader() {
        private var loaded = false

        final override fun hasNext(): Boolean = !loaded

        final override fun onLoad(): ByteArray {
            loaded = true
            return onLoad(src)
        }

        abstract fun onLoad(src: T): ByteArray
    }

    /**
     * A multi-source data loader.
     */
    abstract class MultiSource<T>(private val srcset: Iterator<T>) : PlaybackLoader() {

        override fun hasNext(): Boolean = srcset.hasNext()

        final override fun onLoad(): ByteArray {
            return onLoad(srcset.next())
        }

        abstract fun onLoad(src: T): ByteArray
    }
}