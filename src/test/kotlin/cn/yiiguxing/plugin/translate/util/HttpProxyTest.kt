package cn.yiiguxing.plugin.translate.util

import org.junit.Assert
import org.junit.Test
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Verifies that [Http] routes requests through a plugin-configured proxy instead of
 * connecting to the target host directly.
 *
 * The target host is `example.invalid`, which never resolves, so a request that reached
 * the origin directly could not succeed. For plain HTTP an HTTP proxy is addressed with
 * an absolute-form request line, so the recorded request line itself proves the proxy
 * was used.
 */
class HttpProxyTest {

    @Test
    fun `test get is routed through the configured proxy`() {
        FakeProxy().use { proxy ->
            val result = Http.get(TARGET_URL, proxy.proxy)

            Assert.assertEquals("Response body", "hello", result)
            val line = proxy.requestLine.get()
            Assert.assertNotNull("The proxy must receive the request", line)
            Assert.assertTrue(
                "The proxy must receive an absolute-form request line, but was: $line",
                line!!.startsWith("GET ") && line.contains(TARGET_URL)
            )
        }
    }

    @Test
    fun `test post body is sent through the configured proxy`() {
        FakeProxy().use { proxy ->
            val result = Http.post(TARGET_URL, proxy.proxy, Http.MIME_TYPE_FORM, "key=value")

            Assert.assertEquals("Response body", "hello", result)
            val line = proxy.requestLine.get()
            Assert.assertNotNull("The proxy must receive the request", line)
            Assert.assertTrue(
                "The proxy must receive an absolute-form POST request line, but was: $line",
                line!!.startsWith("POST ") && line.contains(TARGET_URL)
            )
            Assert.assertEquals("Request body", "key=value", proxy.requestBody.get())
        }
    }

    @Test
    fun `test requests ask for gzip by default like the platform builder does`() {
        // HttpRequests$RequestBuilderImpl defaults myGzip to true. The proxied path must match,
        // otherwise the same request behaves differently depending on whether a proxy is set.
        FakeProxy().use { proxy ->
            Http.get(TARGET_URL, proxy.proxy)

            val headers = proxy.requestHeaders.get()
            Assert.assertTrue(
                "Should send Accept-Encoding: gzip by default, but headers were: $headers",
                headers.any { it.startsWith("Accept-Encoding:", ignoreCase = true) && it.contains("gzip") }
            )
        }
    }

    /**
     * A minimal HTTP proxy: records the request line, headers and body, then answers with a
     * canned response. It never forwards anything, which is fine because the assertions only
     * care that the request arrived here rather than at the (nonexistent) origin.
     */
    private class FakeProxy : Closeable {

        private val server = ServerSocket(0)

        val requestLine = AtomicReference<String?>(null)
        val requestBody = AtomicReference<String?>(null)
        val requestHeaders = AtomicReference<List<String>>(emptyList())

        private val serverThread = thread(isDaemon = true, name = "fake-proxy") { serve() }

        val proxy: Proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", server.localPort))

        override fun close() {
            server.close()
            serverThread.join(5_000)
        }

        private fun serve() {
            try {
                server.accept().use { socket ->
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.ISO_8859_1))
                    requestLine.set(reader.readLine())
                    requestBody.set(readBody(reader))

                    val body = BODY
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n" +
                            "Connection: close\r\n" +
                            "\r\n" +
                            body
                    socket.getOutputStream().apply {
                        write(response.toByteArray(Charsets.ISO_8859_1))
                        flush()
                    }
                }
            } catch (_: Throwable) {
                // The test is over and the server socket was closed.
            }
        }

        private fun readBody(reader: BufferedReader): String? {
            val headers = mutableListOf<String>()
            var contentLength = 0
            while (true) {
                val line = reader.readLine()
                if (line.isNullOrEmpty()) break
                headers += line
                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line.substringAfter(':').trim().toIntOrNull() ?: 0
                }
            }
            requestHeaders.set(headers)
            if (contentLength <= 0) {
                return null
            }
            val buffer = CharArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val count = reader.read(buffer, read, contentLength - read)
                if (count < 0) break
                read += count
            }
            return String(buffer, 0, read)
        }

        private companion object {
            const val BODY = "hello"
        }
    }

    private companion object {
        const val TARGET_URL = "http://example.invalid/test"
    }
}