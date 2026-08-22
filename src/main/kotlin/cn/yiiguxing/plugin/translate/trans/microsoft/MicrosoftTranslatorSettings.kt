package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.TranslationStorages
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag


/**
 * Microsoft translator settings.
 */
@Service
@State(name = "Translation.MicrosoftTranslatorSettings", storages = [Storage(TranslationStorages.PREFERENCES_STORAGE_NAME)])
class MicrosoftTranslatorSettings : BaseState(), PersistentStateComponent<MicrosoftTranslatorSettings> {

    /**
     * Whether to keep the original text after documentation translation.
     */
    @get:OptionTag("KEEP_ORIGINAL_DOCUMENTATION")
    var keepOriginalDocumentation: Boolean by property(false)

    override fun getState(): MicrosoftTranslatorSettings = this

    override fun loadState(state: MicrosoftTranslatorSettings) {
        copyFrom(state)
    }
}
