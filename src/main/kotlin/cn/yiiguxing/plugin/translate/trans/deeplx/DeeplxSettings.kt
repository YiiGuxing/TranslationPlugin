package cn.yiiguxing.plugin.translate.trans.deeplx

import cn.yiiguxing.plugin.translate.TranslationStorages
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag


/**
 * OpenAI settings.
 */
@Service
@State(name = "Translation.DdeeplxSettings", storages = [Storage(TranslationStorages.PREFERENCES_STORAGE_NAME)])
class DeeplxSettings : BaseState(), PersistentStateComponent<DeeplxSettings> {

    @get:OptionTag("API_ENDPOINT")
    var apiEndpoint: String? by string()

    override fun getState(): DeeplxSettings = this

    override fun loadState(state: DeeplxSettings) {
        copyFrom(state)
    }
}
