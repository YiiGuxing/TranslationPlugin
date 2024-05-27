package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.intellij.compat.action.UpdateInBackgroundCompatAction
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

abstract class PsiElementTranslateAction : UpdateInBackgroundCompatAction() {

    final override fun update(e: AnActionEvent) {
        val editor = e.editor
        val psiFile = e.psiFile

        e.presentation.isEnabledAndVisible = if (editor != null && psiFile != null && psiFile.isValid) {
            val dataContext = e.dataContext
            val element = try {
                pickPsiElement(editor, psiFile, dataContext)
            } catch (e: Throwable) {
                LOG.w("Failed to pick PSI element", e)
                null
            }
            element?.let { it.isValid && isEnabledForElement(editor, it, dataContext) } ?: false
        } else {
            false
        }
    }

    protected open fun pickPsiElement(editor: Editor, psiFile: PsiFile, dataContext: DataContext): PsiElement? {
        return dataContext.getData(LangDataKeys.PSI_ELEMENT)
    }

    protected abstract fun isEnabledForElement(editor: Editor, element: PsiElement, dataContext: DataContext): Boolean

    final override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor ?: return
        val psiFile = e.psiFile ?: return
        val dataContext = e.dataContext
        val element = try {
            pickPsiElement(editor, psiFile, dataContext) ?: return
        } catch (e: Throwable) {
            LOG.w("Failed to pick PSI element", e)
            return
        }

        doTranslate(editor, element, dataContext)
    }

    protected abstract fun doTranslate(editor: Editor, element: PsiElement, dataContext: DataContext)

    companion object {
        private val LOG = Logger.getInstance(PsiElementTranslateAction::class.java)

        private val AnActionEvent.editor: Editor?
            get() = getData(CommonDataKeys.EDITOR)

        private val AnActionEvent.psiFile: PsiFile?
            get() = getData(LangDataKeys.PSI_FILE)
    }
}