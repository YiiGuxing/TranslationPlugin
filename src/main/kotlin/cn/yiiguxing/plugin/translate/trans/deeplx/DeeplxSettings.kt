package cn.yiiguxing.plugin.translate.trans.deeplx

import cn.yiiguxing.plugin.translate.TranslationStorages
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag

@Service
@State(name = "Translation.DdeeplxSettings", storages = [Storage(TranslationStorages.PREFERENCES_STORAGE_NAME)])
class DdeeplxSettings : BaseState(), PersistentStateComponent<DdeeplxSettings> {

    @get:OptionTag("API_ENDPOINT")
    var apiEndpoint: String? by string()

    override fun getState(): DdeeplxSettings = this

    override fun loadState(state: DdeeplxSettings) {
        copyFrom(state)
    }
}