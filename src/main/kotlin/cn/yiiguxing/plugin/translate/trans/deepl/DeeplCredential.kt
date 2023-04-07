package cn.yiiguxing.plugin.translate.trans.deepl

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.util.credential.SimpleStringCredentialManager
import cn.yiiguxing.plugin.translate.util.credential.StringCredentialManager
import com.intellij.credentialStore.generateServiceName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
internal class DeeplCredential private constructor() :
    StringCredentialManager by SimpleStringCredentialManager(SERVICE_NAME) {

    companion object {

        private val SERVICE_NAME =
            generateServiceName("DeepL Credentials", "${TranslationPlugin.PLUGIN_ID}.DEEPL_AUTH_KEY")

        private val service: DeeplCredential get() = service()

        val isAuthKeySet: Boolean get() = service.isCredentialSet

        var authKey: String?
            get() = service.credential
            set(value) {
                service.credential = value
            }
    }
}