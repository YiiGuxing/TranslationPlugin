package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument
import cn.yiiguxing.plugin.translate.ui.StyledViewer
import cn.yiiguxing.plugin.translate.util.chunked
import cn.yiiguxing.plugin.translate.util.text.*
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import icons.Icons
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractButton
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.plaf.basic.BasicButtonUI
import javax.swing.text.*

class GoogleExamplesDocument private constructor(private val examples: List<List<CharSequence>>) : TranslationDocument {

    override val text: String
        get() = examples.joinToString("\n") { it.joinToString("") }

    override fun applyTo(viewer: StyledViewer) {
        viewer.styledDocument.apply {
            initStyle()

            val startOffset = length
            appendExamples()
            setParagraphStyle(startOffset, length - startOffset, EXAMPLE_STYLE, false)
        }
    }

    private fun StyledDocument.appendExamples() {
        appendExample(examples.first(), false)
        if (examples.size > 1) {
            val expandIconAttr = SimpleAttributeSet()
            StyleConstants.setComponent(expandIconAttr, ExpandButton(examples.size) {
                remove(length - 2, 2)
                val startOffset = length
                examples.drop(1).forEach { appendExample(it) }
                setParagraphStyle(startOffset, length - startOffset, EXAMPLE_STYLE, true)
            })

            newLine()
            appendString(" ", expandIconAttr)
            setParagraphStyle(length - 1, 1, EXPAND_ICON_PARAGRAPH_STYLE, false)
        }
    }

    private fun StyledDocument.appendExample(example: List<CharSequence>, newLine: Boolean = true) {
        if (newLine) {
            newLine()
        }
        appendString(" ", ICON_QUOTE_STYLE)
        appendString("\t")
        for (ex in example) {
            appendCharSequence(ex)
        }
    }

    override fun toString(): String = text

    private class ExpandButton(exampleCount: Int, action: ActionListener) : JButton(Icons.ExpandExamples) {

        init {
            toolTipText = message("title.google.document.examples.show.all", exampleCount)
            addActionListener(action)
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent?) {
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                }

                override fun mouseExited(e: MouseEvent?) {
                    cursor = Cursor.getDefaultCursor()
                }
            })
        }

        override fun updateUI() {
            super.setUI(ExpandButtonUI())
        }
    }

    private class ExpandButtonUI : BasicButtonUI() {

        private val icon = Icons.ExpandExamples

        override fun installDefaults(b: AbstractButton?) {
        }

        override fun getPreferredSize(c: JComponent): Dimension {
            return Dimension(icon.iconWidth, icon.iconHeight)
        }

        override fun paint(g: Graphics, c: JComponent) {
            icon.paintIcon(c, g, 0, 0)
        }

        override fun update(g: Graphics, c: JComponent) {
            paint(g, c)
        }

    }

    companion object : TranslationDocument.Factory<GExamples?, GoogleExamplesDocument> {

        private val BOLD_REGEX = Regex("<b>(.+?)</b>")

        private const val EXAMPLE_STYLE = "g_example_style"
        private const val ICON_QUOTE_STYLE = "g_example_icon_quote_style"
        private const val EXAMPLE_BOLD_STYLE = "g_example_bold_style"
        private const val EXPAND_ICON_PARAGRAPH_STYLE = "g_example_icon_expand_ps"

        override fun getDocument(input: GExamples?): GoogleExamplesDocument? {
            if (input == null || input.examples.isEmpty()) {
                return null
            }

            val examples = input.examples.asSequence()
                .map { (example) ->
                    example.chunked(BOLD_REGEX) { StyledString(it.groupValues[1], EXAMPLE_BOLD_STYLE) }
                }
                .toList()
            return GoogleExamplesDocument(examples)
        }

        private fun StyledDocument.initStyle() {
            val defaultStyle = getStyle(StyleContext.DEFAULT_STYLE)
            getStyleOrAdd(EXAMPLE_STYLE, defaultStyle) { style ->
                StyleConstants.setTabSet(style, TabSet(arrayOf(TabStop(JBUIScale.scale(5f)))))
                StyleConstants.setForeground(style, JBColor(0x606060, 0xBBBDBF))
            }
            getStyleOrAdd(EXAMPLE_BOLD_STYLE, defaultStyle) { style ->
                StyleConstants.setBold(style, true)
                StyleConstants.setForeground(style, JBColor(0x555555, 0xC8CACC))
            }
            getStyleOrAdd(ICON_QUOTE_STYLE) { style ->
                StyleConstants.setIcon(style, Icons.Quote)
            }
            getStyleOrAdd(EXPAND_ICON_PARAGRAPH_STYLE) { style ->
                StyleConstants.setAlignment(style, StyleConstants.ALIGN_CENTER)
            }
        }
    }
}