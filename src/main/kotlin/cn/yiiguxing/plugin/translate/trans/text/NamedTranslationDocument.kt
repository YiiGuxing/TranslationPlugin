package cn.yiiguxing.plugin.translate.trans.text

import cn.yiiguxing.plugin.translate.ui.StyledViewer
import cn.yiiguxing.plugin.translate.util.text.appendString
import cn.yiiguxing.plugin.translate.util.text.getStyleOrAdd
import cn.yiiguxing.plugin.translate.util.text.setParagraphStyle
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

class NamedTranslationDocument(
    val name: String,
    val document: TranslationDocument
) : TranslationDocument {

    override val text: String
        get() = "$name\n${document.text}"

    private fun appendName(viewer: StyledViewer) {
        viewer.styledDocument.apply {
            setParagraphStyle(style = TITLE_PARAGRAPH_STYLE)
            if (length > 0) {
                setParagraphStyle(style = TITLE_PARAGRAPH_STYLE2, replace = false)
            }
            appendString("$name\n", TITLE_STYLE)
            setParagraphStyle(style = TITLE_END_PARAGRAPH_STYLE)
        }
    }

    override fun applyTo(viewer: StyledViewer) {
        viewer.initStyle()
        appendName(viewer)
        document.applyTo(viewer)
    }

    override fun toString(): String = text

    companion object {
        private const val TITLE_STYLE = "named_translation_document_title"
        private const val TITLE_PARAGRAPH_STYLE = "named_translation_document_title_ps"
        private const val TITLE_PARAGRAPH_STYLE2 = "named_translation_document_title_ps2"
        private const val TITLE_END_PARAGRAPH_STYLE = "named_translation_document_title_eps"

        private fun StyledViewer.initStyle() {
            val styledDocument = styledDocument
            val defaultStyle = getStyle(StyleContext.DEFAULT_STYLE)

            styledDocument.getStyleOrAdd(TITLE_PARAGRAPH_STYLE, defaultStyle) { style ->
                StyleConstants.setSpaceBelow(style, JBUIScale.scale(3f))
            }
            styledDocument.getStyleOrAdd(TITLE_PARAGRAPH_STYLE2, defaultStyle) { style ->
                StyleConstants.setSpaceAbove(style, JBUIScale.scale(16f))
            }
            styledDocument.getStyleOrAdd(TITLE_END_PARAGRAPH_STYLE, defaultStyle) { style ->
                StyleConstants.setSpaceAbove(style, 0f)
                StyleConstants.setSpaceBelow(style, 0f)
            }
            styledDocument.getStyleOrAdd(TITLE_STYLE, defaultStyle) { style ->
                StyleConstants.setForeground(style, JBColor(0x6E6E6E, 0xAFB1B3))
            }
        }
    }
}