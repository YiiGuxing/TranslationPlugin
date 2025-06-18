@file:Suppress("UnstableApiUsage", "DEPRECATION")

package cn.yiiguxing.plugin.translate.documentation.actions

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.action.ImportantTranslationAction
import cn.yiiguxing.plugin.translate.action.ToggleableTranslationAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.documentation.DocTranslationService
import cn.yiiguxing.plugin.translate.documentation.TranslatableDocumentationTarget
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.util.invokeLater
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.documentation.QuickDocUtil
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.EditorMouseHoverPopupManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.ide.documentation.DOCUMENTATION_BROWSER
import com.intellij.platform.ide.documentation.DocumentationBrowserFacade
import com.intellij.util.concurrency.AppExecutorUtil

open class ToggleQuickDocTranslationAction :
    ToggleableTranslationAction(),
    HintManagerImpl.ActionToIgnore,
    ImportantTranslationAction {

    override val priority: Int = 1

    init {
        // Enable in hovering documentation popup
        isEnabledInModalContext = true
        templatePresentation.text = adaptedMessage("action.ToggleQuickDocTranslationAction.text")
    }

    private fun documentationBrowser(dc: DataContext): DocumentationBrowserFacade? = dc.getData(DOCUMENTATION_BROWSER)

    final override fun update(event: AnActionEvent, isSelected: Boolean) {
        val presentation = event.presentation
        presentation.text = adaptedMessage(
            if (isSelected) "action.ToggleQuickDocTranslationAction.text.original"
            else "action.ToggleQuickDocTranslationAction.text"
        )

        val project = event.project
        if (project == null) {
            presentation.isEnabled = false
            return
        }

        if (DocTranslationService.isDocumentationV2) {
            presentation.isEnabledAndVisible = documentationBrowser(event.dataContext)
                ?.targetPointer
                ?.dereference() is TranslatableDocumentationTarget
            return
        }

        update(project, event)
    }

    private fun update(project: Project, e: AnActionEvent) {
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
        val isDocMenuPlace = e.place == ActionPlaces.JAVADOC_TOOLBAR || e.place == "documentation.pane.content.menu"
        e.presentation.isEnabled =
            activeDocComponent != null && (isDocMenuPlace || toolWindow == null || toolWindow.isActive)
    }

    override fun isSelected(event: AnActionEvent): Boolean {
        val project = event.project ?: return false
        val state = if (DocTranslationService.isDocumentationV2) {
            documentationBrowser(event.dataContext)
                ?.targetPointer
                ?.dereference()
                ?.let { it as? TranslatableDocumentationTarget }
                ?.translate
                ?: false
        } else {
            QuickDocUtil.getActiveDocComponent(project)?.element?.let {
                DocTranslationService.getTranslationState(it)
            } ?: Settings.getInstance().translateDocumentation
        }

        return state
    }

    override fun setSelected(event: AnActionEvent, state: Boolean) {
        val project = event.project ?: return
        if (DocTranslationService.isDocumentationV2) {
            documentationBrowser(event.dataContext)?.let { browserFacade ->
                val target = browserFacade.targetPointer.dereference() as? TranslatableDocumentationTarget
                    ?: return

                target.translate = state
                browserFacade.reload()
            }
        } else {
            toggleTranslation(project, state)
        }
    }

    private fun toggleTranslation(project: Project, state: Boolean) {
        val docComponent = QuickDocUtil.getActiveDocComponent(project) ?: return
        val element = docComponent.element ?: return
        val currentText = docComponent.text
        val originalElement = DocumentationManager.getOriginalElement(element)

        DocTranslationService.setTranslationState(element, state)

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
        invokeLater {
            val component = QuickDocUtil.getActiveDocComponent(project)
            if (component?.text == currentText) {
                component?.replaceText(newText, component.element)
            }
        }
    }
}