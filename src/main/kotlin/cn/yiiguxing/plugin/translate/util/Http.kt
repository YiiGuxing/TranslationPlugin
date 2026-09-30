@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.RegistryKeys
import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.TranslationPlugin
import com.google.gson.Gson
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.registry.RegistryManager
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.net.URLConnection
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPInputStream
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

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


    /**
     * 返回当前配置的代理。未启用代理时返回 `null`，此时请求交由平台的 [HttpRequests]
     * 处理，即沿用 IDE 的全局代理设置。
     */
    private fun currentProxy(): Proxy? = Settings.getInstance().proxySettings.toProxyOrNull()

    /**
     * 创建请求。代理为空时使用平台的 [HttpRequests]，否则使用自建的、走指定代理的连接。
     * [contentType] 不为 `null` 时创建 POST 请求。
     */
    @PublishedApi
    internal fun newRequest(url: String, contentType: String? = null): RequestBuilder =
        newRequest(url, currentProxy(), contentType)

    internal fun newRequest(url: String, proxy: Proxy?, contentType: String? = null): RequestBuilder {
        if (proxy == null) {
            return if (contentType == null) HttpRequests.request(url) else HttpRequests.post(url, contentType)
        }
        return ProxiedRequestBuilder(url, contentType, proxy)
    }

    fun get(url: String, init: RequestBuilder.() -> Unit = {}): String {
        return get(url, currentProxy(), init)
    }

    internal fun get(url: String, proxy: Proxy?, init: RequestBuilder.() -> Unit = {}): String {
        return newRequest(url, proxy)
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
        return newRequest(url)
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
        init: RequestBuilder.() -> Unit = {}
    ): String {
        return post(url, currentProxy(), contentType, data, init)
    }

    internal fun post(
        url: String,
        proxy: Proxy?,
        contentType: String,
        data: String,
        init: RequestBuilder.() -> Unit = {}
    ): String {
        return newRequest(url, proxy, contentType)
            .accept(MIME_TYPE_JSON)
            .apply(init)
            .send(data) { it.readString() }
    }

    fun HttpRequests.Request.checkResponseCode() {
        val connection = connection as HttpURLConnection
        val responseCode = connection.responseCode
        if (responseCode >= 400) {
            throw Http.StatusException(
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
        return newRequest(url, contentType)
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

/**
 * 走插件指定代理的 [RequestBuilder]。
 *
 * 平台的 [HttpRequests] 只能表达"用 / 不用 IDE 代理"（`HttpRequests$RequestImpl` 仅依据
 * `myUseProxy` 分叉），无法指定代理地址，因此这里自行创建连接。`RequestBuilder` 的
 * `readString` / `readBytes` / `saveToFile` 都由基类实现并回调 [connect]，所以只需重写
 * [connect]；其余配置方法退化为本地记录。
 */
private class ProxiedRequestBuilder(
    private val url: String,
    private val contentType: String?,
    private val proxy: Proxy
) : RequestBuilder() {

    // 默认值与平台的 HttpRequests$RequestBuilderImpl 保持一致，否则走代理与直连两条路径
    // 在超时、压缩、重定向上的行为会不同。
    private var accepted: String? = null
    private var userAgent: String? = null
    private var connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT
    private var readTimeout: Int = DEFAULT_READ_TIMEOUT
    private var gzip: Boolean = true
    private var redirectLimit: Int = DEFAULT_REDIRECT_LIMIT
    private var forceHttps: Boolean = false
    private var hostNameVerifier: HostnameVerifier? = null
    private var tuner: HttpRequests.ConnectionTuner? = null
    private var throwStatusCodeException: Boolean = true

    override fun accept(contentType: String?): RequestBuilder = apply { accepted = contentType }

    override fun userAgent(userAgent: String?): RequestBuilder = apply { this.userAgent = userAgent }

    override fun productNameAsUserAgent(): RequestBuilder = apply { userAgent = Http.PLUGIN_USER_AGENT }

    override fun connectTimeout(timeout: Int): RequestBuilder = apply { connectTimeout = timeout }

    override fun readTimeout(timeout: Int): RequestBuilder = apply { readTimeout = timeout }

    override fun gzip(gzip: Boolean): RequestBuilder = apply { this.gzip = gzip }

    override fun redirectLimit(limit: Int): RequestBuilder = apply { redirectLimit = limit }

    override fun forceHttps(forceHttps: Boolean): RequestBuilder = apply { this.forceHttps = forceHttps }

    override fun hostNameVerifier(verifier: HostnameVerifier?): RequestBuilder =
        apply { hostNameVerifier = verifier }

    /** 请求始终使用插件配置的代理，忽略平台的代理开关。 */
    override fun useProxy(useProxy: Boolean): RequestBuilder = this

    override fun throwStatusCodeException(throwException: Boolean): RequestBuilder =
        apply { throwStatusCodeException = throwException }

    override fun isReadResponseOnError(value: Boolean): RequestBuilder = this

    override fun tuner(tuner: HttpRequests.ConnectionTuner?): RequestBuilder = apply { this.tuner = tuner }

    override fun <T> connect(processor: HttpRequests.RequestProcessor<T>): T {
        val connection = openConnection()
        return try {
            processor.process(ProxiedRequest(connection, throwStatusCodeException))
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(): HttpURLConnection {
        val connection = URL(resolveUrl()).openConnection(proxy) as HttpURLConnection
        connection.instanceFollowRedirects = redirectLimit > 0
        connection.useCaches = false

        if (connectTimeout > 0) {
            connection.connectTimeout = connectTimeout
        }
        if (readTimeout > 0) {
            connection.readTimeout = readTimeout
        }
        if (contentType != null) {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", contentType)
        }
        accepted?.let { connection.setRequestProperty("Accept", it) }
        userAgent?.let { connection.setRequestProperty("User-Agent", it) }
        if (gzip) {
            connection.setRequestProperty("Accept-Encoding", "gzip")
        }
        if (connection is HttpsURLConnection) {
            hostNameVerifier?.let { connection.hostnameVerifier = it }
        }
        tuner?.tune(connection)

        return connection
    }

    private fun resolveUrl(): String = if (forceHttps && url.startsWith("http:")) {
        "https:${url.substring("http:".length)}"
    } else {
        url
    }

    private companion object {
        /** 取自 `HttpRequests$RequestBuilderImpl.myConnectTimeout`。 */
        private const val DEFAULT_CONNECT_TIMEOUT = 10_000

        /** 取自 `HttpRequests$RequestBuilderImpl.myTimeout`。 */
        private const val DEFAULT_READ_TIMEOUT = 60_000

        /** 取自 `HttpRequests$RequestBuilderImpl.myRedirectLimit`。 */
        private const val DEFAULT_REDIRECT_LIMIT = 10
    }
}

private fun HttpURLConnection.getErrorText(): String? {
    val errorStream = errorStream ?: return null
    val stream = if (contentEncoding?.contains("gzip", ignoreCase = true) == true) {
        GZIPInputStream(errorStream)
    } else {
        errorStream
    }
    return InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
}

/**
 * [HttpRequests.Request] 的代理版本实现。多数方法直接委托给底层连接；
 * `write` 由接口的默认实现提供。
 */
private class ProxiedRequest(
    private val connection: HttpURLConnection,
    private val throwStatusCodeException: Boolean
) : HttpRequests.Request {

    override fun getConnection(): URLConnection = connection

    override fun getURL(): String = connection.url.toExternalForm()

    override fun getInputStream(): InputStream {
        val stream = connection.inputStream
        return if (connection.contentEncoding?.contains("gzip", ignoreCase = true) == true) {
            GZIPInputStream(stream)
        } else {
            stream
        }
    }

    override fun getReader(): BufferedReader = getReader(null)

    override fun getReader(indicator: ProgressIndicator?): BufferedReader {
        return BufferedReader(InputStreamReader(getInputStream(), getCharset()))
    }

    override fun readString(indicator: ProgressIndicator?): String {
        throwStatusCodeErrorIfNeeded()
        return getReader(indicator).use { it.readText() }
    }

    override fun readChars(indicator: ProgressIndicator?): CharSequence = readString(indicator)

    override fun readBytes(indicator: ProgressIndicator?): ByteArray {
        throwStatusCodeErrorIfNeeded()
        return getInputStream().use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                indicator?.checkCanceled()
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }

    override fun saveToFile(file: Path, indicator: ProgressIndicator?): Path =
        saveToFile(file, indicator, false)

    override fun saveToFile(file: Path, indicator: ProgressIndicator?, append: Boolean): Path {
        throwStatusCodeErrorIfNeeded()
        file.parent?.let { Files.createDirectories(it) }
        val options = if (append) {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        } else {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }
        getInputStream().use { input ->
            Files.newOutputStream(file, *options).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    indicator?.checkCanceled()
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
            }
        }
        return file
    }

    override fun readError(): String? {
        val stream = connection.errorStream ?: return null
        val charset = getCharset()
        return stream.use {
            val input = if (connection.contentEncoding?.contains("gzip", ignoreCase = true) == true) {
                GZIPInputStream(it)
            } else {
                it
            }
            input.reader(charset).readText()
        }
    }

    /**
     * 平台在响应码 >= 400 且未禁用状态码异常时抛出 [HttpRequests.HttpStatusException]。
     */
    private fun throwStatusCodeErrorIfNeeded() {
        if (!throwStatusCodeException) {
            return
        }
        val statusCode = connection.responseCode
        if (statusCode >= 400) {
            throw Http.StatusException(
                "Request failed with status code $statusCode",
                statusCode,
                connection.url.toExternalForm(),
                connection.responseMessage,
                connection.getErrorText()
            )
        }
    }

    private fun getCharset(): Charset {
        val contentType = connection.contentType ?: return Charsets.UTF_8
        return CHARSET_PATTERN.find(contentType)?.groupValues?.get(1)?.let { name ->
            runCatching { Charset.forName(name.trim().trim('"')) }.getOrNull()
        } ?: Charsets.UTF_8
    }

    private companion object {
        private val CHARSET_PATTERN = Regex("charset=([^;]+)", RegexOption.IGNORE_CASE)
    }
}