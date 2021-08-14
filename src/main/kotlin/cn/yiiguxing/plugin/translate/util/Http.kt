package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.GOOGLE_TRANSLATE_HOST
import cn.yiiguxing.plugin.translate.GOOGLE_TRANSLATE_HOST_CN
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import java.net.URLConnection

private const val GOOGLE_REFERER = "https://translate.google.com/"
private const val GOOGLE_REFERER_CN = "https://translate.google.cn/"

private const val CHROME_VERSION = "92.0.4515.131"

object Http {

    val googleHost: String
        get() = if (Settings.googleTranslateSettings.useTranslateGoogleCom) {
            GOOGLE_TRANSLATE_HOST
        } else {
            GOOGLE_TRANSLATE_HOST_CN
        }

    fun postDataFrom(url: String, vararg dataFrom: Pair<String, String>, init: RequestBuilder.() -> Unit = {}): String {
        val data = dataFrom.joinToString("&") { (key, value) -> "$key=${value.urlEncode()}" }
        return HttpRequests.post(url, "application/x-www-form-urlencoded")
            .apply(init)
            .connect {
                it.write(data)
                it.readString()
            }
    }

    @Suppress("SpellCheckingInspection")
    fun getUserAgent(): String {
        val arch = System.getProperty("os.arch")
        val is64Bit = arch != null && "64" in arch
        val systemInformation =
            when {
                SystemInfo.isWindows -> {
                    "Windows NT ${SystemInfo.OS_VERSION}${if (is64Bit) "; Win64; x64" else ""}"
                }
                SystemInfo.isMac -> {
                    val parts = SystemInfo.OS_VERSION.split('.').toMutableList()
                    if (parts.size < 3) {
                        parts.add("0")
                    }
                    "Macintosh; Intel Mac OS X ${parts.joinToString("_")}"
                }
                else -> "X11; Linux x86${if (is64Bit) "_64" else ""}"
            }

        return "Mozilla/5.0 ($systemInformation) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$CHROME_VERSION Safari/537.36"
    }
}


fun RequestBuilder.userAgent(): RequestBuilder = apply { userAgent(Http.getUserAgent()) }

fun URLConnection.setGoogleReferer() = apply {
    val googleReferer = if (Settings.googleTranslateSettings.useTranslateGoogleCom) {
        GOOGLE_REFERER
    } else {
        GOOGLE_REFERER_CN
    }
    setRequestProperty("Referer", googleReferer)
}