package cn.yiiguxing.plugin.translate.documentation.actions

import cn.yiiguxing.intellij.compat.DocumentationRenderingCompat
import cn.yiiguxing.intellij.compat.instance
import cn.yiiguxing.plugin.translate.action.ToggleableTranslationAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.documentation.Documentations
import cn.yiiguxing.plugin.translate.documentation.isPsiInlineDocumentationTranslationEnabled
import cn.yiiguxing.plugin.translate.documentation.setPsiInlineDocumentationTranslationEnabled
import cn.yiiguxing.plugin.translate.util.getNextSiblingSkippingCondition
import cn.yiiguxing.plugin.translate.util.isWhitespace
import cn.yiiguxing.plugin.translate.util.startOffset
import com.intellij.codeInsight.documentation.render.DocRenderManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.AnimatedIcon
import icons.TranslationIcons

internal class TranslateRenderedDocAction(
    private val editor: Editor,
    private val renderedText: String
) : ToggleableTranslationAction() {

    override fun update(event: AnActionEvent, isSelected: Boolean) {
        val comment = getPsiFile(editor)?.let { getPsiDocComment(editor, it) }
        val isTranslationEnabled = comment?.let { isPsiInlineDocumentationTranslationEnabled(it) } ?: false
        with(event.presentation) {
            isEnabledAndVisible = comment != null
            icon = when {
                isSelected -> TranslationIcons.Translation
                isTranslationEnabled -> AnimatedIcon.Default.INSTANCE
                else -> TranslationIcons.TranslationInactivated
            }
            text = adaptedMessage(
                if (isSelected) "action.TranslateRenderedDocAction.text.original"
                else "action.TranslateRenderedDocAction.text"
            )
        }
    }

    override fun isSelected(event: AnActionEvent): Boolean {
        return Documentations.isTranslated(renderedText)
    }

    @Suppress("UnstableApiUsage")
    override fun setSelected(event: AnActionEvent, state: Boolean) {
        val psiFile = getPsiFile(editor) ?: return
        val comment = getPsiDocComment(editor, psiFile) ?: return

        val isDocRenderingEnabled = DocRenderManager.isDocRenderingEnabled(editor)
        if (!isDocRenderingEnabled) {
            DocRenderManager.setDocRenderingEnabled(editor, true)
        }

        //TODO 先翻译并缓存，再更新的时候从缓存中获取翻译结果更新
        setPsiInlineDocumentationTranslationEnabled(comment, state)
        DocumentationRenderingCompat.instance().update(editor, psiFile)
    }


    companion object {
        private fun getPsiFile(editor: Editor): PsiFile? {
            val psiDocumentManager = PsiDocumentManager.getInstance(editor.project ?: return null)
            return psiDocumentManager.getPsiFile(editor.document)
        }

        private fun getPsiDocComment(editor: Editor, psiFile: PsiFile): PsiDocCommentBase? {
            val editor = editor.takeUnless { it.isDisposed } ?: return null
            val offset = editor.caretModel.offset
            var element = psiFile.findElementAt(offset) ?: return null
            if (element.isWhitespace) {
                element = element.getNextSiblingSkippingCondition(PsiElement::isWhitespace) ?: return null
            }
            element = psiFile.findElementAt(element.startOffset) ?: return null

            return PsiTreeUtil.getParentOfType(element, PsiDocCommentBase::class.java, false)
        }
    }
}