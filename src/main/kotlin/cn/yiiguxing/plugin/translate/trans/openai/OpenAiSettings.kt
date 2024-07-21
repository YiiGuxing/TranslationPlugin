package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.TranslationStorages
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient


/**
 * OpenAI settings.
 */
@Service
@State(name = "Translation.OpenAISettings", storages = [Storage(TranslationStorages.PREFERENCES_STORAGE_NAME)])
class OpenAiSettings : BaseState(), PersistentStateComponent<OpenAiSettings> {

    @get:OptionTag("PROVIDER")
    var provider: ServiceProvider by enum(ServiceProvider.OpenAI)

    @get:OptionTag("OPEN_AI")
    var openAi: OpenAi by property(OpenAi())

    @get:OptionTag("AZURE")
    var azure: Azure by property(Azure())

    @Transient
    fun isConfigured(configType: ConfigType): Boolean = when (provider) {
        ServiceProvider.Azure -> when (configType) {
            ConfigType.TRANSLATOR -> !with(azure) { endpoint.isNullOrBlank() || deployment.isNullOrBlank() }
            ConfigType.TTS -> !with(azure) { endpoint.isNullOrBlank() || ttsDeployment.isNullOrBlank() }
        }

        else -> true
    }

    fun getOptions(provider: ServiceProvider = this.provider): OpenAiService.Options = when (provider) {
        ServiceProvider.OpenAI -> openAi
        ServiceProvider.Azure -> azure
    }

    override fun getState(): OpenAiSettings = this

    override fun loadState(state: OpenAiSettings) {
        copyFrom(state)
    }

    open class CommonState : BaseState() {
        @get:OptionTag("ENDPOINT")
        var endpoint: String? by string()

        @get:OptionTag("TTS_MODEL")
        var ttsModel: OpenAiModel by enum(OpenAiModel.TTS_1)

        @get:OptionTag("TTS_VOICE")
        var ttsVoice: OpenAiTtsVoice by enum(OpenAiTtsVoice.ALLOY)

        @get:OptionTag("TTS_SPEED")
        var ttsSpeed: Int by property(100)
    }

    @Tag("open-ai")
    class OpenAi : CommonState(), OpenAiService.OpenAIOptions {
        @get:OptionTag("MODEL")
        override var model: OpenAiModel by enum(OpenAiModel.GPT_4O_MINI)
    }

    @Tag("azure")
    class Azure : CommonState(), OpenAiService.AzureOptions {
        @get:OptionTag("API_VERSION")
        override var apiVersion: AzureServiceVersion by enum(AzureServiceVersion.V2024_02_01)

        @get:OptionTag("TTS_API_VERSION")
        override var ttsApiVersion: AzureServiceVersion by enum(AzureServiceVersion.V2024_05_01_PREVIEW)

        @get:OptionTag("DEPLOYMENT_ID")
        override var deployment: String? by string()

        @get:OptionTag("TTS_DEPLOYMENT_ID")
        override var ttsDeployment: String? by string()
    }
}
