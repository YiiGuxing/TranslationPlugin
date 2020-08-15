package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware

/**
 * 显示翻译对话框动作
 */
class ShowTranslationDialogAction : AnAction(), DumbAware {

    init {
        isEnabledInModalContext = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (!ApplicationManager.getApplication().isHeadlessEnvironment) {
            TranslationUIManager.showDialog(e.project)
        }
    }
}