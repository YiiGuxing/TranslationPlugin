package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.ui.BalloonPositionTracker
import cn.yiiguxing.plugin.translate.util.SelectionMode
import cn.yiiguxing.plugin.translate.util.createCaretRangeMarker
import cn.yiiguxing.plugin.translate.util.processBeforeTranslate
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PopupAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor

/**
 * 翻译动作
 *
 * @param checkSelection 指定是否检查手动选择的文本。`true` - 如果有手动选择文本, 则忽略`autoSelectionMode`, `false` - 将忽略手动选择的文本。
 */
open class TranslateAction(checkSelection: Boolean = false) :
    AutoSelectAction(checkSelection),
    PopupAction,
    DumbAware {

    override val selectionMode
        get() = SelectionMode.INCLUSIVE

    override fun onActionPerformed(event: AnActionEvent, editor: Editor, selectionRange: TextRange) {
        val project = editor.project ?: CommonDataKeys.PROJECT.getData(event.dataContext)
        val markupModel = editor.markupModel
        val selectionModel = editor.selectionModel
        val isColumnSelectionMode = editor.caretModel.caretCount > 1

        val text: String
        val starts: IntArray
        val ends: IntArray
        if (selectionModel.hasSelection(true) && isColumnSelectionMode) {
            starts = selectionModel.blockSelectionStarts
            ends = selectionModel.blockSelectionEnds
            text = selectionModel.getSelectedText(true)?.processBeforeTranslate() ?: return
        } else {
            starts = intArrayOf(selectionRange.startOffset)
            ends = intArrayOf(selectionRange.endOffset)
            text = editor.document.getText(selectionRange).processBeforeTranslate() ?: return
        }

        //this logic is also used in ShowTranslationDialogAction
        val currentNewTD = TranslationUIManager.instance(project).currentTranslationDialog()
        if (currentNewTD != null) {
            currentNewTD.translate(text)
            return
        }

        val startLine by lazy { editor.offsetToVisualPosition(selectionRange.startOffset).line }
        val endLine by lazy { editor.offsetToVisualPosition(selectionRange.endOffset).line }
        val highlightAttributes = if (starts.size > 1 || startLine == endLine) {
            HIGHLIGHT_ATTRIBUTES
        } else {
            MULTILINE_HIGHLIGHT_ATTRIBUTES
        }

        val highlighters = ArrayList<RangeHighlighter>(starts.size)
        try {
            for (i in starts.indices) {
                highlighters += markupModel.addRangeHighlighter(
                    starts[i],
                    ends[i],
                    HighlighterLayer.SELECTION - 1,
                    highlightAttributes,
                    HighlighterTargetArea.EXACT_RANGE
                )
            }

            editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)

            val caretRangeMarker = editor.createCaretRangeMarker(selectionRange)
            val tracker = BalloonPositionTracker(editor, caretRangeMarker)
            val balloon = TranslationUIManager.showBalloon(editor, text, tracker)

            if (balloon.disposed) {
                markupModel.removeHighlighters(highlighters)
            } else {
                Disposer.register(balloon) { markupModel.removeHighlighters(highlighters) }
            }
        } catch (thr: Throwable) {
            markupModel.removeHighlighters(highlighters)
        }
    }

    private companion object {
        val EFFECT_COLOR = JBColor(0xFFEE6000.toInt(), 0xFFCC7832.toInt())

        val HIGHLIGHT_ATTRIBUTES: TextAttributes = TextAttributes().apply {
            effectType = EffectType.LINE_UNDERSCORE
            effectColor = EFFECT_COLOR
        }

        val MULTILINE_HIGHLIGHT_ATTRIBUTES: TextAttributes = TextAttributes().apply {
            effectType = EffectType.BOXED
            effectColor = EFFECT_COLOR
        }

        private fun MarkupModel.removeHighlighters(highlighters: Collection<RangeHighlighter>) {
            for (highlighter in highlighters) {
                removeHighlighter(highlighter)
                highlighter.dispose()
            }
        }
    }
}
