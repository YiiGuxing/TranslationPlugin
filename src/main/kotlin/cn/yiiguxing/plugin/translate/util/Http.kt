@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.TranslationPlugin
import com.google.gson.Gson
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.util.zip.GZIPInputStream

object Http {

    const val PLUGIN_USER_AGENT = "${TranslationPlugin.PLUGIN_ID}.TranslationPlugin"

    const val MIME_TYPE_JSON = "application/json"

    const val MIME_TYPE_FORM = "application/x-www-form-urlencoded"

    const val CHROMIUM_VERSION = "144.0.3719.92"

    val CHROMIUM_MAJOR_VERSION: Int = CHROMIUM_VERSION.substringBefore('.').toInt()

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
            .throwStatusCodeException(false)
            .connect {
                it.write(data)
                it.checkResponseCode()
                it.readString()
            }
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
        throwStatusCodeException(false)
        return connect {
            it.write(data)
            it.checkResponseCode()
            dataReader(it)
        }
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

    fun getUserAgent(): String {
        val chrome = "Chrome/$CHROMIUM_MAJOR_VERSION.0.0.0"
        val edge = "Edg/$CHROMIUM_MAJOR_VERSION.0.0.0"
        val safari = "Safari/537.36"
        val appleWebKit = "AppleWebKit/537.36"
        val mozilla = "Mozilla/5.0"
        val systemInfo = "Windows NT ${if (SystemInfoRt.isWindows) SystemInfoRt.OS_VERSION else "10.0"}; Win64; x64"
        @Suppress("SpellCheckingInspection")
        return "$mozilla ($systemInfo) $appleWebKit (KHTML, like Gecko) $chrome $safari $edge"
    }

    fun RequestBuilder.setUserAgent(): RequestBuilder = apply { userAgent(getUserAgent()) }

    fun RequestBuilder.pluginUserAgent(): RequestBuilder = apply { userAgent(PLUGIN_USER_AGENT) }
}