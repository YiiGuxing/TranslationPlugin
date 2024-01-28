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
class OpenAISettings : BaseState(), PersistentStateComponent<OpenAISettings> {

    @get:OptionTag("PROVIDER")
    var provider: ServiceProvider by enum(ServiceProvider.OpenAI)

    @get:OptionTag("OPEN_AI")
    var openAi: OpenAI by property(OpenAI())

    @get:OptionTag("AZURE")
    var azure: Azure by property(Azure())

    @get:Transient
    val model: OpenAIModel
        get() = when (provider) {
            ServiceProvider.OpenAI -> openAi.model
            ServiceProvider.Azure -> azure.model
        }

    override fun getState(): OpenAISettings = this

    override fun loadState(state: OpenAISettings) {
        copyFrom(state)
    }

    @Tag("open-ai")
    open class OpenAI : BaseState(), OpenAIService.Options {
        @get:OptionTag("MODEL")
        override var model: OpenAIModel by enum(OpenAIModel.GPT_3_5_TURBO)

        @get:OptionTag("ENDPOINT")
        override var endpoint: String? by string()
    }

    @Tag("azure")
    class Azure : OpenAI(), OpenAIService.AzureOptions {
        @get:OptionTag("API_VERSION")
        override var apiVersion: AzureServiceVersion by enum(AzureServiceVersion.V2023_05_15)
    }
}
