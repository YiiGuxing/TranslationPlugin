package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.processBeforeTranslate
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

/**
 * TranslateQuickDocAction
 */
class TranslateQuickDocAction : AnAction(), DumbAware, HintManagerImpl.ActionToIgnore {

    init {
        isEnabledInModalContext = true
        templatePresentation.description = message("action.description.quickDoc")
    }

    override fun update(e: AnActionEvent) {
        val selected = e.getData(DocumentationManager.SELECTED_QUICK_DOC_TEXT)
        e.presentation.isEnabled = !selected.isNullOrBlank()
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.getData(DocumentationManager.SELECTED_QUICK_DOC_TEXT)
                ?.processBeforeTranslate()
                ?.let {
                    e.project.let { project ->
                        project?.hideDocInfoHint()
                        TranslationUIManager.showDialog(project).translate(it)
                    }
                }
    }

    private companion object {
        fun Project.hideDocInfoHint() {
            DocumentationManager.getInstance(this).docInfoHint?.cancel()
        }
    }
}