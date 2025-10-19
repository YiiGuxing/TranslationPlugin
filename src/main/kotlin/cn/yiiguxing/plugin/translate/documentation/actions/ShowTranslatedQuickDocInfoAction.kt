package cn.yiiguxing.plugin.translate.documentation.actions

import cn.yiiguxing.plugin.translate.action.ImportantTranslationAction
import cn.yiiguxing.plugin.translate.documentation.TranslatableDocumentationTarget
import com.intellij.codeInsight.hint.HintManagerImpl.ActionToIgnore
import com.intellij.lang.documentation.ide.impl.DocumentationManagementHelper
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.ide.documentation.DOCUMENTATION_TARGETS

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
        e.presentation.isEnabledAndVisible = isSupportedForDataContext(e.dataContext)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        val target = getTranslatableDocumentationTarget(dataContext) ?: return
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return

        target.translate = true
        showQuickDoc(project, editor, target)
    }

    private fun isSupportedForDataContext(dataContext: DataContext): Boolean {
        return dataContext.getData(DOCUMENTATION_TARGETS)?.firstOrNull() is TranslatableDocumentationTarget
    }

    private fun getTranslatableDocumentationTarget(dataContext: DataContext): TranslatableDocumentationTarget? {
        return dataContext.getData(DOCUMENTATION_TARGETS)?.firstOrNull() as? TranslatableDocumentationTarget
    }
}

@Suppress("UnstableApiUsage")
private val showQuickDoc: (Project, Editor, DocumentationTarget) -> Unit by lazy {
    // The showQuickDoc method added a new parameter in version 2025.3,
    // so reflection is required for compatibility.
    val method = DocumentationManagementHelper::class.java.methods.firstOrNull { it.name == "showQuickDoc" }
    return@lazy { project, editor, target ->
        method ?: throw NoSuchMethodException("Method showQuickDoc not found in DocumentationManagementHelper")

        val helper = DocumentationManagementHelper.getInstance(project)
        val args = Array<Any?>(method.parameterCount) { null }
        args[0] = editor
        args[1] = target
        method.isAccessible = true
        try {
            method.invoke(helper, *args)
        } finally {
            method.isAccessible = false
        }
    }
}