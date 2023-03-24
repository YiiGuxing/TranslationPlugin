package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.intellij.compat.action.UpdateInBackgroundCompatAction
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.Settings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Condition

/**
 * Translation Engine Action
 */
class TranslationEngineAction(private val translator: TranslationEngine) :
    UpdateInBackgroundCompatAction(translator.translatorName, null, translator.icon), DumbAware {

    fun isAvailable(): Boolean = translator.isConfigured() || Settings.translator == translator

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = isAvailable()
    }

    override fun actionPerformed(e: AnActionEvent) {
        Settings.translator = translator
    }

    companion object {
        private val ACTIONS: List<TranslationEngineAction> =
            TranslationEngine.values().toList().map { TranslationEngineAction(it) }

        fun availableActions(): List<TranslationEngineAction> = ACTIONS.filter { it.isAvailable() }

        fun unavailableActions(): List<TranslationEngineAction> = ACTIONS.filterNot { it.isAvailable() }

        val PRESELECT_CONDITION: Condition<AnAction> = Condition { action ->
            (action as? TranslationEngineAction)?.translator == Settings.translator
        }
    }
}