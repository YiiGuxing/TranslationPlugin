package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.TranslationPlugin.generateId
import cn.yiiguxing.plugin.translate.util.credential.SimpleStringCredentialManager
import cn.yiiguxing.plugin.translate.util.credential.StringCredentialManager
import com.intellij.credentialStore.generateServiceName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
internal class OpenAiCredentials private constructor() {

    private val openAi: StringCredentialManager by lazy { SimpleStringCredentialManager(OPEN_AI_SERVICE_NAME) }
    private val openAiTTS: StringCredentialManager by lazy { SimpleStringCredentialManager(OPEN_AI_TTS_SERVICE_NAME) }
    private val azure: StringCredentialManager by lazy { SimpleStringCredentialManager(AZURE_SERVICE_NAME) }

    companion object {
        private const val SUBSYSTEM_NAME = "OpenAI Credentials"
        private val OPEN_AI_SERVICE_NAME = generateServiceName(SUBSYSTEM_NAME, generateId("OPENAI_API_KEY"))
        private val OPEN_AI_TTS_SERVICE_NAME = generateServiceName(SUBSYSTEM_NAME, generateId("OPENAI_API_TTS_KEY"))
        private val AZURE_SERVICE_NAME = generateServiceName(SUBSYSTEM_NAME, generateId("AZURE_OPENAI_API_KEY"))

        private val service: OpenAiCredentials get() = service()

        fun manager(provider: ServiceProvider, forTTS: Boolean = false): StringCredentialManager = when (provider) {
            ServiceProvider.OpenAI -> if (forTTS) service.openAiTTS else service.openAi
            ServiceProvider.Azure -> service.azure
        }

        fun isCredentialSet(provider: ServiceProvider, forTTS: Boolean = false): Boolean {
            return manager(provider, forTTS).isCredentialSet
        }
    }
}