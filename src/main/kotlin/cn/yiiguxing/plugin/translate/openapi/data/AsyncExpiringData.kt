package cn.yiiguxing.plugin.translate.openapi.data

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

/**
 * Provides coroutine-safe asynchronous loading and expiration management
 * for a single piece of data.
 *
 * Subclasses only need to implement [load] and, when necessary, define
 * the cache policy.
 *
 * Features:
 *
 * - lazy loading;
 * - time-based expiration;
 * - data-dependent cache policies;
 * - single-flight loading;
 * - stale-while-revalidate;
 * - explicit refresh;
 * - explicit invalidation;
 * - generation-based invalidation;
 * - failure isolation;
 * - coroutine-safe concurrent access.
 *
 * A failed load is never cached. A subsequent request will attempt
 * to load the value again.
 *
 * This class does not own the lifecycle of [scope]. The caller is
 * responsible for managing the supplied scope.
 *
 * @param T the type of data managed by this instance.
 * @param scope the coroutine scope used for background refreshes.
 */
abstract class AsyncExpiringData<T>(
    private val scope: CoroutineScope,
) : AsyncData<T> {

    private val mutex = Mutex()

    /**
     * The currently cached entry, if any.
     */
    private var entry: Entry<T>? = null

    /**
     * The currently running load operation, if any.
     */
    private var loading: Loading<T>? = null

    /**
     * The currently running background refresh.
     *
     * This is used only to prevent multiple background refreshes
     * from being launched simultaneously.
     */
    private var backgroundRefreshJob: Job? = null

    /**
     * Changes whenever the cache is invalidated.
     *
     * A load operation may only populate the cache if its generation
     * still matches the current generation.
     */
    private var generation = 0L

    /**
     * Defines the default cache policy.
     *
     * Override this property when all values use the same policy.
     */
    protected open val defaultCachePolicy: CachePolicy
        get() = CachePolicy.NoExpiration

    /**
     * Loads a new value.
     *
     * This method is always invoked outside the internal mutex.
     *
     * @return the newly loaded value.
     */
    protected abstract suspend fun load(): T

    /**
     * Determines the cache policy for a successfully loaded [value].
     *
     * The default implementation returns [defaultCachePolicy].
     *
     * @param value the successfully loaded value.
     * @return the cache policy associated with [value].
     */
    protected open fun cachePolicy(value: T): CachePolicy = defaultCachePolicy

    /**
     * Returns a usable value.
     *
     * A fresh value is returned immediately.
     *
     * A stale value is returned immediately and a background refresh
     * is scheduled.
     *
     * An expired or missing value causes a load operation.
     *
     * Concurrent callers share the same load operation.
     */
    override suspend fun get(): T {
        val action: Action<T> = mutex.withLock {
            val current = entry

            if (current != null) {
                when (current.state()) {
                    EntryState.FRESH -> {
                        return@withLock Action.Return(current.value)
                    }

                    EntryState.STALE -> {
                        return@withLock Action.ReturnStale(
                            value = current.value,
                            generation = generation,
                        )
                    }

                    EntryState.EXPIRED -> {
                        // Continue with loading.
                    }
                }
            }

            loading
                ?.takeIf { it.generation == generation }
                ?.let {
                    return@withLock Action.Await(it.deferred)
                }

            Action.StartLoad(generation = generation)
        }

        return when (action) {
            is Action.Return -> action.value
            is Action.ReturnStale -> {
                startBackgroundRefresh(action.generation)
                action.value
            }

            is Action.Await -> action.deferred.await()
            is Action.StartLoad -> loadOnce(action.generation)
        }
    }

    /**
     * Returns the currently usable cached value without triggering
     * a load operation.
     *
     * A stale value is considered usable.
     *
     * @return the usable cached value, or `null` if none exists.
     */
    override suspend fun getIfValid(): T? {
        return mutex.withLock {
            entry?.takeIf { it.state() != EntryState.EXPIRED }?.value
        }
    }

    /**
     * Forces a load operation.
     *
     * An existing load operation belonging to the current generation
     * is shared.
     *
     * The existing cached value is ignored.
     */
    override suspend fun refresh(): T {
        val action: Action<T> = mutex.withLock {
            loading
                ?.takeIf { it.generation == generation }
                ?.let {
                    return@withLock Action.Await(it.deferred)
                }

            Action.StartLoad(generation = generation)
        }

        return when (action) {
            is Action.Await -> action.deferred.await()
            is Action.StartLoad -> loadOnce(action.generation)
            is Action.Return,
            is Action.ReturnStale -> error("Unexpected action: $action")
        }
    }

    /**
     * Invalidates the current cached value.
     *
     * The current cache generation is incremented so that any load
     * belonging to the previous generation can no longer populate
     * the cache.
     *
     * An already running load operation is not cancelled.
     */
    override suspend fun invalidate() {
        mutex.withLock {
            generation++
            entry = null
        }
    }

    /**
     * Returns whether a usable cached value is available.
     *
     * A stale value is considered usable.
     */
    override suspend fun isValid(): Boolean {
        return mutex.withLock {
            entry?.state() != EntryState.EXPIRED
        }
    }

    /**
     * Executes or joins a single-flight load operation.
     *
     * Only the producer executes [load].
     */
    private suspend fun loadOnce(loadGeneration: Long): T {
        val operation: Loading<T>
        val producer: Boolean

        mutex.withLock {
            /*
             * Another coroutine may have started the load between
             * the initial state inspection and this point.
             */
            val existing = loading
                ?.takeIf {
                    it.generation == loadGeneration
                }

            if (existing != null) {
                operation = existing
                producer = false
            } else {
                operation = Loading(
                    deferred = CompletableDeferred(),
                    generation = loadGeneration,
                )

                loading = operation
                producer = true
            }
        }

        /*
         * A non-producer simply waits for the existing operation.
         */
        if (!producer) {
            return operation.deferred.await()
        }

        return try {
            /*
             * The expensive operation is intentionally performed
             * outside the mutex.
             */
            val value = load()

            val policy = cachePolicy(value)

            mutex.withLock {
                /*
                 * The result can only be cached if no invalidation
                 * occurred while loading.
                 */
                if (generation == loadGeneration) {
                    entry = Entry(
                        value = value,
                        policy = policy,
                        createdAt = TimeSource.Monotonic.markNow(),
                    )
                }

                /*
                 * Do not clear a newer load operation that may have
                 * replaced this one after an invalidation.
                 */
                if (loading === operation) {
                    loading = null
                }
            }

            operation.deferred.complete(value)

            value
        } catch (exception: Throwable) {
            mutex.withLock {
                if (loading === operation) {
                    loading = null
                }
            }

            operation.deferred.completeExceptionally(exception)

            throw exception
        }
    }

    /**
     * Starts a background refresh for a stale entry.
     *
     * The refresh is associated with [refreshGeneration]. If the cache
     * is invalidated before the refresh completes, the result will not
     * be stored.
     */
    private suspend fun startBackgroundRefresh(refreshGeneration: Long) {
        mutex.withLock {
            /*
             * The stale entry may have been invalidated between get()
             * and this method.
             */
            if (generation != refreshGeneration) {
                return
            }

            /*
             * Another foreground or background load is already active.
             */
            if (loading != null) {
                return
            }

            /*
             * A background refresh is already scheduled.
             */
            if (backgroundRefreshJob?.isActive == true) {
                return
            }

            backgroundRefreshJob = scope.launch {
                try {
                    loadOnce(refreshGeneration)
                } catch (exception: CancellationException) {
                    /*
                     * Cancellation is not a load failure and must not
                     * be swallowed.
                     */
                    throw exception
                } catch (_: Throwable) {
                    /*
                     * Background refresh failures are intentionally
                     * ignored. The stale value remains available until
                     * its stale period expires.
                     */
                } finally {
                    mutex.withLock {
                        if (backgroundRefreshJob === coroutineContext[Job]) {
                            backgroundRefreshJob = null
                        }
                    }
                }
            }
        }
    }

    /**
     * Determines the current state of a cached entry.
     */
    private fun Entry<T>.state(): EntryState {
        return when (val policy = policy) {
            CachePolicy.NoExpiration -> EntryState.FRESH

            is CachePolicy.ExpireAfter -> if (!createdAt.plus(policy.duration).hasPassedNow()) {
                EntryState.FRESH
            } else {
                EntryState.EXPIRED
            }

            is CachePolicy.StaleWhileRevalidate -> {
                val freshUntil = createdAt.plus(policy.freshFor)
                val staleUntil = freshUntil.plus(policy.staleFor)
                when {
                    !freshUntil.hasPassedNow() -> EntryState.FRESH
                    !staleUntil.hasPassedNow() -> EntryState.STALE
                    else -> EntryState.EXPIRED
                }
            }
        }
    }

    /**
     * A successfully loaded value and its associated cache policy.
     */
    private data class Entry<T>(
        val value: T,
        val policy: CachePolicy,
        val createdAt: TimeSource.Monotonic.ValueTimeMark,
    )

    /**
     * Represents a load operation currently in progress.
     *
     * The generation identifies the cache generation to which this
     * operation belongs.
     */
    private data class Loading<T>(
        val deferred: CompletableDeferred<T>,
        val generation: Long,
    )

    private enum class EntryState {
        FRESH,
        STALE,
        EXPIRED,
    }

    /**
     * Describes the action that should be performed after inspecting
     * the current cache state.
     */
    private sealed interface Action<T> {

        /**
         * Returns an already cached value.
         */
        data class Return<T>(
            val value: T,
        ) : Action<T>

        /**
         * Returns a stale value and starts a background refresh.
         */
        data class ReturnStale<T>(
            val value: T,
            val generation: Long,
        ) : Action<T>

        /**
         * Waits for an existing load operation.
         */
        data class Await<T>(
            val deferred: CompletableDeferred<T>,
        ) : Action<T>

        /**
         * Starts a new load operation.
         */
        data class StartLoad<T>(
            val generation: Long,
        ) : Action<T>
    }
}

