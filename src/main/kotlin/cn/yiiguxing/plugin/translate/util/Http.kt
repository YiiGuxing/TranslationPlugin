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
        val data = dataForm.entries.joinToString("&") { (key, value) ->
            "$key=${value.urlEncode()}"
        }
        return post(url, "application/x-www-form-urlencoded", data, init)
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

    private fun HttpRequests.Request.checkResponseCode() {
        val responseCode = (connection as HttpURLConnection).responseCode
        if (responseCode >= 400) {
            throw HttpRequests.HttpStatusException("Request failed with status code $responseCode", responseCode, url)
        }
    }

    /**
     * A simple Http response.
     */
    sealed class Response private constructor(val code: Int, val message: String, open val body: Any?) {
        /**
         * Successful response.
         */
        class Success(code: Int, message: String, override val body: String) : Response(code, message, body)

        /**
         * Error response.
         */
        class Error(code: Int, message: String, override val body: String?) : Response(code, message, body)

        override fun toString(): String {
            return "Response - ${javaClass.simpleName}\n" +
                    "\tcode: $code\n" +
                    "\tmessage: $message\n" +
                    "\tbody: $body"
        }
    }

    /**
     * Sends a request and returns a [Response].
     */
    fun RequestBuilder.send(data: String): Response {
        throwStatusCodeException(false)
        return connect {
            it.write(data)

            val connection = it.connection as HttpURLConnection
            val responseCode = connection.responseCode
            val message = connection.responseMessage
            if (responseCode < 400) {
                Response.Success(responseCode, message, it.readString())
            } else {
                Response.Error(responseCode, message, connection.getErrorText())
            }
        }
    }

    /**
     * Sends a request with the specified [data] in JSON format and returns a [Response].
     */
    fun RequestBuilder.sendJson(data: Any): Response {
        return send(defaultGson.toJson(data))
    }

    private fun HttpURLConnection.getErrorText(): String? {
        val errorStream = errorStream ?: return null
        val stream = if (contentEncoding == "gzip") GZIPInputStream(errorStream) else errorStream
        return InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
    }

    private fun getUserAgent(): String {
        val chrome = "Chrome/115.0.0.0"
        val edge = "Edg/115.0.1901.203"
        val safari = "Safari/537.36"
        val systemInfo = "Windows NT ${SystemInfoRt.OS_VERSION}; Win64; x64"
        @Suppress("SpellCheckingInspection")
        return "Mozilla/5.0 ($systemInfo) AppleWebKit/537.36 (KHTML, like Gecko) $chrome $safari $edge"
    }

    fun RequestBuilder.userAgent(): RequestBuilder = apply { userAgent(getUserAgent()) }

    fun RequestBuilder.pluginUserAgent(): RequestBuilder = apply { userAgent(PLUGIN_USER_AGENT) }
}