@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans.text

import cn.yiiguxing.plugin.translate.trans.YoudaoTranslation
import cn.yiiguxing.plugin.translate.ui.StyledViewer
import cn.yiiguxing.plugin.translate.util.text.appendString
import cn.yiiguxing.plugin.translate.util.text.getStyleOrAdd
import com.intellij.ui.JBColor
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.StyledDocument

class YoudaoWebTranslationDocument(private val webTranslations: Map<String, String>) : TranslationDocument {

    override val text: String = webTranslations
        .map { (key, value) -> "$key - $value" }
        .joinToString(separator = "\n")

    override fun setupTo(viewer: StyledViewer) {
        viewer.styledDocument.apply {
            initStyle()
            appendContents(webTranslations)
        }
    }

    override fun toString(): String = text

    object Factory : TranslationDocument.Factory<YoudaoTranslation, YoudaoWebTranslationDocument> {
        override fun getDocument(input: YoudaoTranslation): YoudaoWebTranslationDocument? {
            val webTranslations = input.webExplains
                ?.mapNotNull { (key, values) ->
                    if (key == null || values == null) {
                        null
                    } else {
                        key to values.joinToString(separator = "; ")
                    }
                }
                ?.toMap()
                ?.takeIf { it.isNotEmpty() }
                ?: return null

            return YoudaoWebTranslationDocument(webTranslations)
        }
    }

    companion object {
        private const val REGULAR_STYLE = "yd_web_regular"
        private const val KEY_STYLE = "yd_web_key"
        private const val VALUE_STYLE = "yd_web_value"

        private fun StyledDocument.initStyle() {
            val defaultStyle = getStyle(StyleContext.DEFAULT_STYLE)
            getStyleOrAdd(REGULAR_STYLE) { style ->
                StyleConstants.setForeground(style, JBColor(0x555555, 0xACACAC))
            }
            getStyleOrAdd(KEY_STYLE, defaultStyle) { style ->
                StyleConstants.setForeground(style, JBColor(0x293B2B, 0x77B767))
            }
            getStyleOrAdd(VALUE_STYLE, defaultStyle) { style ->
                StyleConstants.setForeground(style, JBColor(0x707070, 0x6A8759))
            }
        }

        private fun StyledDocument.appendContents(webTranslations: Map<String, String>) {
            var isFirst = true
            for ((key, value) in webTranslations) {
                if (!isFirst) {
                    appendString("\n")
                } else {
                    isFirst = false
                }

                appendString(key, KEY_STYLE)
                appendString(" - ", REGULAR_STYLE)
                appendString(value, VALUE_STYLE)
            }
        }
    }
}