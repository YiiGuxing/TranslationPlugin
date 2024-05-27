package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.intellij.compat.DocumentationRenderingCompat
import cn.yiiguxing.intellij.compat.instance
import cn.yiiguxing.plugin.translate.action.FixedIconToggleAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.util.concurrent.finishOnUiThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocCommentBase
import icons.TranslationIcons

internal class TranslateRenderedDocAction(
    private val editor: Editor,
    private val docComment: PsiDocCommentBase
) : FixedIconToggleAction(
    TranslationIcons.Documentation,
    { adaptedMessage("action.TranslateRenderedDocAction.text") }
) {

    private val isEnabled: Boolean by lazy { DocTranslationService.isSupportedForPsiElement(docComment) }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = isEnabled
    }

    override fun isSelected(event: AnActionEvent): Boolean {
        return DocTranslationService.isInlayDocTranslated(docComment)
    }

    override fun setSelected(event: AnActionEvent, value: Boolean) {
        val editor = editor.takeUnless { it.isDisposed } ?: return
        val file = docComment.containingFile ?: return

        DocTranslationService.setInlayDocTranslated(docComment, value)
        DocumentationRenderingCompat
            .instance()
            .update(editor, file)
            .finishOnUiThread(ModalityState.current()) {
                if (it != true) {
                    DocTranslationService.setInlayDocTranslated(docComment, !value)
                    Toggleable.setSelected(event.presentation, !value)
                }
            }
    }
}
