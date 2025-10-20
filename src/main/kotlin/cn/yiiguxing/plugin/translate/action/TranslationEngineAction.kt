package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Condition

/**
 * Translation Engine Action
 */
class TranslationEngineAction(private val translator: TranslationEngine) :
    AnAction(translator.translatorName, null, translator.icon), DumbAware {

    private val settings: Settings = Settings.getInstance()

    fun isAvailable(): Boolean = settings.translator == translator || try {
        translator.isConfigured()
    } catch (_: Throwable) {
        false
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

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
            return TranslationEngine.entries
                .map { TranslationEngineAction(it) }
                .groupBy { it.isAvailable() }
                .let { it.getOrDefault(true, emptyList()) to it.getOrDefault(false, emptyList()) }
        }

        val PRESELECT_CONDITION: Condition<AnAction> = Condition { action ->
            (action as? TranslationEngineAction)?.translator == Settings.getInstance().translator
        }
    }
}