package cn.yiiguxing.plugin.translate.trans.deeplx

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.util.credential.SimpleStringCredentialManager
import cn.yiiguxing.plugin.translate.util.credential.StringCredentialManager
import com.intellij.credentialStore.generateServiceName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
internal class DeeplxCredential private constructor() :
    StringCredentialManager by SimpleStringCredentialManager(SERVICE_NAME) {

    companion object {

        private val SERVICE_NAME =
            generateServiceName("DeepLx Credentials", "${TranslationPlugin.PLUGIN_ID}.DEEPLX_API_ENDPOINT")

        private val service: DeeplxCredential get() = service()

        val isApiEndpointSet: Boolean get() = service.isCredentialSet

        var apiEndpoint: String?
            get() = service.credential
            set(value) {
                service.credential = value
            }
    }
}
