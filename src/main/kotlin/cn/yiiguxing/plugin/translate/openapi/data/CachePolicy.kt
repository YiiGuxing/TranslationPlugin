package cn.yiiguxing.plugin.translate.openapi.data

import kotlin.time.Duration

/**
 * Defines how a successfully loaded value is cached.
 */
sealed interface CachePolicy {

    /**
     * The value never expires automatically.
     */
    data object NoExpiration : CachePolicy

    /**
     * The value remains valid for [duration] after it has been loaded.
     *
     * @property duration the validity duration.
     */
    data class ExpireAfter(
        val duration: Duration,
    ) : CachePolicy {

        init {
            require(!duration.isNegative()) {
                "duration must not be negative."
            }
        }
    }

    /**
     * The value is considered fresh for [freshFor].
     *
     * After the fresh period, the value becomes stale but remains
     * usable for an additional [staleFor] period.
     *
     * During the stale period, callers receive the existing value
     * immediately while a background refresh is performed.
     *
     * @property freshFor the duration for which the value is fresh.
     * @property staleFor the additional duration for which the value
     * may be served as stale.
     */
    data class StaleWhileRevalidate(
        val freshFor: Duration,
        val staleFor: Duration,
    ) : CachePolicy {

        init {
            require(!freshFor.isNegative()) {
                "freshFor must not be negative."
            }

            require(!staleFor.isNegative()) {
                "staleFor must not be negative."
            }
        }
    }
}