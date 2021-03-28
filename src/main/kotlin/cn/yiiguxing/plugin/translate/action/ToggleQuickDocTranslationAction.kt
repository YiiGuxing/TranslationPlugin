package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.documentation.TranslateDocumentationTask
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.invokeLater
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.documentation.QuickDocUtil
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.concurrency.AppExecutorUtil
import icons.Icons

open class ToggleQuickDocTranslationAction :
    ToggleAction({ adaptedMessage("settings.options.translate.documentation") }, Icons.Translation),
    HintManagerImpl.ActionToIgnore {

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val docComponentExists = QuickDocUtil.getActiveDocComponent(project) != null
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DOCUMENTATION)

        e.presentation.isEnabled = docComponentExists && (toolWindow == null || toolWindow.isActive)
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        return Settings.translateDocumentation
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        Settings.translateDocumentation = state

        val project = e.project ?: return
        val activeDocComponent = QuickDocUtil.getActiveDocComponent(project) ?: return
        toggleTranslation(project, activeDocComponent)
    }

    private fun toggleTranslation(project: Project, docComponent: DocumentationComponent) {
        val currentText = docComponent.text

        if (Settings.translateDocumentation) {
            TranslateDocumentationTask(currentText, docComponent.element?.language).onSuccess { translation ->
                replaceActiveComponentText(project, currentText, translation)
            }
        } else {
            val element = docComponent.element ?: return
            val originalElement = DocumentationManager.getOriginalElement(element)

            val now = System.currentTimeMillis()
            val replaceComponentAction = ReadAction.nonBlocking {
                if (element.isValid && originalElement?.isValid != false) {
                    val originalText = DocumentationManager.getInstance(project)
                        .generateDocumentation(element, originalElement, false)

                    replaceActiveComponentText(project, currentText, originalText)
                }
            }
            replaceComponentAction
                .expireWhen { System.currentTimeMillis() - now > 5000 }
                .submit(AppExecutorUtil.getAppExecutorService())
        }
    }

    private fun replaceActiveComponentText(
        project: Project,
        currentText: String?,
        newText: String
    ) {
        invokeLater {
            val component = QuickDocUtil.getActiveDocComponent(project)
            if (component?.text == currentText) {
                component?.replaceText(newText, component.element)
            }
        }
    }

}

private class ToggleQuickDocTranslationActionWithShortcut : ToggleQuickDocTranslationAction() {
    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val activeDocComponent = QuickDocUtil.getActiveDocComponent(project)
        val docComponentExists = activeDocComponent != null
        val hasSelection = TranslateQuickDocSelectionAction.quickDocHasSelection(e)

        val toolWindowIsActiveIfShown =
            ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DOCUMENTATION)?.isActive ?: true

        e.presentation.isEnabled = docComponentExists && !hasSelection && toolWindowIsActiveIfShown
    }

}