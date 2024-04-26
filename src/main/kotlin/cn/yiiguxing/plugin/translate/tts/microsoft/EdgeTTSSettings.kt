package cn.yiiguxing.plugin.translate.tts.microsoft

import cn.yiiguxing.plugin.translate.TranslationStorages
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag

@Service
@State(name = "Translation.EdgeTTSSettings", storages = [Storage(TranslationStorages.PREFERENCES_STORAGE_NAME)])
class EdgeTTSSettings : BaseState(), PersistentStateComponent<EdgeTTSSettings> {

    @get:OptionTag("VOICE")
    var voice: String? by string()

    @get:OptionTag("SPEED")
    var speed: Int by property(EDGE_TTS_NORMAL_SPEED)

    override fun getState(): EdgeTTSSettings = this

    override fun loadState(state: EdgeTTSSettings) {
        copyFrom(state)
    }
}