package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.util.credential.SimpleStringCredentialManager
import cn.yiiguxing.plugin.translate.util.credential.StringCredentialManager
import com.intellij.credentialStore.generateServiceName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
internal class OpenAICredential private constructor() :
    StringCredentialManager by SimpleStringCredentialManager(SERVICE_NAME) {

    companion object {
        private val SERVICE_NAME =
            generateServiceName("OpenAI Credentials", "${TranslationPlugin.PLUGIN_ID}.OPENAI_API_KEY")

        private val service: OpenAICredential get() = service()

        val isApiKeySet: Boolean get() = service.isCredentialSet

        var apiKey: String?
            get() = service.credential
            set(value) {
                service.credential = value
            }
    }
}