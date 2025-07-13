package cn.yiiguxing.plugin.translate.documentation.actions

import cn.yiiguxing.plugin.translate.action.ToggleableTranslationAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.documentation.Documentations
import cn.yiiguxing.plugin.translate.documentation.TranslatableInlineDocumentation
import cn.yiiguxing.plugin.translate.util.getNextSiblingSkippingCondition
import cn.yiiguxing.plugin.translate.util.isWhitespace
import cn.yiiguxing.plugin.translate.util.startOffset
import com.intellij.codeInsight.documentation.render.DocRenderPassFactory
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.AppExecutorUtil

@Suppress("UnstableApiUsage")
internal class TranslateRenderedDocAction(
    private val editor: Editor,
    private val renderedText: String
) : ToggleableTranslationAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(event: AnActionEvent, isSelected: Boolean) {
        val presentation = event.presentation
        // FIXME
        presentation.isEnabledAndVisible = true
        presentation.text = adaptedMessage(
            if (isSelected) "action.TranslateRenderedDocAction.text.original"
            else "action.TranslateRenderedDocAction.text"
        )
    }

    override fun isSelected(event: AnActionEvent): Boolean {
        return Documentations.isTranslated(renderedText)
    }

    override fun setSelected(event: AnActionEvent, state: Boolean) {
        val editor = editor.takeUnless { it.isDisposed } ?: return
        val psiDocumentManager = PsiDocumentManager.getInstance(editor.project ?: return)
        val psiFile = psiDocumentManager.getPsiFile(editor.document) ?: return

        val offset = editor.caretModel.offset
        var element = psiFile.findElementAt(offset) ?: return
        if (element.isWhitespace) {
            element = element.getNextSiblingSkippingCondition(PsiElement::isWhitespace) ?: return
        }
        element = psiFile.findElementAt(element.startOffset) ?: return

        val comment = PsiTreeUtil.getParentOfType(element, PsiDocCommentBase::class.java, false)
        if (comment == null) {
            return
        }

        comment.putUserData(TranslatableInlineDocumentation.TRANSLATION_FLAG_KEY, state)

        ReadAction.nonBlocking<DocRenderPassFactory.Items?> {
            try {
                DocRenderPassFactory.calculateItemsToRender(editor, psiFile).apply {
                    ProgressManager.checkCanceled()
                }
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Throwable) {
                LOG.warn("Failed to calculate doc items to render", e)
                null
            }
        }
            .expireWhen { !editor.isValid }
            .withDocumentsCommitted(psiFile.project)
            .finishOnUiThread(ModalityState.any()) { items ->
                if (items != null) {
                    DocRenderPassFactory.applyItemsToRender(editor, psiFile.project, items, false)
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    companion object {
        private val LOG = logger<TranslateRenderedDocAction>()

        private val Editor.isValid: Boolean
            get() {
                val editorProject = project
                return editorProject != null
                        && !editorProject.isDisposed
                        && !isDisposed
            }
    }
}