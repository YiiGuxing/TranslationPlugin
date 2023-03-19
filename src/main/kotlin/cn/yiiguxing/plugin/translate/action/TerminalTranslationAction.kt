package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.intellij.compat.action.UpdateInBackgroundCompatAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.terminal.JBTerminalWidget

class TerminalTranslationAction :
    UpdateInBackgroundCompatAction({ adaptedMessage("action.TerminalTranslationAction.text") }),
    ImportantTranslationAction,
    DumbAware {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = !e.getSelectedTextFromTerminal().isNullOrBlank()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedText = e.getSelectedTextFromTerminal() ?: return
        TranslationUIManager.showDialog(e.project).translate(selectedText)
    }

    companion object {
        fun AnActionEvent.getSelectedTextFromTerminal(): String? = getData(JBTerminalWidget.SELECTED_TEXT_DATA_KEY)
    }
}