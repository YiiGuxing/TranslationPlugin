package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.canPreSelectFromCurrentCaret
import cn.yiiguxing.plugin.translate.util.getSelectionFromCurrentCaret
import cn.yiiguxing.plugin.translate.util.invokeLater
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
    }

    private class Handler : EditorActionHandler() {

        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
            val text = when {
                editor.selectionModel.hasSelection() -> editor.selectionModel.selectedText
                !editor.isViewer -> editor.getSelectionFromCurrentCaret(Settings.getInstance().autoSelectionMode)?.let {
                    editor.document.getText(it)
                }

                else -> null
            }

            val project = editor.project
            text?.processBeforeTranslate()?.let {
                // Defer showing the translation dialog until the next AWT event dispatch.
                // This prevents focus issues that can occur when this action is executed
                // from a popup window, which may cause the dialog to fail to close when
                // pressing ESC.
                invokeLater {
                    if (project?.isDisposed != true) {
                        TranslationUIManager.showDialog(project).translate(it)
                    }
                }
            }
        }

        override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?) =
            when {
                editor.selectionModel.hasSelection() -> !editor.selectionModel.selectedText.isNullOrBlank()
                !editor.isViewer -> editor.canPreSelectFromCurrentCaret()
                else -> false
            }
    }

}
