package cn.yiiguxing.plugin.translate.openapi.data

/**
 * Represents asynchronously obtainable data that can be reused
 * while it remains valid.
 *
 * Implementations are responsible for determining how the data is
 * obtained, cached, and invalidated.
 *
 * @param T the type of data.
 */
interface AsyncData<T> {

    /**
     * Returns a usable value.
     *
     * If no usable value is currently available, this method loads
     * a new value.
     *
     * Concurrent callers may share the same in-progress load operation.
     *
     * @return a usable value.
     * @throws Exception if loading the value fails.
     */
    suspend fun get(): T

    /**
     * Returns the currently usable cached value without triggering
     * a load operation.
     *
     * A stale value may be returned if the implementation supports
     * stale-while-revalidate semantics.
     *
     * @return the currently usable value, or `null` if no usable
     * value is available.
     */
    suspend fun getIfValid(): T?

    /**
     * Forces the value to be loaded again.
     *
     * If a load operation for the current cache generation is already
     * in progress, the existing operation may be shared instead of
     * starting another load.
     *
     * @return the newly loaded value.
     * @throws Exception if loading the value fails.
     */
    suspend fun refresh(): T

    /**
     * Invalidates the currently cached value.
     *
     * An already running load operation is not canceled. However, if
     * the invalidation changes the cache generation before that load
     * completes, its result will not be stored in the cache.
     */
    suspend fun invalidate()

    /**
     * Returns whether a usable value is currently available without
     * performing a new load operation.
     *
     * @return `true` if a usable value is available, otherwise `false`.
     */
    suspend fun isValid(): Boolean
}
