package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.intellij.compat.action.UpdateInBackgroundCompatAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.processBeforeTranslate
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import icons.TranslationIcons

/**
 * Translate quick doc selection action
 */
class TranslateQuickDocSelectionAction :
    UpdateInBackgroundCompatAction(),
    DumbAware,
    ImportantTranslationAction,
    HintManagerImpl.ActionToIgnore {

    init {
        isEnabledInModalContext = true
        templatePresentation.text = adaptedMessage("action.TranslateQuickDocSelectionAction.text")
        templatePresentation.description = message("action.description.quickDoc")
    }

    override fun update(e: AnActionEvent) {
        //don't show in toolbar, invoke via context menu or with keyboard shortcut
        //to not clash with "Translate documentation" toggle
        e.presentation.isEnabledAndVisible = quickDocHasSelection(e) && !e.isFromActionToolbar
        e.presentation.icon = TranslationIcons.Translation
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.getData(DocumentationManager.SELECTED_QUICK_DOC_TEXT)
            ?.processBeforeTranslate()
            ?.let {
                e.project.let { project ->
                    TranslationUIManager.showDialog(project).translate(it)
                }
            }
    }

    companion object {
        fun quickDocHasSelection(e: AnActionEvent): Boolean =
            !e.getData(DocumentationManager.SELECTED_QUICK_DOC_TEXT).isNullOrBlank()
    }
}