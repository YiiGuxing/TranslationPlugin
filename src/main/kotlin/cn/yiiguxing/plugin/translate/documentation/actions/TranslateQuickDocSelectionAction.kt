package cn.yiiguxing.plugin.translate.documentation.actions

import cn.yiiguxing.plugin.translate.action.ACTION_HIGH_PRIORITY
import cn.yiiguxing.plugin.translate.action.ImportantTranslationAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.processBeforeTranslate
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import icons.TranslationIcons

/**
 * Translate quick doc selection action
 */
class TranslateQuickDocSelectionAction :
    AnAction(),
    DumbAware,
    ImportantTranslationAction,
    HintManagerImpl.ActionToIgnore {

    override val priority: Int = ACTION_HIGH_PRIORITY

    init {
        isEnabledInModalContext = true
        templatePresentation.text = adaptedMessage("action.TranslateQuickDocSelectionAction.text")
        templatePresentation.description = message("action.description.quickDoc")
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        //don't show in toolbar, invoke via context menu or with keyboard shortcut
        //to not clash with "Translate documentation" toggle
        e.presentation.isEnabledAndVisible = hasQuickDocSelection(e) && !e.isFromActionToolbar
        e.presentation.icon = TranslationIcons.Translation
    }

    override fun actionPerformed(e: AnActionEvent) {
        getSelectedQuickDocText(e)
            ?.processBeforeTranslate()
            ?.let {
                e.project.let { project ->
                    TranslationUIManager.showDialog(project).translate(it)
                }
            }
    }

    companion object {
        fun getSelectedQuickDocText(e: AnActionEvent): String? =
            e.dataContext.getData(DocumentationManager.SELECTED_QUICK_DOC_TEXT)?.takeIf { it.isNotBlank() }

        fun hasQuickDocSelection(e: AnActionEvent): Boolean = getSelectedQuickDocText(e) != null
    }
}