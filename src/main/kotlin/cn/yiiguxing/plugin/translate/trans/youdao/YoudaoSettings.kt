package cn.yiiguxing.plugin.translate.trans.youdao

import cn.yiiguxing.plugin.translate.TranslationStorages
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag


/**
 * Google settings.
 */
@Service
@State(name = "Translation.YoudaoSettings", storages = [Storage(TranslationStorages.PREFERENCES_STORAGE_NAME)])
class YoudaoSettings : BaseState(), PersistentStateComponent<YoudaoSettings> {

    @get:OptionTag("DOMAIN")
    var domain: YoudaoDomain by enum(YoudaoDomain.GENERAL)

    override fun getState(): YoudaoSettings = this

    override fun loadState(state: YoudaoSettings) {
        copyFrom(state)
    }
}