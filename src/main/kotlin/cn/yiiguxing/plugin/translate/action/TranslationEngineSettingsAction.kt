package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class TranslationEngineSettingsAction : AnAction() {

    companion object {
        const val ACTION_ID = "Translation.TranslationEngineSettingsAction"
    }

    private val settings: Settings by lazy { service<Settings>() }
    private val currentTranslationEngine: TranslationEngine get() = settings.translator

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.apply {
            isEnabledAndVisible = currentTranslationEngine.hasConfiguration
            text = message(
                "action.TranslationEngineSettingsAction.text",
                currentTranslationEngine.translatorName
            )
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        currentTranslationEngine.showConfigurationDialog()
    }
}