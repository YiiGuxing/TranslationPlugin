package cn.yiiguxing.plugin.translate.util

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * RateLimiterTest
 */
class RateLimiterTest {

    @Test
    fun testTryAcquireWithinTimeout() = runBlocking {
        val rateLimiter = RateLimiter.withInterval(100.milliseconds)
        
        // First acquire should succeed immediately
        val result1 = rateLimiter.tryAcquire(1.seconds)
        Assert.assertTrue("First acquire should succeed", result1)
        
        // Second acquire should succeed because we give it enough timeout
        val result2 = rateLimiter.tryAcquire(200.milliseconds)
        Assert.assertTrue("Second acquire with sufficient timeout should succeed", result2)
    }

    @Test
    fun testTryAcquireTimeout() = runBlocking {
        val rateLimiter = RateLimiter.withInterval(500.milliseconds)
        
        // First acquire should succeed immediately
        val result1 = rateLimiter.tryAcquire(1.seconds)
        Assert.assertTrue("First acquire should succeed", result1)
        
        // Second acquire with insufficient timeout should fail
        val result2 = rateLimiter.tryAcquire(100.milliseconds)
        Assert.assertFalse("Second acquire with insufficient timeout should fail", result2)
    }

    @Test
    fun testTryAcquireMultipleTimes() = runBlocking {
        val rateLimiter = RateLimiter.withInterval(50.milliseconds)
        
        // Acquire multiple times with sufficient timeout
        for (i in 1..3) {
            val result = rateLimiter.tryAcquire(200.milliseconds)
            Assert.assertTrue("Acquire #$i should succeed", result)
        }
    }

    @Test
    fun testTryAcquireWithZeroInterval() = runBlocking {
        val rateLimiter = RateLimiter.withInterval(0.milliseconds)
        
        // NoneRateLimiter should always succeed immediately
        val result1 = rateLimiter.tryAcquire(1.milliseconds)
        Assert.assertTrue("First acquire should succeed", result1)
        
        val result2 = rateLimiter.tryAcquire(1.milliseconds)
        Assert.assertTrue("Second acquire should succeed immediately", result2)
        
        val result3 = rateLimiter.tryAcquire(1.milliseconds)
        Assert.assertTrue("Third acquire should succeed immediately", result3)
    }

    @Test
    fun testTryAcquireWithRate() = runBlocking {
        val rateLimiter = RateLimiter.withRate(2, 1.seconds) // 2 calls per second = 500ms interval
        
        // First acquire should succeed
        val result1 = rateLimiter.tryAcquire(1.seconds)
        Assert.assertTrue("First acquire should succeed", result1)
        
        // Second acquire with insufficient timeout should fail
        val result2 = rateLimiter.tryAcquire(100.milliseconds)
        Assert.assertFalse("Second acquire with 100ms timeout should fail (needs ~500ms)", result2)
        
        // Third acquire with sufficient timeout should succeed
        val result3 = rateLimiter.tryAcquire(600.milliseconds)
        Assert.assertTrue("Third acquire with sufficient timeout should succeed", result3)
    }

    @Test
    fun testAcquireStillWorks() = runBlocking {
        val rateLimiter = RateLimiter.withInterval(50.milliseconds)
        
        // Test that the original acquire() method still works
        rateLimiter.acquire() // Should succeed immediately
        rateLimiter.acquire() // Should wait ~50ms
        rateLimiter.acquire() // Should wait ~50ms
        
        // If we get here without timeout, the test passes
        Assert.assertTrue(true)
    }

    @Test
    fun testMixedAcquireAndTryAcquire() = runBlocking {
        val rateLimiter = RateLimiter.withInterval(100.milliseconds)
        
        // Use regular acquire
        rateLimiter.acquire()
        
        // Try to acquire with insufficient timeout
        val result1 = rateLimiter.tryAcquire(50.milliseconds)
        Assert.assertFalse("tryAcquire after acquire should fail with short timeout", result1)
        
        // Try to acquire with sufficient timeout
        val result2 = rateLimiter.tryAcquire(150.milliseconds)
        Assert.assertTrue("tryAcquire with sufficient timeout should succeed", result2)
    }
}
