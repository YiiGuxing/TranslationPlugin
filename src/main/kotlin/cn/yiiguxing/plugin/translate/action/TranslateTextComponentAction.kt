package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.canPreSelectFromCurrentCaret
import cn.yiiguxing.plugin.translate.util.getSelectionFromCurrentCaret
import cn.yiiguxing.plugin.translate.util.processBeforeTranslate
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actions.TextComponentEditorAction
import com.intellij.openapi.project.DumbAware

/**
 * 文本组件（如文本框、提示气泡、输入框……）翻译
 */
class TranslateTextComponentAction :
    TextComponentEditorAction(Handler()),
    HintManagerImpl.ActionToIgnore,
    ImportantTranslationAction,
    DumbAware {

    init {
        isEnabledInModalContext = true
        templatePresentation.text = adaptedMessage("action.TranslateTextComponent.text")
        templatePresentation.description = message("action.description.textComponent")
    }

    private class Handler : EditorActionHandler() {

        private val settings = Settings

        public override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
            val text = when {
                editor.selectionModel.hasSelection() -> editor.selectionModel.selectedText
                !editor.isViewer -> editor.getSelectionFromCurrentCaret(settings.autoSelectionMode)?.let {
                    editor.document.getText(it)
                }

                else -> null
            }

            text?.processBeforeTranslate()?.let { TranslationUIManager.showDialog(editor.project).translate(it) }
        }

        public override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?) =
            when {
                editor.selectionModel.hasSelection() -> !editor.selectionModel.selectedText.isNullOrBlank()
                !editor.isViewer -> editor.canPreSelectFromCurrentCaret()
                else -> false
            }
    }

}
