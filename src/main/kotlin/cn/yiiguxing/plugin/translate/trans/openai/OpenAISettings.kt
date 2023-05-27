package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.TranslationStorages
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag


/**
 * OpenAI settings.
 */
@Service
@State(name = "Translation.OpenAISettings", storages = [Storage(TranslationStorages.PREFERENCES_STORAGE_NAME)])
class OpenAISettings : BaseState(), PersistentStateComponent<OpenAISettings> {

    @get:OptionTag("MODEL")
    var model: OpenAIModel by enum(OpenAIModel.GPT_3_5_TURBO)

    override fun getState(): OpenAISettings = this

    override fun loadState(state: OpenAISettings) {
        copyFrom(state)
    }
}