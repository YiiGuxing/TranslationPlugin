package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument
import cn.yiiguxing.plugin.translate.ui.StyledViewer
import cn.yiiguxing.plugin.translate.util.chunked
import cn.yiiguxing.plugin.translate.util.text.*
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import icons.TranslationIcons
import java.awt.Color
import javax.swing.text.*

class GoogleExamplesDocument private constructor(private val examples: List<List<CharSequence>>) : TranslationDocument {

    override val text: String
        get() = examples.joinToString("\n") { it.joinToString("") }

    override fun applyTo(viewer: StyledViewer) {
        viewer.styledDocument.apply {
            initStyle()

            val startOffset = length
            appendExamples()
            setParagraphStyle(startOffset, length - startOffset, EXAMPLE_PARAGRAPH_STYLE, false)
        }
    }

    private fun StyledDocument.appendExamples() {
        appendExample(examples.first(), false)
        if (examples.size > 1) {
            newLine()
            val startOffset = length
            val foldingAttr = SimpleAttributeSet(getStyle(EXAMPLE_FOLDING_STYLE))
            StyledViewer.StyleConstants.setMouseListener(foldingAttr, createFoldingMouseListener(examples.drop(1)))
            val placeholder = " " + message("title.google.document.examples.show.all", examples.size) + " "
            appendString(placeholder, foldingAttr)
            setParagraphStyle(startOffset, placeholder.length, EXAMPLE_FOLDING_PARAGRAPH_STYLE, false)
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

    private fun createFoldingMouseListener(foldedExamples: List<List<CharSequence>>): StyledViewer.FoldingMouseListener {
        return StyledViewer.FoldingMouseListener(foldedExamples) { viewer, element, _ ->
            viewer.styledDocument.apply {
                remove(element.startOffset - 1, element.rangeSize + 1)
                val startOffset = length
                examples.drop(1).forEach { appendExample(it) }
                setParagraphStyle(startOffset, length - startOffset, EXAMPLE_PARAGRAPH_STYLE, true)
            }
        }
    }

    override fun toString(): String = text

    companion object : TranslationDocument.Factory<GExamples?, GoogleExamplesDocument> {

        private val BOLD_REGEX = Regex("<b>(.+?)</b>")

        private const val EXAMPLE_PARAGRAPH_STYLE = "g_example_p_style"
        private const val ICON_QUOTE_STYLE = "g_example_icon_quote_style"
        private const val EXAMPLE_BOLD_STYLE = "g_example_bold_style"
        private const val EXAMPLE_FOLDING_STYLE = "g_example_folding_style"
        private const val EXAMPLE_FOLDING_PARAGRAPH_STYLE = "g_example_folding_ps"

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
            getStyleOrAdd(EXAMPLE_PARAGRAPH_STYLE, defaultStyle) { style ->
                StyleConstants.setTabSet(style, TabSet(arrayOf(TabStop(JBUIScale.scale(5f)))))
                StyleConstants.setForeground(style, JBColor(0x606060, 0xBBBDBF))
            }
            getStyleOrAdd(EXAMPLE_BOLD_STYLE, defaultStyle) { style ->
                StyleConstants.setBold(style, true)
                StyleConstants.setForeground(style, JBColor(0x555555, 0xC8CACC))
            }
            getStyleOrAdd(ICON_QUOTE_STYLE) { style ->
                StyleConstants.setIcon(style, TranslationIcons.Quote)
            }
            getStyleOrAdd(EXAMPLE_FOLDING_STYLE, defaultStyle) { style ->
                StyleConstants.setFontSize(style, getFont(style).size - 1)
                StyleConstants.setForeground(style, JBColor(0x777777, 0x888888))
                val background = JBColor(Color(0, 0, 0, 0x18), Color(0xFF, 0xFF, 0xFF, 0x10))
                StyleConstants.setBackground(style, background)
            }
            getStyleOrAdd(EXAMPLE_FOLDING_PARAGRAPH_STYLE, defaultStyle) { style ->
                StyleConstants.setSpaceAbove(style, JBUIScale.scale(8f))
            }
        }
    }
}