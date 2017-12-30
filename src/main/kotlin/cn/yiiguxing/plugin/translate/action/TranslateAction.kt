package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.TranslationManager
import cn.yiiguxing.plugin.translate.util.SelectionMode
import cn.yiiguxing.plugin.translate.util.splitWord
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import java.awt.Color
import java.util.*

/**
 * 翻译动作
 *
 * @param checkSelection 指定是否检查手动选择的文本。`true` - 如果有手动选择文本, 则忽略`autoSelectionMode`, `false` - 将忽略手动选择的文本。
 */
open class TranslateAction(checkSelection: Boolean = false) : AutoSelectAction(checkSelection), DumbAware {

    override val selectionMode
        get() = SelectionMode.INCLUSIVE

    override fun onUpdate(e: AnActionEvent, active: Boolean) {
        e.presentation.isEnabledAndVisible = active
    }

    override fun onActionPerformed(e: AnActionEvent, editor: Editor, selectionRange: TextRange) {
        editor.document.getText(selectionRange).splitWord().takeIf { it.isNotBlank() }?.let { queryText ->
            val highlighters = editor.project?.let {
                ArrayList<RangeHighlighter>().apply {
                    HighlightManager.getInstance(it).addRangeHighlight(editor, selectionRange.startOffset,
                            selectionRange.endOffset, HIGHLIGHT_ATTRIBUTES, true, this)
                }
            }

            val caretRangeMarker = editor.createCaretRangeMarker(selectionRange)
            val balloon = TranslationManager.instance.showBalloon(editor, caretRangeMarker, queryText)

            highlighters?.takeIf { it.isNotEmpty() }?.let {
                Disposer.register(balloon, Disposable {
                    for (highlighter in it) {
                        highlighter.dispose()
                    }
                })
            }
        }
    }

    private companion object {
        val HIGHLIGHT_ATTRIBUTES: TextAttributes = TextAttributes().apply {
            backgroundColor = JBColor(Color(0xFFE4E4FF.toInt()), Color(0xFF344134.toInt()))
            effectType = EffectType.LINE_UNDERSCORE
            effectColor = JBColor(0xFFEE6000.toInt(), 0xFFCC7832.toInt())
        }

        fun Editor.createCaretRangeMarker(selectionRange: TextRange) = document.createRangeMarker(selectionRange)
                .apply {
                    isGreedyToLeft = true
                    isGreedyToRight = true
                }
    }
}
