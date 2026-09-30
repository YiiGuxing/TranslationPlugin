package cn.yiiguxing.plugin.translate

import com.intellij.util.xmlb.XmlSerializer
import org.junit.Assert
import org.junit.Test
import java.net.Proxy

class ProxySettingsTest {

    @Test
    fun `test defaults produce no proxy`() {
        val settings = ProxySettings()
        Assert.assertFalse("Proxy must be disabled by default", settings.enabled)
        Assert.assertEquals("Default type", ProxyType.HTTP, settings.type)
        Assert.assertEquals("Default host", "", settings.host)
        Assert.assertEquals("Default port", 0, settings.port)
        Assert.assertNull("Default settings must not yield a proxy", settings.toProxyOrNull())
    }

    @Test
    fun `test disabled yields no proxy even when address is valid`() {
        val settings = ProxySettings().apply {
            enabled = false
            host = "127.0.0.1"
            port = 7890
        }
        Assert.assertNull("Disabled settings must not yield a proxy", settings.toProxyOrNull())
    }

    @Test
    fun `test blank host yields no proxy`() {
        val settings = ProxySettings().apply {
            enabled = true
            host = "   "
            port = 7890
        }
        Assert.assertNull("Blank host must not yield a proxy", settings.toProxyOrNull())
    }

    @Test
    fun `test port out of range yields no proxy`() {
        for (port in listOf(-1, 0, 65536, Int.MAX_VALUE)) {
            val settings = ProxySettings().apply {
                enabled = true
                host = "127.0.0.1"
                this.port = port
            }
            Assert.assertNull("Port $port must not yield a proxy", settings.toProxyOrNull())
        }
    }

    @Test
    fun `test port boundaries yield a proxy`() {
        for (port in listOf(1, 65535)) {
            val settings = ProxySettings().apply {
                enabled = true
                host = "127.0.0.1"
                this.port = port
            }
            Assert.assertNotNull("Port $port must yield a proxy", settings.toProxyOrNull())
        }
    }

    @Test
    fun `test http type maps to proxy type http`() {
        val proxy = ProxySettings().apply {
            enabled = true
            type = ProxyType.HTTP
            host = "127.0.0.1"
            port = 7890
        }.toProxyOrNull()

        Assert.assertNotNull("Proxy", proxy)
        Assert.assertEquals("Proxy type", Proxy.Type.HTTP, proxy!!.type())
    }

    @Test
    fun `test socks type maps to proxy type socks`() {
        val proxy = ProxySettings().apply {
            enabled = true
            type = ProxyType.SOCKS
            host = "127.0.0.1"
            port = 1080
        }.toProxyOrNull()

        Assert.assertNotNull("Proxy", proxy)
        Assert.assertEquals("Proxy type", Proxy.Type.SOCKS, proxy!!.type())
    }

    @Test
    fun `test address is passed through`() {
        val proxy = ProxySettings().apply {
            enabled = true
            host = "127.0.0.1"
            port = 7890
        }.toProxyOrNull()

        Assert.assertNotNull("Proxy", proxy)
        val address = proxy!!.address() as java.net.InetSocketAddress
        Assert.assertEquals("Proxy host", "127.0.0.1", address.hostString)
        Assert.assertEquals("Proxy port", 7890, address.port)
    }

    @Test
    fun `test proxy settings survive xml serialization`() {
        val original = ProxySettings().apply {
            enabled = true
            type = ProxyType.SOCKS
            host = "127.0.0.1"
            port = 7897
        }

        val restored = XmlSerializer.deserialize(XmlSerializer.serialize(original), ProxySettings::class.java)

        Assert.assertTrue("enabled", restored.enabled)
        Assert.assertEquals("type", ProxyType.SOCKS, restored.type)
        Assert.assertEquals("host", "127.0.0.1", restored.host)
        Assert.assertEquals("port", 7897, restored.port)
    }
}
