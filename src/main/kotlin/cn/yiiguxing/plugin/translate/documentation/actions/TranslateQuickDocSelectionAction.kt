package cn.yiiguxing.plugin.translate.documentation.actions

import cn.yiiguxing.plugin.translate.action.ACTION_HIGH_PRIORITY
import cn.yiiguxing.plugin.translate.action.ImportantTranslationAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.processBeforeTranslate
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
        templatePresentation.icon = TranslationIcons.Translation
        templatePresentation.text = adaptedMessage("action.TranslateQuickDocSelectionAction.text")
        templatePresentation.description = message("action.TranslateQuickDocSelectionAction.description")
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        //don't show in toolbar, invoke via context menu or with keyboard shortcut
        //to not clash with "Translate documentation" toggle
        e.presentation.isEnabledAndVisible = hasQuickDocSelection(e) && !e.isFromActionToolbar
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
        private const val SELECTED_QUICK_DOC_TEXT = "QUICK_DOC.SELECTED_TEXT"

        fun getSelectedQuickDocText(e: AnActionEvent): String? =
            (e.dataContext.getData(SELECTED_QUICK_DOC_TEXT) as? String)?.takeIf { it.isNotBlank() }

        fun hasQuickDocSelection(e: AnActionEvent): Boolean = getSelectedQuickDocText(e) != null
    }
}