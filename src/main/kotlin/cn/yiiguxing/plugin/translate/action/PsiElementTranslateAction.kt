package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.ui.BalloonPositionTracker
import cn.yiiguxing.plugin.translate.util.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.createCaretRangeMarker
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.JBColor
import java.util.*

abstract class PsiElementTranslateAction : AnAction() {

    final override fun update(e: AnActionEvent) {
        val editor = e.editor
        val psiFile = e.psiFile

        e.presentation.isEnabledAndVisible = if (editor != null && psiFile != null && psiFile.isValid) {
            val dataContext = e.dataContext
            val element = pickPsiElement(editor, psiFile, dataContext)
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
        val element = pickPsiElement(editor, psiFile, dataContext) ?: return
        val translateContent = getTranslateContent(editor, element, dataContext)?.takeIf { it.isNotEmpty() } ?: return

        doTranslate(editor, translateContent, element)
    }

    protected abstract fun getTranslateContent(editor: Editor, element: PsiElement, dataContext: DataContext): String?

    protected open fun doTranslate(editor: Editor, translateContent: String, element: PsiElement) {
        val highlightManager = editor.project?.let { HighlightManager.getInstance(it) } ?: return
        val highlighters = ArrayList<RangeHighlighter>()
        val range = element.textRange

        highlightManager.addRangeHighlight(editor, range.startOffset, range.endOffset, HIGHLIGHT, true, highlighters)

        val tracker = BalloonPositionTracker(editor, editor.createCaretRangeMarker(range))
        val balloon = TranslationUIManager.showBalloon(editor, translateContent, tracker, Balloon.Position.below)

        if (highlighters.isNotEmpty()) {
            Disposer.register(balloon, Disposable {
                for (highlighter in highlighters) {
                    highlightManager.removeSegmentHighlighter(editor, highlighter)
                }
            })
        }
    }

    companion object {
        private val HIGHLIGHT: TextAttributes = TextAttributes().apply {
            effectType = EffectType.BOXED
            effectColor = JBColor(0xFFEE6000.toInt(), 0xFFCC7832.toInt())
        }
        private val AnActionEvent.editor: Editor?
            get() = getData(CommonDataKeys.EDITOR)

        private val AnActionEvent.psiFile: PsiFile?
            get() = getData(LangDataKeys.PSI_FILE)
    }
}