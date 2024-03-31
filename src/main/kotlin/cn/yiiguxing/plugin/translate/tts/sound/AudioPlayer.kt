@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.tts.sound

import cn.yiiguxing.plugin.translate.util.Observable
import cn.yiiguxing.plugin.translate.util.ObservableValue
import cn.yiiguxing.plugin.translate.util.withPluginContextClassLoader
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.HttpRequests
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.function.Supplier
import javax.sound.sampled.*

/**
 * An audio player with multiple sources, supporting audio formats such as
 * '.wav', '.mp3', '.au', '.aiff', etc. The player follows a strategy of
 * playing one and preloading one when loading and playing audio sources.
 *
 * **Note**: The player is disposable, once stopped, it will no longer be
 * available. When preloading an audio source, all its data will be loaded
 * into memory, so the data of a single source should not be too large.
 * If it is too large, please split it into multiple sources.
 */
class AudioPlayer : PlaybackController {

    private val _state: ObservableValue<PlaybackState> = ObservableValue(PlaybackState.IDLE)
    private var currentState: PlaybackState by _state
    private var playbackList: MutableList<PlaybackSource> = ArrayList()
    private val playWorker: PlayWorker = PlayWorker(this)
    private var playingIndex: Int = -1

    private val playing: Boolean get() = currentState == PlaybackState.PLAYING
    private val completed: Boolean get() = currentState.isCompletedState

    @Volatile
    private var errorHandler: ((Throwable) -> Unit)? = null

    override val stateBinding: Observable<PlaybackState> =
        object : ObservableValue.ReadOnlyWrapper<PlaybackState>(_state) {
            override val value: PlaybackState
                get() = synchronized(this@AudioPlayer) { super.value }
        }
    override val isPlaying: Boolean get() = synchronized(this) { playing }
    override val isCompleted: Boolean get() = synchronized(this) { completed }


    /**
     * Add an audio source from the specified [file].
     *
     * @throws IllegalStateException if the player is not in the idle state.
     */
    fun addSource(file: File) {
        checkIdleState { "Cannot add source in $it state" }
        addSource { file.readBytes() }
    }

    /**
     * Add an audio source from the specified [url].
     *
     * @throws IllegalStateException if the player is not in the idle state.
     */
    fun addSource(url: String) {
        checkIdleState { "Cannot add source in $it state" }
        addSource {
            val indicator = EmptyProgressIndicator()
            val listener = Observable.ChangeListener<PlaybackState> { state, _ ->
                if (state == PlaybackState.STOPPED) {
                    indicator.cancel()
                }
            }
            _state.observe(listener)
            try {
                HttpRequests.request(url).readBytes(indicator)
            } finally {
                _state.unobserve(listener)
            }
        }
    }

    /**
     * Add an audio source from the specified [inputStream].
     *
     * @throws IllegalStateException if the player is not in the idle state.
     */
    fun addSource(inputStream: InputStream) {
        checkIdleState { "Cannot add source in $it state" }
        addSource { inputStream.use { it.readBytes() } }
    }

    /**
     * Add an audio source from the specified [dataSource].
     *
     * @throws IllegalStateException if the player is not in the idle state.
     */
    fun addSource(dataSource: Supplier<ByteArray>) {
        synchronized(this) {
            checkIdleState { "Cannot add source in $it state" }
            playbackList.add(PlaybackSource(dataSource))
        }
    }

    private inline fun checkIdleState(message: (actualState: PlaybackState) -> String) {
        val actualState = currentState
        check(actualState == PlaybackState.IDLE) { message(actualState) }
    }

    /**
     * Set the error handler.
     */
    fun setErrorHandler(handler: (Throwable) -> Unit) {
        errorHandler = handler
    }

    private fun onError(error: Throwable) {
        val handler = errorHandler
        val cause = if (error is PlaybackException) error.cause!! else error
        if (handler != null) {
            handler(cause)
        } else {
            thisLogger().error("Error occurred", cause)
        }
    }

    override fun start() {
        synchronized(this) {
            checkIdleState { "Cannot start in $it state" }
            if (playbackList.isNotEmpty()) {
                currentState = PlaybackState.PREPARING
            } else {
                currentState = PlaybackState.STOPPED
                return
            }
        }

        prepare(0)
    }

    override fun stop() {
        stop(PlaybackState.STOPPED, true)
    }

    private fun stop(state: PlaybackState, immediate: Boolean) {
        check(state == PlaybackState.STOPPED || state == PlaybackState.ERROR) {
            "Invalid stop state: $state"
        }

        var needStop = false
        var needRelease = false
        synchronized(this) {
            if (!completed) {
                currentState.let {
                    needStop = immediate && it == PlaybackState.PLAYING
                    needRelease = it == PlaybackState.PREPARING
                }
                currentState = state
            }
        }
        if (needStop) {
            playWorker.stop(true)
        }
        if (needRelease) {
            release()
        }
    }

    private fun release() {
        playWorker.release()
        synchronized(this) { playingIndex = -1 }
        playbackList.forEach(PlaybackSource::close)
    }

    private fun prepare(index: Int): Boolean {
        if (index >= playbackList.size) {
            return false
        }

        AppExecutorUtil.getAppExecutorService().execute {
            if (!isCompleted) {
                playbackList.getOrNull(index)?.let { source ->
                    source.prepare()
                    onPrepared(index)
                }
            }
        }
        return true
    }

