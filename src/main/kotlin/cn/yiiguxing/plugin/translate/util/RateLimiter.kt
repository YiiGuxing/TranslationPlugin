package cn.yiiguxing.plugin.translate.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A rate limiter that limits the rate of executing coroutines.
 */
interface RateLimiter {

    /**
     * Acquires permission to proceed. If the rate limit has been reached,
     * this function will suspend until permission is granted.
     */
    suspend fun acquire()

    /**
     * Attempts to acquire permission to proceed within the specified [timeout].
     * If the rate limit has been reached, this function will suspend until either
     * permission is granted or the timeout expires.
     *
     * @param timeout The maximum time to wait for permission.
     * @return `true` if permission was granted within the timeout, `false` otherwise.
     */
    suspend fun tryAcquire(timeout: Duration): Boolean

    companion object {
        private val NONE = NoneRateLimiter()

        /**
         * Creates a [RateLimiter] that allows one call per specified [interval].
         * If [interval] is `0`, a no-op rate limiter is returned.
         *
         * @throws IllegalArgumentException if [interval] is less than 0 milliseconds.
         */
        fun withInterval(interval: Duration): RateLimiter {
            val intervalInMilliseconds = interval.inWholeMilliseconds
            return if (intervalInMilliseconds <= 0L) NONE else RateLimiterImpl(intervalInMilliseconds)
        }

        /**
         * Creates a [RateLimiter] that allows the specified number of [calls] per given [perDuration].
         * For example, `withRate(5, 1.seconds)` allows 5 calls per second.
         *
         * @throws IllegalArgumentException if [calls] is less than or equal to 0,
         * or if [perDuration] is less than or equal to 0 milliseconds.
         */
        @Suppress("unused")
        fun withRate(calls: Int, perDuration: Duration = 1.seconds): RateLimiter {
            require(calls > 0) { "calls must be greater than 0" }

            val durationInMilliseconds = perDuration.inWholeMilliseconds
            require(durationInMilliseconds > 0) { "perDuration must be greater than 0 milliseconds" }

            val interval = durationInMilliseconds / calls
            return withInterval(interval.milliseconds)
        }
    }
}

private class RateLimiterImpl(private val interval: Long) : RateLimiter {

    private var lastCallTime = 0L
    private val mutex = Mutex()

    init {
        require(interval > 0) { "interval must be greater than 0" }
    }

    override suspend fun acquire() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastCallTime
            if (elapsed < interval) {
                delay(interval - elapsed)
            }
            lastCallTime = System.currentTimeMillis()
        }
    }

    override suspend fun tryAcquire(timeout: Duration): Boolean {
        return withTimeoutOrNull(timeout) {
            acquire()
            true
        } ?: false
    }
}

private class NoneRateLimiter : RateLimiter {
    override suspend fun acquire() = Unit
    override suspend fun tryAcquire(timeout: Duration) = true
}