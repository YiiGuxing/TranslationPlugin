package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.util.credential.SimpleStringCredentialManager
import cn.yiiguxing.plugin.translate.util.credential.StringCredentialManager
import com.intellij.credentialStore.generateServiceName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
internal class OpenAiCredentials private constructor() {

    val openAi = SimpleStringCredentialManager(OPEN_AI_SERVICE_NAME)
    val azure = SimpleStringCredentialManager(AZURE_SERVICE_NAME)

    companion object {
        private val OPEN_AI_SERVICE_NAME =
            generateServiceName("OpenAI Credentials", "${TranslationPlugin.PLUGIN_ID}.OPENAI_API_KEY")
        private val AZURE_SERVICE_NAME =
            generateServiceName("OpenAI Credentials", "${TranslationPlugin.PLUGIN_ID}.AZURE_OPENAI_API_KEY")

        private val service: OpenAiCredentials get() = service()

        fun manager(provider: ServiceProvider): StringCredentialManager = when (provider) {
            ServiceProvider.OpenAI -> service.openAi
            ServiceProvider.Azure -> service.azure
        }

        fun isCredentialSet(provider: ServiceProvider): Boolean = manager(provider).isCredentialSet
    }
}