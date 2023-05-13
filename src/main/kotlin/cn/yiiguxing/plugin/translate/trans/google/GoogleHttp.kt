package cn.yiiguxing.plugin.translate.trans.google

import com.intellij.openapi.components.service
import com.intellij.util.io.RequestBuilder


internal const val GOOGLE_APIS_URL = "https://translate.googleapis.com"

private const val GOOGLE_REFERER = "https://translate.google.com/"

internal val googleApiServiceUrl: String
    get() = service<GoogleSettings>().let { settings ->
        if (settings.useMirror) {
            settings.mirrorUrl ?: GOOGLE_APIS_URL
        } else {
            GOOGLE_APIS_URL
        }
    }

internal fun googleApiUrl(path: String): String {
    val serviceUrl = googleApiServiceUrl.let {
        if (it.endsWith('/')) it.trimEnd('/') else it
    }
    val apiPath = if (path.startsWith('/')) path.trimStart('/') else path
    return "$serviceUrl/$apiPath"
}


internal fun RequestBuilder.googleReferer() = apply {
    tuner {
        it.setRequestProperty("Referer", GOOGLE_REFERER)
    }
}