package cn.yiiguxing.plugin.translate.trans.google

import com.intellij.openapi.components.service
import com.intellij.util.io.RequestBuilder


const val DEFAULT_GOOGLE_API_SERVER_URL = "https://translate.googleapis.com"

private const val GOOGLE_REFERER = "https://translate.google.com/"

internal val googleApiServerUrl: String
    get() = service<GoogleSettings>().let { settings ->
        if (settings.customServer) {
            settings.serverUrl ?: DEFAULT_GOOGLE_API_SERVER_URL
        } else {
            DEFAULT_GOOGLE_API_SERVER_URL
        }
    }

internal fun googleApiUrl(path: String): String {
    val serverUrl = googleApiServerUrl.let {
        if (it.endsWith('/')) it.trimEnd('/') else it
    }
    val apiPath = if (path.startsWith('/')) path.trimStart('/') else path
    return "$serverUrl/$apiPath"
}


internal fun RequestBuilder.googleReferer() = apply {
    tuner {
        it.setRequestProperty("Referer", GOOGLE_REFERER)
    }
}