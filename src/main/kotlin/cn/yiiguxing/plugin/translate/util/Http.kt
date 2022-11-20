package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.TranslationPlugin
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import java.lang.reflect.Type
import java.net.HttpURLConnection

object Http {

    const val PLUGIN_USER_AGENT = "${TranslationPlugin.PLUGIN_ID}.TranslationPlugin"

    private val logger = Logger.getInstance(Http::class.java)

    val defaultGson = Gson()


    inline fun get(url: String, init: RequestBuilder.() -> Unit = {}): String {
        return HttpRequests.request(url)
            .accept("application/json")
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
            .accept("application/json")
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
        return post(url, "application/json", json, init)
    }

    fun post(
        url: String,
        contentType: String,
        data: String,
        init: RequestBuilder.() -> Unit
    ): String {
        logger.d("POST ==> $url\n\t|==> $data")
        return HttpRequests.post(url, contentType)
            .accept("application/json")
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


    private fun getUserAgent(): String {
        val chrome = "Chrome/107.0.0.0"
        val edge = "Edg/107.0.1418.52"
        val safari = "Safari/537.36"
        val systemInfo = "Windows NT ${SystemInfoRt.OS_VERSION}; Win64; x64"
        @Suppress("SpellCheckingInspection")
        return "Mozilla/5.0 ($systemInfo) AppleWebKit/537.36 (KHTML, like Gecko) $chrome $safari $edge"
    }

    fun RequestBuilder.userAgent(): RequestBuilder = apply { userAgent(getUserAgent()) }

    fun RequestBuilder.pluginUserAgent(): RequestBuilder = apply { userAgent(PLUGIN_USER_AGENT) }
}