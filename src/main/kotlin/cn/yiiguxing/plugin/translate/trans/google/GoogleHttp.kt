package cn.yiiguxing.plugin.translate.trans.google

import com.intellij.util.io.RequestBuilder

private const val GOOGLE_REFERER = "https://translate.google.com/"

fun RequestBuilder.googleReferer() = apply {
    tuner {
        it.setRequestProperty("Referer", GOOGLE_REFERER)
    }
}