package cn.yiiguxing.plugin.translate.trans.google

import com.intellij.openapi.components.service
import com.intellij.openapi.util.registry.RegistryManager


internal const val DEFAULT_GOOGLE_API_SERVER_URL = "https://translate-pa.googleapis.com"
private const val DEFAULT_API_KEY_GTX = "AIzaSyDLEeFI5OtFBwYBIoK_jj5m32rZK5CkCXA"
private const val DEFAULT_API_KEY_TE_LIB = "AIzaSyATBXajvzQLTDHEQbcpq0Ihe0vWDHmO520"
private const val REGISTRY_KEY_GTX = "google.translate.gtx.apiKey"
private const val REGISTRY_KEY_TE_LIB = "google.translate.teLib.apiKey"


private val googleTranslateApiServerUrl: String
    get() = service<GoogleSettings>().let { settings ->
        if (settings.customServer) {
            settings.serverUrl ?: DEFAULT_GOOGLE_API_SERVER_URL
        } else {
            DEFAULT_GOOGLE_API_SERVER_URL
        }
    }

internal fun googleTranslateApiUrl(path: String, baseUrl: String = googleTranslateApiServerUrl): String {
    val serverUrl = baseUrl.trimEnd('/')
    val apiPath = path.trimStart('/')
    return "$serverUrl/$apiPath"
}

internal fun googleTranslateApiKey(client: GoogleTranslateClient): String {
    val registryManager = RegistryManager.getInstance()
    return when (client) {
        GoogleTranslateClient.GTX -> registryManager.stringValue(REGISTRY_KEY_GTX)
            .takeIf { !it.isNullOrBlank() }
            ?: DEFAULT_API_KEY_GTX

        GoogleTranslateClient.TE_LIB -> registryManager.stringValue(REGISTRY_KEY_TE_LIB)
            .takeIf { !it.isNullOrBlank() }
            ?: DEFAULT_API_KEY_TE_LIB
    }
}