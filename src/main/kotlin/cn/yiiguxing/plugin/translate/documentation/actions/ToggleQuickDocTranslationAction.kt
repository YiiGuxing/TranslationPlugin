@file:Suppress("UnstableApiUsage", "DEPRECATION")

package cn.yiiguxing.plugin.translate.documentation.actions

import cn.yiiguxing.plugin.translate.action.ImportantTranslationAction
import cn.yiiguxing.plugin.translate.action.ToggleableTranslationAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.documentation.TranslatableDocumentationTarget
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.platform.ide.documentation.DOCUMENTATION_BROWSER
import com.intellij.platform.ide.documentation.DocumentationBrowserFacade

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

        presentation.isEnabledAndVisible = documentationBrowser(event.dataContext)
            ?.targetPointer
            ?.dereference() is TranslatableDocumentationTarget
    }

    override fun isSelected(event: AnActionEvent): Boolean {
        return documentationBrowser(event.dataContext)
            ?.targetPointer
            ?.dereference()
            ?.let { it as? TranslatableDocumentationTarget }
            ?.translate
            ?: false
    }

    override fun setSelected(event: AnActionEvent, state: Boolean) {
        documentationBrowser(event.dataContext)?.let { browserFacade ->
            val target = browserFacade.targetPointer.dereference() as? TranslatableDocumentationTarget
                ?: return

            target.translate = state
            browserFacade.reload()
        }
    }
}