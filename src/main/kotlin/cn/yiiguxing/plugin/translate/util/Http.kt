@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.RegistryKeys
import cn.yiiguxing.plugin.translate.TranslationPlugin
import com.google.gson.Gson
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.registry.RegistryManager
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

object Http {

    const val PLUGIN_USER_AGENT = "${TranslationPlugin.PLUGIN_ID}.TranslationPlugin"

    const val MIME_TYPE_JSON = "application/json"

    const val MIME_TYPE_FORM = "application/x-www-form-urlencoded"

    const val DEFAULT_CHROMIUM_VERSION = "150.0.4078.83"

    const val DEFAULT_MAX_REDIRECTS = 5

    private val REDIRECT_STATUS_CODES = setOf(
        HttpURLConnection.HTTP_MOVED_PERM, // 301 Moved Permanently
        HttpURLConnection.HTTP_MOVED_TEMP, // 302 Found
        307, // Temporary Redirect
        308  // Permanent Redirect
    )

    private val CHROMIUM_VERSION_REGEX = Regex("^\\d+(\\.\\d+){3}$")
    private val DEFAULT_CHROMIUM_VERSION_PARTS = DEFAULT_CHROMIUM_VERSION.split('.')

    val defaultGson = Gson()


    inline fun get(url: String, init: RequestBuilder.() -> Unit = {}): String {
        return HttpRequests.request(url)
            .accept(MIME_TYPE_JSON)
            .apply(init)
            .readString()
    }

    inline fun <reified T> request(
        url: String,
        gson: Gson = defaultGson,
        typeOfT: Type = T::class.java,
        init: RequestBuilder.() -> Unit = {}
    ): T {
        return HttpRequests.request(url)
            .accept(MIME_TYPE_JSON)
            .apply(init)
            .connect { gson.fromJson(it.reader, typeOfT) }
    }

    inline fun <reified T> post(
        url: String,
        vararg dataForm: Pair<String, String>,
        gson: Gson = defaultGson,
        typeOfT: Type = T::class.java,
        noinline init: RequestBuilder.() -> Unit = {}
    ): T {
        val result = post(url, dataForm.toMap(), init)
        return gson.fromJson(result, typeOfT)
    }

    fun post(
        url: String,
        vararg dataForm: Pair<String, String>,
        init: RequestBuilder.() -> Unit = {}
    ): String {
        return post(url, dataForm.toMap(), init)
    }

    fun post(
        url: String,
        dataForm: Map<String, String>,
        init: RequestBuilder.() -> Unit = {}
    ): String {
        val data = getFormUrlEncoded(dataForm)
        return post(url, MIME_TYPE_FORM, data, init)
    }

    inline fun <reified T> postJson(
        url: String,
        data: Any,
        gson: Gson = defaultGson,
        typeOfT: Type = T::class.java,
        noinline init: RequestBuilder.() -> Unit = {}
    ): T {
        val result = postJson(url, data, gson, init)
        return gson.fromJson(result, typeOfT)
    }

    fun postJson(url: String, data: Any, gson: Gson = defaultGson, init: RequestBuilder.() -> Unit = {}): String {
        val json = gson.toJson(data)
        return post(url, MIME_TYPE_JSON, json, init)
    }

    fun post(
        url: String,
        contentType: String,
        data: String,
        init: RequestBuilder.() -> Unit
    ): String {
        return HttpRequests.post(url, contentType)
            .accept(MIME_TYPE_JSON)
            .apply(init)
            .send(data) { it.readString() }
    }

    fun HttpRequests.Request.checkResponseCode() {
        val connection = connection as HttpURLConnection
        val responseCode = connection.responseCode
        if (responseCode >= 400) {
            throw StatusException(
                "Request failed with status code $responseCode",
                responseCode,
                url,
                connection.responseMessage,
                connection.getErrorText()
            )
        }
    }

    class StatusException(
        message: String,
        status: Int,
        url: String,
        val responseMessage: String?,
        val errorText: String?
    ) : HttpRequests.HttpStatusException(message, status, url)

