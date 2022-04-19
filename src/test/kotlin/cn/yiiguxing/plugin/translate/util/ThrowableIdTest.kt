package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.diagnostic.generateId
import org.junit.Assert
import org.junit.Test

class ThrowableIdTest {

    private fun getException(msg: String, cause: Throwable? = null, vararg suppressed: Throwable): Exception {
        return Exception(msg, cause).apply {
            suppressed.forEach { addSuppressed(it) }
        }
    }

    @Test
    fun testBase() {
        val (e2, e3) = getException("test").generateId() to RuntimeException("test").generateId()
        Assert.assertEquals(true, e2 != e3)
    }


    @Test
    fun testMessage() {
        val (e1, e2) = getException("test").generateId() to getException("test").generateId()
        Assert.assertEquals(true, e1 == e2)

        val (e3, e4) = getException("test1").generateId() to getException("test2").generateId()
        Assert.assertEquals(true, e3 == e4)

        val (e5, e6) =
            getException("test1", getException("test2")).generateId() to getException("test1", getException("test2")).generateId()
        Assert.assertEquals(true, e5 == e6)

        val (e7, e8) =
            getException("test1", getException("test2")).generateId() to getException("test3", getException("test4")).generateId()
        Assert.assertEquals(true, e7 == e8)

        val set = setOf(e1, e3, e5, e7)
        Assert.assertEquals(4, set.size)
    }

    @Test
    fun testCause() {
        val (e1, e2) = getException("test1") to getException("test2")
        val (e3, e4) = getException("test3", e1).generateId() to getException("test4", e2).generateId()
        Assert.assertEquals(true, e3 == e4)

        val e5 = getException("test5", e1).generateId()
        Assert.assertEquals(true, e3 != e5)

        val (e6, e7) = getException("test", e1).generateId() to getException("test").generateId()
        Assert.assertEquals(true, e6 != e7)
    }

    @Test
    fun testSuppressed() {
        val (e1, e2) = getException("test1") to getException("test2")
        val (e3, e4) = getException("test3", null, e1).generateId() to getException("test4", null, e2).generateId()
        Assert.assertEquals(true, e3 == e4)

        val (e5, e6) = getException("test").generateId() to getException("test", null, e2).generateId()
        Assert.assertEquals(true, e5 != e6)

        val (e7, e8) = getException("test", e1).generateId() to getException("test", null, e2).generateId()
        Assert.assertEquals(true, e7 != e8)
    }


}