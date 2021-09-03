package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder

object Http {

    private val logger = Logger.getInstance(Http::class.java)

    fun postDataFrom(url: String, vararg dataFrom: Pair<String, String>, init: RequestBuilder.() -> Unit = {}): String {
        val data = dataFrom.joinToString("&") { (key, value) -> "$key=${value.urlEncode()}" }
        logger.d("POST ==> $url\n\t|==> $data")
        return HttpRequests.post(url, "application/x-www-form-urlencoded")
            .apply(init)
            .connect {
                it.write(data)
                it.readString()
            }
    }

}