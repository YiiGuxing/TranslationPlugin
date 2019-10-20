package cn.yiiguxing.plugin.translate.util

import java.util.*


/**
 * LruCache
 *
 * @param [maxSize] for caches that do not override [.sizeOf][LruCache.sizeOf], this is
 * the maximum number of entries in the cache. For all other caches,
 * this is the maximum sum of the sizes of the entries in this cache.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
open class LruCache<K, V>(maxSize: Int) {
    private val map: LinkedHashMap<K, V>

    /**
     * Size of this cache in units. Not necessarily the number of elements.
     *
     * For caches that do not override [.sizeOf][sizeOf], this returns the number
     * of entries in the cache. For all other caches, this returns the sum of
     * the sizes of the entries in this cache.
     */
    var size: Int = 0
        private set
        @Synchronized get
    /**
     * For caches that do not override [.sizeOf][sizeOf], this returns the maximum
     * number of entries in the cache. For all other caches, this returns the
     * maximum sum of the sizes of the entries in this cache.
     */
    var maxSize = maxSize
        private set
        @Synchronized get

    /**
     * The number of times [.put][put] was called.
     */
    var putCount: Int = 0
        private set
        @Synchronized get
    /**
     * The number of times [.create][create] returned a value.
     */
    var createCount: Int = 0
        private set
        @Synchronized get
    /**
     * The number of values that have been evicted.
     */
    var evictionCount: Int = 0
        private set
        @Synchronized get
    /**
     * The number of times [.get][get] returned a value that was already present in the cache.
     */
    var hitCount: Int = 0
        private set
        @Synchronized get
    /**
     * The number of times [.get][get] returned null or required a new value to be created.
     */
    var missCount: Int = 0
        private set
        @Synchronized get

    /**
     * A copy of the current contents of the cache, ordered from least
     * recently accessed to most recently accessed.
     */
    val snapshot: Map<K, V>
        @Synchronized get() = LinkedHashMap(map)

    init {
        require(maxSize > 0) { "maxSize <= 0" }
        this.map = LinkedHashMap(0, 0.75f, true)
    }

    /**
     * Sets the size of the cache.
     *
     * @param maxSize The new maximum size.
     */
    fun resize(maxSize: Int) {
        require(maxSize > 0) { "maxSize <= 0" }

        synchronized(this) {
            this.maxSize = maxSize
        }
        trimToSize(maxSize)
    }

    /**
     * Returns the value for `key` if it exists in the cache or can be
     * created by `#create`. If a value was returned, it is moved to the
     * head of the queue. This returns null if a value is not cached and cannot
     * be created.
     */
    operator fun get(key: K): V? {
        synchronized(this) {
            map[key]?.let {
                hitCount++
                return it
            }
            missCount++
        }

        /*
         * Attempt to create a value. This may take a long time, and the map
         * may be different when create() returns. If a conflicting value was
         * added to the map while create() was working, we leave that value in
         * the map and release the created value.
         */
        val createdValue = create(key) ?: return null
        val mapValue: V? = synchronized(this) {
            createCount++

            val previous = map.put(key, createdValue)
            if (previous != null) {
                // There was a conflict so undo that last put
                map[key] = previous
            } else {
                size += safeSizeOf(key, createdValue)
            }
            previous
        }

        return if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue)
            mapValue
        } else {
            trimToSize(maxSize)
            createdValue
        }
    }

    /**
     * Caches `value` for `key`. The value is moved to the head of
     * the queue.
     *
     * @return the previous value mapped by `key`.
     */
    fun put(key: K, value: V): V? {
        val previous: V? = synchronized(this) {
            putCount++
            size += safeSizeOf(key, value)
            map.put(key, value)?.apply {
                size -= safeSizeOf(key, this@apply)
            }
        }
        previous?.let {
            entryRemoved(false, key, it, value)
        }

        trimToSize(maxSize)
        return previous
    }

    /**
     * Remove the eldest entries until the total of remaining entries is at or
     * below the requested size.
     *
     * @param maxSize the maximum size of the cache before returning. May be -1
     * to evict even 0-sized elements.
     */
    fun trimToSize(maxSize: Int) {
        while (true) {
            val toEvict = synchronized(this) {
                check(!(size < 0 || map.isEmpty() && size != 0)) {
                    javaClass.name + ".sizeOf() is reporting inconsistent results!"
                }

                if (size <= maxSize || map.isEmpty()) {
                    return
                }

                val toEvict = map.entries.iterator().next()
                map.remove(toEvict.key)
                size -= safeSizeOf(toEvict.key, toEvict.value)
                evictionCount++
                toEvict
            }

            entryRemoved(true, toEvict.key, toEvict.value, null)
        }
    }

    /**
     * Removes the entry for `key` if it exists.
     *
     * @return the previous value mapped by `key`.
     */
    fun remove(key: K): V? {
        val previous: V? = synchronized(this) {
            map.remove(key)?.apply {
                size -= safeSizeOf(key, this@apply)
            }
        }

        return previous?.apply {
            entryRemoved(false, key, this@apply, null)
        }
    }

    /**
     * Called for entries that have been evicted or removed. This method is
     * invoked when a value is evicted to make space, removed by a call to
     * [.remove], or replaced by a call to [.put]. The default
     * implementation does nothing.
     *
     * The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * @param evicted  true if the entry is being removed to make space, false
     * if the removal was caused by a [.put] or [.remove].
     * @param newValue the new value for `key`, if it exists. If non-null,
     * this removal was caused by a [.put]. Otherwise it was caused by
     * an eviction or a [.remove].
     */
    protected open fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V?) {}

    /**
     * Called after a cache miss to compute a value for the corresponding key.
     * Returns the computed value or null if no value can be computed. The
     * default implementation returns null.
     *
     * The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * If a value for `key` exists in the cache when this method
     * returns, the created value will be released with [.entryRemoved]
     * and discarded. This can occur when multiple threads request the same key
     * at the same time (causing multiple values to be created), or when one
     * thread calls [.put] while another is creating a value for the same
     * key.
     */
    protected open fun create(key: K): V? {
        return null
    }

    private fun safeSizeOf(key: K, value: V): Int {
        val result = sizeOf(key, value)
        check(result >= 0) { "Negative size: $key=$value" }
        return result
    }

    /**
     * Returns the size of the entry for `key` and `value` in
     * user-defined units.  The default implementation returns 1 so that size
     * is the number of entries and max size is the maximum number of entries.
     *
     * An entry's size must not change while it is in the cache.
     */
    protected open fun sizeOf(key: K, value: V): Int {
        return 1
    }

    /**
     * Clear the cache, calling [.entryRemoved] on each removed entry.
     */
    fun evictAll() {
        trimToSize(-1) // -1 will evict 0-sized elements
    }

    @Synchronized
    override fun toString(): String {
        val accesses = hitCount + missCount
        val hitPercent = if (accesses != 0) 100 * hitCount / accesses else 0
        return "LruCache[maxSize=$maxSize,hits=$hitCount,misses=$missCount,hitRate=$hitPercent%]"
    }
}