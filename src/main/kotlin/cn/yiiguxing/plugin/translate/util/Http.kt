package cn.yiiguxing.plugin.translate.util

import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import java.lang.reflect.Type
import java.net.HttpURLConnection

object Http {

    private val logger = Logger.getInstance(Http::class.java)

    val defaultGson = Gson()

    inline fun <reified T> request(
        url: String,
        gson: Gson = defaultGson,
        typeOfT: Type = T::class.java,
        noinline init: RequestBuilder.() -> Unit = {}
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

    private fun post(
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

}