    fun getFormUrlEncoded(dataForm: Map<String, String>): String {
        return dataForm.entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }
    }

    fun <T> RequestBuilder.send(data: String, dataReader: (HttpRequests.Request) -> T): T {
        var builder = this
        var redirectCount = DEFAULT_MAX_REDIRECTS

        while (true) {
            when (val result = builder.sendInternal(data, dataReader)) {
                is SendResult.Redirect -> {
                    if (redirectCount <= 0) {
                        throw IOException("Too many redirects: ${result.url}")
                    }
                    redirectCount--
                    builder = result.rebuildRequest()
                }

                is SendResult.Success -> return result.value
            }
        }
    }

    private fun <T> RequestBuilder.sendInternal(
        data: String,
        dataReader: (HttpRequests.Request) -> T
    ): SendResult<T> {
        throwStatusCodeException(false)
        return connect {
            val connection = it.connection as HttpURLConnection
            val requestHeaders = connection.requestProperties
            val contentType = connection.getRequestProperty("Content-Type")
            it.write(data)
            val redirect = connection.getRedirectOrNull(it.url, requestHeaders, contentType)
            if (redirect != null) {
                redirect
            } else {
                it.checkResponseCode()
                SendResult.Success(dataReader(it))
            }
        }
    }

    private fun HttpURLConnection.getRedirectOrNull(
        requestUrl: String,
        requestHeaders: Map<String, List<String>>,
        contentType: String?
    ): SendResult.Redirect? {
        val statusCode = responseCode
        if (statusCode !in REDIRECT_STATUS_CODES) {
            return null
        }
        return getHeaderField("Location")?.let { location ->
            SendResult.Redirect(
                resolveRedirectUrl(requestUrl, location),
                requestHeaders,
                contentType
            )
        }
    }

    private fun resolveRedirectUrl(baseUrl: String, location: String): String {
        return if (location.contains("://")) {
            location
        } else {
            URL(URL(baseUrl), location).toExternalForm()
        }
    }

    private fun SendResult.Redirect.rebuildRequest(): RequestBuilder {
        return HttpRequests.post(url, contentType)
            .accept(MIME_TYPE_JSON)
            .tuner { connection ->
                headers.forEach { (key, values) ->
                    if (key.equals("Content-Type", ignoreCase = true)) {
                        return@forEach
                    }
                    values.forEach { value -> connection.setRequestProperty(key, value) }
                }
            }
    }

    private sealed interface SendResult<out T> {
        data class Success<T>(val value: T) : SendResult<T>

        data class Redirect(
            val url: String,
            val headers: Map<String, List<String>>,
            val contentType: String?
        ) : SendResult<Nothing>
    }

    fun <T> RequestBuilder.sendForm(dataForm: Map<String, String>, dataReader: (HttpRequests.Request) -> T): T {
        return send(getFormUrlEncoded(dataForm), dataReader)
    }

    fun <T> RequestBuilder.sendJson(data: Any, dataReader: (HttpRequests.Request) -> T): T {
        return send(defaultGson.toJson(data), dataReader)
    }

    private fun HttpURLConnection.getErrorText(): String? {
        val errorStream = errorStream ?: return null
        val stream = if (contentEncoding == "gzip") GZIPInputStream(errorStream) else errorStream
        return InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
    }

    private fun isVersionGreaterThanDefault(version: String): Boolean {
        val versionParts = version.split('.')
        for (index in versionParts.indices) {
            val comparison = compareVersionPart(versionParts[index], DEFAULT_CHROMIUM_VERSION_PARTS[index])
            if (comparison != 0) return comparison > 0
        }
        return false
    }

    private fun compareVersionPart(left: String, right: String): Int {
        val normalizedLeft = left.trimStart('0').ifEmpty { "0" }
        val normalizedRight = right.trimStart('0').ifEmpty { "0" }
        return normalizedLeft.length.compareTo(normalizedRight.length).takeIf { it != 0 }
            ?: normalizedLeft.compareTo(normalizedRight)
    }

    fun getAgentChromiumVersion(): String = RegistryManager.getInstance()
        .stringValue(RegistryKeys.HTTP_AGENT_CHROMIUM_VERSION)
        ?.trim()
        ?.takeIf {
            it.isNotEmpty() && it.matches(CHROMIUM_VERSION_REGEX) && isVersionGreaterThanDefault(it)
        }
        ?: DEFAULT_CHROMIUM_VERSION

    fun getUserAgent(): String {
        val chromiumMajorVersion = getAgentChromiumVersion().substringBefore('.').toInt()
        val chrome = "Chrome/$chromiumMajorVersion.0.0.0"
        val edge = "Edg/$chromiumMajorVersion.0.0.0"
        val safari = "Safari/537.36"
        val appleWebKit = "AppleWebKit/537.36"
        val mozilla = "Mozilla/5.0"
        val systemInfo = "Windows NT ${if (SystemInfoRt.isWindows) SystemInfoRt.OS_VERSION else "10.0"}; Win64; x64"
        return "$mozilla ($systemInfo) $appleWebKit (KHTML, like Gecko) $chrome $safari $edge"
    }

    fun RequestBuilder.setUserAgent(): RequestBuilder = apply { userAgent(getUserAgent()) }

    fun RequestBuilder.pluginUserAgent(): RequestBuilder = apply { userAgent(PLUGIN_USER_AGENT) }
}