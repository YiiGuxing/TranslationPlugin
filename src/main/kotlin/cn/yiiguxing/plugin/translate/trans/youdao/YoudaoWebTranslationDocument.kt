@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans.youdao

import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument
import cn.yiiguxing.plugin.translate.ui.StyledViewer
import cn.yiiguxing.plugin.translate.util.text.appendString
import cn.yiiguxing.plugin.translate.util.text.getStyleOrAdd
import com.intellij.ui.JBColor
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.StyledDocument

class YoudaoWebTranslationDocument private constructor(private val webTranslations: List<WebTranslation>) :
    TranslationDocument {

    override val text: String = webTranslations.joinToString(separator = "\n") { translation ->
        "${translation.key} - ${translation.values.joinToString("; ")}"
    }

    override fun applyTo(viewer: StyledViewer) {
        viewer.styledDocument.apply {
            initStyle()
            appendContents(webTranslations)
        }
    }

    override fun toString(): String = text

    enum class WordType { WEB_KEY, WEB_VALUE }

    private class WebTranslation(val key: String, val values: Array<out String>)

    object Factory : TranslationDocument.Factory<YoudaoTranslation, YoudaoWebTranslationDocument> {
        override fun getDocument(input: YoudaoTranslation): YoudaoWebTranslationDocument? {
            val webTranslations = input.webExplains
                ?.mapNotNull { (key, values) ->
                    val filteredValues = values
                        ?.filterNotNull()
                        ?.filter { it.isNotBlank() }
                        ?.toTypedArray()
                    if (key.isNullOrBlank() || filteredValues.isNullOrEmpty()) {
                        null
                    } else {
                        WebTranslation(key.trim(), filteredValues)
                    }
                }
                ?.takeIf { it.isNotEmpty() }
                ?: return null

            // q: 请翻译为英文：修复指定非空参数为空的问题
            // a: Fix the problem of specifying a non-null parameter as null

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
                StyleConstants.setForeground(style, JBColor(0x555555, 0x738E5B))
            }
            getStyleOrAdd(KEY_STYLE, defaultStyle) { style ->
                val color = JBColor(0x293B2B, 0x77B767)
                val hoverColor = JBColor(0x5B6E5D, 0x8CD07C)
                StyleConstants.setForeground(style, color)
                StyledViewer.StyleConstants.setClickable(style, color, hoverColor, WordType.WEB_KEY)
            }
            getStyleOrAdd(VALUE_STYLE, defaultStyle) { style ->
                val color = JBColor(0x707070, 0x6A8759)
                val hoverColor = JBColor(0x505050, 0x8BA870)
                StyleConstants.setForeground(style, color)
                StyledViewer.StyleConstants.setClickable(style, color, hoverColor, WordType.WEB_VALUE)
            }
        }

        private fun StyledDocument.appendContents(webTranslations: List<WebTranslation>) {
            webTranslations.forEachIndexed { index, webTranslation ->
                if (index > 0) {
                    appendString("\n")
                }
                appendString(webTranslation.key, KEY_STYLE)
                appendString(" - ", REGULAR_STYLE)

                webTranslation.values.forEachIndexed { i, value ->
                    if (i > 0) {
                        appendString("; ", REGULAR_STYLE)
                    }
                    appendString(value, VALUE_STYLE)
                }
            }
        }
    }
}