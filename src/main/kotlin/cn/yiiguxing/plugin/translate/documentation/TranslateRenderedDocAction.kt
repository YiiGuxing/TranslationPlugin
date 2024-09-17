package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.intellij.compat.DocumentationRenderingCompat
import cn.yiiguxing.intellij.compat.instance
import cn.yiiguxing.plugin.translate.action.ToggleableTranslationAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.util.concurrent.finishOnUiThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocCommentBase

internal class TranslateRenderedDocAction(
    private val editor: Editor,
    private val docComment: PsiDocCommentBase
) : ToggleableTranslationAction() {

    private val isEnabled: Boolean by lazy { DocTranslationService.isSupportedForPsiElement(docComment) }

    override fun update(event: AnActionEvent, isSelected: Boolean) {
        val presentation = event.presentation
        presentation.isEnabledAndVisible = isEnabled
        presentation.text = adaptedMessage(
            if (isSelected) "action.TranslateRenderedDocAction.text.original"
            else "action.TranslateRenderedDocAction.text"
        )
    }

    override fun isSelected(event: AnActionEvent): Boolean {
        return DocTranslationService.isInlayDocTranslated(docComment)
    }

    override fun setSelected(event: AnActionEvent, state: Boolean) {
        val editor = editor.takeUnless { it.isDisposed } ?: return
        val file = docComment.containingFile ?: return

        DocTranslationService.setInlayDocTranslated(docComment, state)
        DocumentationRenderingCompat
            .instance()
            .update(editor, file)
            .finishOnUiThread(ModalityState.current()) {
                if (it != true) {
                    DocTranslationService.setInlayDocTranslated(docComment, !state)
                    event.presentation.isTranslationSelected = !state
                }
            }
    }
}
