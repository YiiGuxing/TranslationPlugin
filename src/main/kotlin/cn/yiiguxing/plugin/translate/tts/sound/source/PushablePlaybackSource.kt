package cn.yiiguxing.plugin.translate.tts.sound.source

import com.intellij.openapi.progress.ProcessCanceledException
import java.io.InputStream
import java.io.InterruptedIOException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.ReentrantLock
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.concurrent.withLock

/**
 * A [PlaybackSource] that can be pushed with data.
 * Only supports streaming audio data, such as mp3.
 */
abstract class PushablePlaybackSource : PlaybackSourceWithContext() {

    private val lock = ReentrantLock()
    private val readCondition = lock.newCondition()
    private val pushCondition = lock.newCondition()

    private var buffer1 = EMPTY_BUFFER
    private var buffer2 = EMPTY_BUFFER
    private var swap = false
    private val buffer: ByteBuffer get() = if (swap) buffer2 else buffer1
    private var stacked = 0

    private var count = 0
    private var position = 0

    private val inputStream = MyInputStream()
    private val audioStream: AudioInputStream by lazy {
        AudioSystem.getAudioInputStream(inputStream)
    }

    @Volatile
    private var onStalledAction: (() -> Unit)? = null

    private var closed = false
    private var finished = false

    private val hasData: Boolean
        get() = !finished || stacked > 0

    @Volatile
    private var preparationProcessCanceledException: ProcessCanceledException? = null
    private val readySignal = CountDownLatch(1)

    override fun getAudioInputStreamWithContext(): AudioInputStream = audioStream

    override fun onStalled(action: () -> Unit) {
        onStalledAction = action
    }

    /**
     * Notify that the source is ready.
     *
     * @see waitForReady
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected fun ready() {
        readySignal.countDown()
    }

    protected fun setPrepareCanceled(e: ProcessCanceledException) {
        preparationProcessCanceledException = e
    }

    override fun waitForReady() {
        try {
            readySignal.await()
        } catch (e: Exception) {
            // Ignore
        }

        preparationProcessCanceledException?.let {
            preparationProcessCanceledException = null
            throw it
        }
    }

    protected fun push(data: ByteArray) {
        push(ByteBuffer.wrap(data))
    }

    /**
     * Push [data] to the source. [finish] must be called after all data is pushed.
     *
     * Note: This method should be called in a separate thread.
     *
     * @param data the data to push
     * @see finish
     */
    protected fun push(data: ByteBuffer) {
        ready()
        lock.withLock {
            if (!closed && !finished && data.hasRemaining()) {
                stacked++
                try {
                    while (!closed && buffer1 !== EMPTY_BUFFER && buffer2 !== EMPTY_BUFFER) {
                        try {
                            pushCondition.await()
                        } catch (e: InterruptedException) {
                            throw InterruptedIOException()
                        }
                    }
                } finally {
                    stacked--
                }

                if (closed) {
                    return
                }

                setData(data)
                count += data.remaining()
                readCondition.signalAll()
            }
        }
    }

    private fun setData(data: ByteBuffer) {
        if (buffer1 === EMPTY_BUFFER) {
            buffer1 = data
        } else if (buffer2 === EMPTY_BUFFER) {
            buffer2 = data
        } else {
            // Should not happen
            throw IllegalStateException("Buffer is full")
        }
    }

    /**
     * Finish the source.
     *
     * Note: This method should be called in a separate thread.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected fun finish() {
        ready()
        lock.withLock {
            finished = true
            pushCondition.signalAll()
            readCondition.signalAll()
        }
    }

    override fun close() {
        ready()
        lock.withLock {
            closed = true
            pushCondition.signalAll()
            readCondition.signalAll()
        }
    }


    private fun swapBuffer() {
        if (swap) {
            buffer2 = EMPTY_BUFFER
        } else {
            buffer1 = EMPTY_BUFFER
        }
        swap = !swap
        pushCondition.signalAll()
    }

    private inner class MyInputStream : InputStream() {

        override fun read(): Int {
            lock.withLock {
                if (!checkPosition()) {
                    return -1
                }
                if (!buffer.hasRemaining()) {
                    swapBuffer()
                }

                val read = buffer.get().toInt() and 0xFF
                position++

                return read
            }
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            Objects.checkFromIndexSize(off, len, b.size)

            lock.withLock {
                if (!checkPosition()) {
                    return -1
                }

                if (!buffer.hasRemaining()) {
                    swapBuffer()
                }

                val read = minOf(len, buffer.remaining())
                if (read <= 0) {
                    return 0
                }

                buffer.get(b, off, read)
                position += read

                return read
            }
        }

        private fun checkPosition(): Boolean {
            while (position == count) {
                if (closed || !hasData) {
                    return false
                }

                onStalledAction?.invoke()
                try {
                    readCondition.await()
                } catch (e: InterruptedException) {
                    throw InterruptedIOException()
                }
            }

            return !closed
        }

        override fun available(): Int = lock.withLock {
            if (!closed) count - position else 0
        }

        override fun reset() {
        }
    }

    companion object {
        private val EMPTY_BUFFER = ByteBuffer.allocate(0).asReadOnlyBuffer()
    }
}