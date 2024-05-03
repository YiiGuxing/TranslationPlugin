package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.tts.TTSEngine
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class TTSEngineAction(private val engine: TTSEngine) : FixedIconToggleAction(engine.icon, engine.ttsName), DumbAware {

    private val settings: Settings = Settings.getInstance()

    fun isAvailable(): Boolean = engine == settings.ttsEngine || try {
        engine.isConfigured()
    } catch (e: Throwable) {
        false
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = isAvailable()
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        return engine == settings.ttsEngine
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (state) {
            settings.ttsEngine = engine
        }
    }

    companion object {
        fun actions(): List<TTSEngineAction> = TTSEngine.values().toList().map { engine ->
            TTSEngineAction(engine)
        }
    }
}