package cn.yiiguxing.plugin.translate.trans.text

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.StyledViewer
import cn.yiiguxing.plugin.translate.util.text.*
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import icons.TranslationIcons
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.StyledDocument

/**
 * Example document.
 */
class ExampleDocument(private val examples: List<List<CharSequence>>) : TranslationDocument {

    override val text: String
        get() = examples.joinToString("\n") { it.joinToString("") }

    override fun applyTo(viewer: StyledViewer) {
        viewer.styledDocument.apply {
            initStyle()

            val startOffset = length
            appendExamples()
            setParagraphStyle(STYLE_EXAMPLE_PARAGRAPH, startOffset, length - startOffset, false)
        }
    }

    private fun StyledDocument.appendExamples() {
        appendExample(examples.first(), false)
        if (examples.size > 1) {
            newLine()
            val startOffset = length
            val foldingAttr = SimpleAttributeSet(getStyle(STYLE_EXAMPLE_FOLDING))
            StyledViewer.StyleConstants.setMouseListener(foldingAttr, createFoldingMouseListener(examples.drop(1)))
            val placeholder = " " + message("examples.document.folding.show.all", examples.size) + " "
            appendString(placeholder, foldingAttr)
            setParagraphStyle(STYLE_EXAMPLE_FOLDING_PARAGRAPH, startOffset, placeholder.length, false)
        }
    }

    private fun StyledDocument.appendExample(example: List<CharSequence>, newLine: Boolean = true) {
        if (newLine) {
            newLine()
        }
        appendString(" ", STYLE_ICON_QUOTE)
        appendString(" ")
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
                setParagraphStyle(STYLE_EXAMPLE_PARAGRAPH, startOffset, length - startOffset, true)
            }
        }
    }

    override fun toString(): String = text


    companion object {
        /** Represents a bold style. */
        const val STYLE_EXAMPLE_BOLD = "example-bold"

        /** Represents a horizontal space style. */
        const val STYLE_EXAMPLE_SPACE = "example-space"

        // private styles
        private const val STYLE_EXAMPLE_PARAGRAPH = "example-p"
        private const val STYLE_ICON_QUOTE = "example-icon-quote"
        private const val STYLE_EXAMPLE_FOLDING = "example-folding"
        private const val STYLE_EXAMPLE_FOLDING_PARAGRAPH = "example-folding--p"

        private val SPACE_ICON = object : Icon {
            override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) = Unit
            override fun getIconWidth(): Int = JBUIScale.scale(16)
            override fun getIconHeight(): Int = 1
        }

        private fun StyledDocument.initStyle() {
            val defaultStyle = getStyle(StyleContext.DEFAULT_STYLE)
            getStyleOrAdd(STYLE_EXAMPLE_PARAGRAPH, defaultStyle) { style ->
                StyleConstants.setForeground(style, JBColor(0x606060, 0xBBBDBF))
            }
            getStyleOrAdd(STYLE_EXAMPLE_SPACE, defaultStyle) { style ->
                StyleConstants.setIcon(style, SPACE_ICON)
            }
            getStyleOrAdd(STYLE_EXAMPLE_BOLD, defaultStyle) { style ->
                StyleConstants.setBold(style, true)
                StyleConstants.setForeground(style, JBColor(0x555555, 0xC8CACC))
            }
            getStyleOrAdd(STYLE_ICON_QUOTE) { style ->
                StyleConstants.setIcon(style, TranslationIcons.Quote)
            }
            getStyleOrAdd(STYLE_EXAMPLE_FOLDING, defaultStyle) { style ->
                StyleConstants.setFontSize(style, getFont(style).size - 1)
                StyleConstants.setForeground(style, JBColor(0x777777, 0x888888))
                val background = JBColor(Color(0, 0, 0, 0x18), Color(0xFF, 0xFF, 0xFF, 0x10))
                StyleConstants.setBackground(style, background)
            }
            getStyleOrAdd(STYLE_EXAMPLE_FOLDING_PARAGRAPH, defaultStyle) { style ->
                StyleConstants.setSpaceAbove(style, JBUIScale.scale(8f))
            }
        }
    }
}