package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.GOOGLE_TRANSLATE_HOST
import cn.yiiguxing.plugin.translate.GOOGLE_TRANSLATE_HOST_CN
import cn.yiiguxing.plugin.translate.util.Settings
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.RequestBuilder

private const val GOOGLE_REFERER = "https://translate.google.com/"
private const val GOOGLE_REFERER_CN = "https://translate.google.cn/"

private const val CHROME_VERSION = "92.0.4515.131"

object GoogleHttp {

    val googleHost: String
        get() = if (Settings.googleTranslateSettings.useTranslateGoogleCom) {
            GOOGLE_TRANSLATE_HOST
        } else {
            GOOGLE_TRANSLATE_HOST_CN
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

        return "Mozilla/5.0 ($systemInformation) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${CHROME_VERSION} Safari/537.36"
    }

}

fun RequestBuilder.userAgent(): RequestBuilder = apply { userAgent(GoogleHttp.getUserAgent()) }

fun RequestBuilder.googleReferer() = apply {
    tuner {
        val googleReferer = if (Settings.googleTranslateSettings.useTranslateGoogleCom) {
            GOOGLE_REFERER
        } else {
            GOOGLE_REFERER_CN
        }
        it.setRequestProperty("Referer", googleReferer)
    }
}