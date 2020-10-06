package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.documentation.TranslateDocumentationTask
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.invokeLater
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.documentation.QuickDocUtil
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.concurrency.runAsync

class ToggleQuickDocTranslationAction : ToggleAction(), HintManagerImpl.ActionToIgnore {

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return

        val docComponentExists = QuickDocUtil.getActiveDocComponent(project) != null
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DOCUMENTATION)

        e.presentation.isEnabled = docComponentExists && (toolWindow == null || toolWindow.isActive)
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        return Settings.translateDocumentation
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        Settings.translateDocumentation = state

        val project = e.project!!
        val activeDocComponent = QuickDocUtil.getActiveDocComponent(project)!!

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

            runAsync {
                QuickDocUtil.runInReadActionWithWriteActionPriorityWithRetries({
                    if (element.isValid && originalElement?.isValid != false) {
                        val originalText = DocumentationManager.getInstance(project)
                            .generateDocumentation(element, originalElement, false)

                        replaceActiveComponentText(project, currentText, originalText)
                    }
                }, 5000, 100)
            }

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