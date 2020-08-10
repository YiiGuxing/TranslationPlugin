package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.ui.BalloonPositionTracker
import cn.yiiguxing.plugin.translate.util.SelectionMode
import cn.yiiguxing.plugin.translate.util.createCaretRangeMarker
import cn.yiiguxing.plugin.translate.util.processBeforeTranslate
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import java.util.*

/**
 * 翻译动作
 *
 * @param checkSelection 指定是否检查手动选择的文本。`true` - 如果有手动选择文本, 则忽略`autoSelectionMode`, `false` - 将忽略手动选择的文本。
 */
open class TranslateAction(checkSelection: Boolean = false) : AutoSelectAction(checkSelection), DumbAware {

    override val selectionMode
        get() = SelectionMode.INCLUSIVE

    override fun onActionPerformed(event: AnActionEvent, editor: Editor, selectionRange: TextRange) {
        val project = editor.project ?: return
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

        val startLine by lazy { editor.offsetToVisualPosition(selectionRange.startOffset).line }
        val endLine by lazy { editor.offsetToVisualPosition(selectionRange.endOffset).line }
        val highlightAttributes = if (starts.size > 1 || startLine == endLine) {
            HIGHLIGHT_ATTRIBUTES
        } else {
            MULTILINE_HIGHLIGHT_ATTRIBUTES
        }

        val highlightManager = HighlightManager.getInstance(project)
        val highlighters = ArrayList<RangeHighlighter>()
        for (i in starts.indices) {
            highlightManager.addRangeHighlight(editor, starts[i], ends[i], highlightAttributes, true, highlighters)
        }

        val caretRangeMarker = editor.createCaretRangeMarker(selectionRange)
        val tracker = BalloonPositionTracker(editor, caretRangeMarker)
        val balloon = TranslationUIManager.showBalloon(editor, text, tracker, Balloon.Position.below)

        if (highlighters.isNotEmpty()) {
            val disposable = Disposable {
                for (highlighter in highlighters) {
                    highlightManager.removeSegmentHighlighter(editor, highlighter)
                }
            }
            if (balloon.disposed) {
                disposable.dispose()
            } else {
                Disposer.register(balloon, disposable)
            }
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
    }
}
