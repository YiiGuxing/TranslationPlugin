package cn.yiiguxing.plugin.translate.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A rate limiter that limits the rate of executing a block of code.
 */
interface RateLimiter {

    /**
     * Run the given [block] with rate limiting.
     */
    suspend fun <T> run(block: suspend () -> T): T

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
            require(intervalInMilliseconds >= 0) { "interval must be greater than or equal to 0 milliseconds" }
            return if (intervalInMilliseconds == 0L) NONE else RateLimiterImpl(intervalInMilliseconds)
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

    override suspend fun <T> run(block: suspend () -> T): T {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastCallTime
            if (elapsed < interval) {
                delay(interval - elapsed)
            }
            lastCallTime = System.currentTimeMillis()
        }

        return block()
    }
}

private class NoneRateLimiter : RateLimiter {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}