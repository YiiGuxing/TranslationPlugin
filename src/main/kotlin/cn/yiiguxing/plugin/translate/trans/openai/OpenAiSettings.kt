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

    @get:Transient
    val isConfigured: Boolean
        get() = when (provider) {
            ServiceProvider.Azure -> !with(azure) { endpoint.isNullOrBlank() || model.isNullOrBlank() }

            else -> true
        }

    fun getOptions(provider: ServiceProvider = this.provider): OpenAiService.Options<*> = when (provider) {
        ServiceProvider.OpenAI -> openAi
        ServiceProvider.Azure -> azure
    }

    override fun getState(): OpenAiSettings = this

    override fun loadState(state: OpenAiSettings) {
        copyFrom(state)
    }

    @Tag("open-ai")
    class OpenAi : BaseState(), OpenAiService.OpenAIOptions {
        @get:OptionTag("ENDPOINT")
        override var endpoint: String? by string()

        @get:OptionTag("MODEL")
        override var model: OpenAiModel by enum(OpenAiModel.GPT_3_5_TURBO)

        @get:OptionTag("TTS_MODEL")
        override var ttsModel: OpenAiModel by enum(OpenAiModel.TTS_1)

        @get:OptionTag("TTS_VOICE")
        override var ttsVoice: OpenAiTtsVoice by enum(OpenAiTtsVoice.ALLOY)

        @get:OptionTag("TTS_SPEED")
        override var ttsSpeed: Float by property(1.0f)
    }

    @Tag("azure")
    class Azure : BaseState(), OpenAiService.AzureOptions {
        @get:OptionTag("ENDPOINT")
        override var endpoint: String? by string()

        @get:OptionTag("API_VERSION")
        override var apiVersion: AzureServiceVersion by enum(AzureServiceVersion.V2023_05_15)

        @get:OptionTag("MODEL")
        override var model: String? by string()

        @get:OptionTag("TTS_MODEL")
        override var ttsModel: String? by string()

        @get:OptionTag("TTS_VOICE")
        override var ttsVoice: OpenAiTtsVoice by enum(OpenAiTtsVoice.ALLOY)

        @get:OptionTag("TTS_SPEED")
        override var ttsSpeed: Float by property(1.0f)
    }
}
