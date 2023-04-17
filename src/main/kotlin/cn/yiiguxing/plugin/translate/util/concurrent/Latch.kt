@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util.concurrent

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 * A synchronization aid that allows one or more threads to wait
 * until a set of operations being performed in other threads completes.
 */
interface Latch {
    /**
     * Causes the current thread to wait until the latch is completed.
     */
    fun await()

    /**
     * Causes the current thread to wait until the latch is completed.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return `true` if the latch completes and `false` if the waiting time elapsed before the latch completes
     */
    fun await(timeout: Long, unit: TimeUnit): Boolean
}

class AsyncLatch : CountDownLatch(1), Latch {
    fun done() {
        countDown()
    }
}