package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.GOOGLE_TRANSLATE_HOST
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.RequestBuilder

private const val GOOGLE_REFERER = "https://translate.google.com/"
private const val GOOGLE_REFERER_CN = "https://translate.google.cn/"

private const val CHROME_VERSION = "93.0.4577.82"

private val USER_AGENT = getUserAgent()


val googleHost: String
    get() = GOOGLE_TRANSLATE_HOST

@Suppress("SpellCheckingInspection")
private fun getUserAgent(): String {
    val systemInformation =
        when {
            SystemInfo.isWindows -> {
                "Windows NT ${SystemInfo.OS_VERSION}${if (SystemInfo.is64Bit) "; Win64; x64" else ""}"
            }
            SystemInfo.isMac -> {
                val parts = SystemInfo.OS_VERSION.split('.').toMutableList()
                if (parts.size < 3) {
                    parts.add("0")
                }
                "Macintosh; Intel Mac OS X ${parts.joinToString("_")}"
            }
            else -> "X11; Linux x86${if (SystemInfo.is64Bit) "_64" else ""}"
        }

    return "Mozilla/5.0 ($systemInformation) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$CHROME_VERSION Safari/537.36 Edg/93.0.961.52"
}

fun RequestBuilder.userAgent(): RequestBuilder = apply { userAgent(USER_AGENT) }

fun RequestBuilder.googleReferer() = apply {
    tuner {
        it.setRequestProperty("Referer", GOOGLE_REFERER)
    }
}