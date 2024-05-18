package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.documentation.DocTranslationService
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.util.invokeLater
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.documentation.QuickDocUtil
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.EditorMouseHoverPopupManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.concurrency.AppExecutorUtil
import icons.TranslationIcons
import javax.swing.Icon

open class ToggleQuickDocTranslationAction :
    FixedIconToggleAction(
        TranslationIcons.Documentation,
        { adaptedMessage("action.ToggleQuickDocTranslationAction.text") }
    ),
    HintManagerImpl.ActionToIgnore,
    ImportantTranslationAction {

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val activeDocComponent = QuickDocUtil.getActiveDocComponent(project)
        val editorMouseHoverPopupManager = EditorMouseHoverPopupManager.getInstance()
        val rdMouseHoverDocComponent = editorMouseHoverPopupManager.documentationComponent
            .takeIf { IdeVersion.buildNumber.productCode == "RD" }
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DOCUMENTATION)

        e.presentation.isVisible = e.presentation.isVisible && rdMouseHoverDocComponent == null
                && activeDocComponent?.element.let { it != null && DocTranslationService.isSupportedForPsiElement(it) }

        // 当Action在ToolWindow的右键菜单上时，点击菜单项会使得ToolWindow失去焦点，
        // 此时toolWindow.isActive为false，Action将不启用。
        // 所以Action在右键菜单上时，直接设为启用状态。
        val isDocToolbarPlace = ActionPlaces.JAVADOC_TOOLBAR == e.place
        e.presentation.isEnabled =
            activeDocComponent != null && (isDocToolbarPlace || toolWindow == null || toolWindow.isActive)
    }

    override fun getIcon(place: String, selected: Boolean): Icon? {
        val isToolbarPlace = ActionPlaces.JAVADOC_TOOLBAR == place || ActionPlaces.TOOLWINDOW_TITLE == place
        return if (!isToolbarPlace && selected) null else icon
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        val activeDocComponent = QuickDocUtil.getActiveDocComponent(project) ?: return false

        return activeDocComponent.element?.let { DocTranslationService.getTranslationState(it) }
            ?: Settings.getInstance().translateDocumentation
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: return
        val activeDocComponent = QuickDocUtil.getActiveDocComponent(project) ?: return
        val element = activeDocComponent.element ?: return

        DocTranslationService.setTranslationState(element, state)
        toggleTranslation(project, activeDocComponent)
    }

    private fun toggleTranslation(project: Project, docComponent: DocumentationComponent) {
        val currentText = docComponent.text
        val element = docComponent.element ?: return
        val originalElement = DocumentationManager.getOriginalElement(element)

        val now = System.currentTimeMillis()
        ReadAction.nonBlocking {
            if (element.isValid && originalElement?.isValid != false) {
                val originalText = DocumentationManager.getInstance(project)
                    .generateDocumentation(element, originalElement, false)
                if (originalText != null) {
                    replaceActiveComponentText(project, currentText, originalText)
                }
            }
        }.expireWhen {
            System.currentTimeMillis() - now > 5000
        }.expireWith(
            TranslationUIManager.disposable(project)
        ).submit(
            AppExecutorUtil.getAppExecutorService()
        )
    }

    private fun replaceActiveComponentText(project: Project, currentText: String?, newText: String) {
        invokeLater(expired = project.disposed) {
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
                && activeDocComponent?.element.let { it != null && DocTranslationService.isSupportedForPsiElement(it) }
    }
}