    private fun onPrepared(sourceIndex: Int) {
        synchronized(this) {
            val source = playbackList[sourceIndex]
            if (completed) {
                source.close()
                return
            }

            source.isPrepared = true
            if (currentState != PlaybackState.PREPARING) {
                return
            }
            currentState = PlaybackState.PLAYING
            playingIndex = sourceIndex
        }

        AppExecutorUtil
            .getAppExecutorService()
            .execute(playWorker)
    }

    private fun getPlayingIndex(): Int? {
        return synchronized(this) {
            playingIndex.takeIf { it in playbackList.indices && playing }
        }
    }

    private fun updateNext(sourceIndex: Int): Boolean {
        synchronized(this) {
            if (completed) {
                return false
            }

            check(currentState == PlaybackState.PLAYING) { "Invalid state: $currentState" }
            if (!playbackList[sourceIndex].isPrepared) {
                currentState = PlaybackState.PREPARING
                return false
            }

            playingIndex = sourceIndex
            return true
        }
    }

    private class PlayWorker(private val player: AudioPlayer) : Runnable {
        private var line: SourceDataLine? = null

        override fun run() {
            val player = player
            val buffer: ByteArray by lazy { ByteArray(BUFFER_SIZE) }
            while (true) {
                val index = player.getPlayingIndex() ?: break
                val source = player.playbackList[index]
                var hasPreparing: Boolean
                try {
                    source.error?.let {
                        throw if (it is ProcessCanceledException) it else PlaybackException("Error occurred", it)
                    }
                    hasPreparing = player.prepare(index + 1)
                    source.use { playLine(it.audioInputStream, buffer) }
                } catch (e: Throwable) {
                    val isCanceled = e is ProcessCanceledException
                    val state = if (isCanceled) PlaybackState.STOPPED else PlaybackState.ERROR
                    player.stop(state, false)
                    if (!isCanceled) {
                        player.onError(e)
                    }
                    break
                }
                if (!hasPreparing) {
                    player.stop(PlaybackState.STOPPED, false)
                    break
                }
                if (!player.updateNext(index + 1)) {
                    break
                }
            }

            checkStopped()
        }

        private fun playLine(ais: AudioInputStream, buffer: ByteArray) {
            val line = initLine(ais.format)
            line.start()

            while (player.isPlaying) {
                val read = ais.read(buffer)
                if (read == -1) {
                    break
                }
                line.write(buffer, 0, read)
            }
        }

        private fun initLine(audioFormat: AudioFormat): SourceDataLine {
            val line = line ?: createLine(audioFormat).also { line = it }
            if (line.isOpen && !line.format.matches(audioFormat)) {
                line.drain()
                line.close()
            }
            if (!line.isOpen) {
                line.open(audioFormat)
            }

            return line
        }

        private fun createLine(audioFormat: AudioFormat): SourceDataLine {
            val info = DataLine.Info(SourceDataLine::class.java, audioFormat, AudioSystem.NOT_SPECIFIED)
            return AudioSystem.getLine(info) as SourceDataLine
        }

        private fun checkStopped() {
            if (player.isCompleted) {
                player.release()
            } else if (!player.isPlaying) {
                stop(false)
            }
        }

        fun stop(immediate: Boolean) {
            line?.let {
                if (immediate) {
                    it.flush()
                } else {
                    it.drain()
                }
                it.stop()
            }
        }

        fun release() {
            line?.let {
                it.drain()
                it.stop()
                it.close()
            }
            line = null
        }
    }

    private class PlaybackException(message: String, cause: Throwable) : IllegalStateException(message, cause)

    private class PlaybackSource(private val dataSource: Supplier<ByteArray>) : AutoCloseable {
        @Volatile
        var isPrepared = false

        @Volatile
        private var _audioInputStream: AudioInputStream? = null

        val audioInputStream: AudioInputStream
            get() = _audioInputStream ?: throw error ?: IllegalStateException("Not prepared")

        @Volatile
        var error: Throwable? = null
            private set
            get() {
                check(isPrepared) { "Not prepared" }
                return field
            }

        fun prepare() {
            try {
                load()
            } catch (e: Throwable) {
                error = e
                closeStream()
            }
        }

        private fun load() {
            val data = dataSource.get()
            val bis = ByteArrayInputStream(data)
            _audioInputStream = withPluginContextClassLoader {
                AudioSystem.getAudioInputStream(bis).decode()
            }
        }

        private fun closeStream() {
            try {
                _audioInputStream?.close()
            } finally {
                _audioInputStream = null
            }
        }

        override fun close() {
            closeStream()
        }
    }

    companion object {
        private const val BUFFER_SIZE = 4000 * 4

        private fun AudioInputStream.decode(): AudioInputStream {
            val sourceFormat = format
            var targetSampleSizeInBits = sourceFormat.sampleSizeInBits
            if (targetSampleSizeInBits <= 0) {
                targetSampleSizeInBits = 16
            }
            if ((sourceFormat.encoding === AudioFormat.Encoding.ULAW) || (sourceFormat.encoding === AudioFormat.Encoding.ALAW)) {
                targetSampleSizeInBits = 16
            }
            if (targetSampleSizeInBits != 8) {
                targetSampleSizeInBits = 16
            }
            val targetFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.sampleRate,
                targetSampleSizeInBits,
                sourceFormat.channels,
                sourceFormat.channels * (targetSampleSizeInBits / 8),
                sourceFormat.sampleRate,
                false
            )

            return AudioSystem.getAudioInputStream(targetFormat, this)
        }
    }
}