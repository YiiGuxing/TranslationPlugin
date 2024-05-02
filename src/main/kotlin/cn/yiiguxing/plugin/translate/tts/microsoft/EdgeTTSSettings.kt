package cn.yiiguxing.plugin.translate.tts.microsoft

import cn.yiiguxing.plugin.translate.TranslationStorages
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag

/**
 * The settings of the Microsoft Edge TTS.
 */
@Service
@State(name = "Translation.EdgeTTSSettings", storages = [Storage(TranslationStorages.PREFERENCES_STORAGE_NAME)])
class EdgeTTSSettings : PersistentStateComponentWithModificationTracker<EdgeTTSSettings.State> {

    private var state = State()

    /** The voice to use for synthesis. */
    var voice: String?
        get() = state.voice
        set(value) {
            state.voice = value
        }

    /** The speed of the speech synthesis. */
    var speed: Int
        get() = state.speed
        set(value) {
            state.speed = value
        }

    override fun getState(): State = state

    override fun getStateModificationCount(): Long = state.modificationCount

    override fun loadState(state: State) {
        this.state = state
    }

    /** The state of the Microsoft Edge TTS settings. */
    class State : BaseState() {
        /** The voice to use for synthesis. */
        @get:OptionTag("VOICE")
        var voice: String? by string()

        /** The speed of the speech synthesis. */
        @get:OptionTag("SPEED")
        var speed: Int by property(EDGE_TTS_NORMAL_SPEED)
    }

    companion object {
        /** Returns the instance of [EdgeTTSSettings]. */
        fun instance(): EdgeTTSSettings = service()
    }
}