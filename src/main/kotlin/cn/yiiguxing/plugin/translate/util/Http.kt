package cn.yiiguxing.plugin.translate.util

import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder

object Http {

    fun postDataFrom(url: String, vararg dataFrom: Pair<String, String>, init: RequestBuilder.() -> Unit = {}): String {
        val data = dataFrom.joinToString("&") { (key, value) -> "$key=${value.urlEncode()}" }
        return HttpRequests.post(url, "application/x-www-form-urlencoded")
            .apply(init)
            .connect {
                it.write(data)
                it.readString()
            }
    }

}