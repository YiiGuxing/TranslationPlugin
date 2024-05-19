@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.tts.sound

import cn.yiiguxing.plugin.translate.tts.sound.source.PlaybackSource
import cn.yiiguxing.plugin.translate.util.Observable
import cn.yiiguxing.plugin.translate.util.ObservableValue
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.concurrency.AppExecutorUtil
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

/**
 * A disposable audio player that will no longer be available once it stops playing.
 */
class AudioPlayer(
    private val playbackSource: PlaybackSource
) : PlaybackController {

    private val _status: ObservableValue<PlaybackStatus> = ObservableValue(PlaybackStatus.IDLE)
    private var currentStatus: PlaybackStatus by _status
    private val playWorker: PlayWorker = PlayWorker()

    @Volatile
    private var errorHandler: ((Throwable) -> Unit)? = null

    private val completed: Boolean get() = currentStatus.isCompletedState

    override val statusBinding: Observable<PlaybackStatus> =
        object : ObservableValue.ReadOnlyWrapper<PlaybackStatus>(_status) {
            override val value: PlaybackStatus
                get() = synchronized(this@AudioPlayer) { super.value }
        }
    override val isPlaying: Boolean get() = synchronized(this) { currentStatus == PlaybackStatus.PLAYING }
    override val isCompleted: Boolean get() = synchronized(this) { completed }

    init {
        playbackSource.onStalled {
            updateStatus(PlaybackStatus.PREPARING)
        }
    }

    /**
     * Set the error handler.
     */
    fun setErrorHandler(handler: (Throwable) -> Unit) {
        errorHandler = handler
    }

    private fun onError(error: Throwable) {
        val handler = errorHandler
        if (handler != null) {
            handler(error)
        } else {
            thisLogger().error(error.message, error)
        }
    }

    override fun start() {
        synchronized(this) {
            val status = currentStatus
            if (status != PlaybackStatus.IDLE) {
                throw PlaybackException("Cannot start from $status state.")
            }
            currentStatus = PlaybackStatus.PREPARING
        }

        AppExecutorUtil.getAppExecutorService().execute(playWorker)
    }

    override fun stop() {
        if (updateStatus(PlaybackStatus.STOPPED)) {
            playbackSource.close()
        }
    }

    private fun updateStatus(status: PlaybackStatus): Boolean {
        synchronized(this) {
            val oldStatus = currentStatus
            if (!oldStatus.isCompletedState) {
                if (oldStatus != status) {
                    currentStatus = status
                }
                return true
            }
        }

        return false
    }


    private inner class PlayWorker : Runnable {
        override fun run() {
            if (isCompleted) {
                return
            }

            try {
                playbackSource.use(::prepareAndPlay)
            } catch (e: Throwable) {
                if (e !is ProcessCanceledException) {
                    updateStatus(PlaybackStatus.ERROR)
                    onError(PlaybackException("Error occurred while playing audio", e))
                    return
                }
            }

            updateStatus(PlaybackStatus.STOPPED)
        }

        private fun prepareAndPlay(source: PlaybackSource) {
            source.prepare()
            source.waitForReady()
            if (isCompleted) {
                return
            }

            source.getAudioInputStream().use { ais ->
                openLine(ais.format).use { line ->
                    line.start()
                    try {
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (!isCompleted) {
                            val read = ais.read(buffer)
                            if (read == -1 || !updateStatus(PlaybackStatus.PLAYING)) {
                                break
                            }
                            line.write(buffer, 0, read)
                        }
                    } finally {
                        line.drain()
                        line.stop()
                    }
                }
            }
        }
    }


    class PlaybackException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

    companion object {
        private const val BUFFER_SIZE = 4000 * 4

        private fun openLine(audioFormat: AudioFormat): SourceDataLine {
            val info = DataLine.Info(SourceDataLine::class.java, audioFormat, AudioSystem.NOT_SPECIFIED)
            return (AudioSystem.getLine(info) as SourceDataLine).apply {
                open(audioFormat)
            }
        }
    }
}