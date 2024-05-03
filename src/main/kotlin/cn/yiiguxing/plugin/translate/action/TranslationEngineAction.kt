package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Condition

/**
 * Translation Engine Action
 */
class TranslationEngineAction(private val translator: TranslationEngine) :
    DumbAwareAction(translator.translatorName, null, translator.icon) {

    private val settings: Settings = Settings.getInstance()

    fun isAvailable(): Boolean = settings.translator == translator || try {
        translator.isConfigured()
    } catch (e: Throwable) {
        false
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = isAvailable()
    }

    override fun actionPerformed(e: AnActionEvent) {
        settings.translator = translator
    }

    companion object {
        /**
         * Returns available - unavailable actions pair.
         */
        fun actionsGroupedByAvailability(): Pair<List<TranslationEngineAction>, List<TranslationEngineAction>> {
            return TranslationEngine.values().toList()
                .map { TranslationEngineAction(it) }
                .groupBy { it.isAvailable() }
                .let { it.getOrDefault(true, emptyList()) to it.getOrDefault(false, emptyList()) }
        }

        val PRESELECT_CONDITION: Condition<AnAction> = Condition { action ->
            (action as? TranslationEngineAction)?.translator == Settings.getInstance().translator
        }
    }
}