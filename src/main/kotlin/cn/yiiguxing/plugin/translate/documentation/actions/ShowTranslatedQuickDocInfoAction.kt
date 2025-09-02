package cn.yiiguxing.plugin.translate.documentation.actions

import cn.yiiguxing.plugin.translate.action.ImportantTranslationAction
import cn.yiiguxing.plugin.translate.documentation.DocTranslationService
import com.intellij.codeInsight.hint.HintManagerImpl.ActionToIgnore
import com.intellij.lang.documentation.ide.impl.DocumentationManagementHelper
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware

class ShowTranslatedQuickDocInfoAction : AnAction(),
    ActionToIgnore,
    DumbAware,
    PopupAction,
    PerformWithDocumentsCommitted,
    ImportantTranslationAction {

    init {
        isEnabledInModalContext = true
        setInjectedContext(true)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = DocTranslationService.isSupportedForDataContext(e.dataContext)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        val target = DocTranslationService.getTranslatableDocumentationTarget(dataContext) ?: return
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return

        target.translate = true
        @Suppress("UnstableApiUsage")
        project.service<DocumentationManagementHelper>().showQuickDoc(editor, target)
    }
}