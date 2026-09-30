package cn.yiiguxing.plugin.translate.util

import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * The assertions are deliberately locale-independent: the bundle resolves against the
 * JVM's default locale, so they check the shape of the message rather than its wording.
 */
class ExceptionsTest {

    @Test
    fun `test connection failure appends a hint when a proxy is configured`() {
        for (error in listOf(
            ConnectException("Connection refused"),
            UnknownHostException("example.invalid"),
            SocketTimeoutException("Read timed out")
        )) {
            val withProxy = error.getCommonMessage(proxyConfigured = true)
            val withoutProxy = error.getCommonMessage(proxyConfigured = false)
            Assert.assertTrue(
                "${error::class.java.simpleName} should append a hint, but was: $withProxy",
                withProxy != withoutProxy && withProxy.startsWith(withoutProxy)
            )
        }
    }

    @Test
    fun `test status errors never append a hint`() {
        assertNoHint(Http.StatusException("Too Many Requests", 429, "http://example.invalid/", null, null))
    }

    @Test
    fun `test unrelated io errors never append a hint`() {
        assertNoHint(IOException("Something else went wrong"))
    }

    private fun assertNoHint(error: IOException) {
        val withProxy = error.getCommonMessage(proxyConfigured = true)
        val withoutProxy = error.getCommonMessage(proxyConfigured = false)
        Assert.assertEquals(
            "${error::class.java.simpleName} should not mention the proxy",
            withoutProxy,
            withProxy
        )
    }
}