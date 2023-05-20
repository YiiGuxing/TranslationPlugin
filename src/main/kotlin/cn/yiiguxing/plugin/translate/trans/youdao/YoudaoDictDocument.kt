@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans.youdao

import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument
import cn.yiiguxing.plugin.translate.ui.StyledViewer
import cn.yiiguxing.plugin.translate.util.alphaBlend
import cn.yiiguxing.plugin.translate.util.text.*
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color
import javax.swing.JComponent
import javax.swing.text.*

class YoudaoDictDocument private constructor(
    private val wordStrings: List<CharSequence>,
    private val variantStrings: List<CharSequence>,
    override val translations: Set<String>
) : TranslationDocument {

    override val text: String get() = toString()

    override fun applyTo(viewer: StyledViewer) {
        viewer.apply {
            initStyle()
            setTabPosition()
            styledDocument.appendContents()
        }
    }

    private fun StyledViewer.setTabPosition() {
        val metrics = getFontMetrics(font)
        val tabWidth = wordStrings.asSequence()
            .filter { it is StyledString && it.style == POS_STYLE }
            .map { metrics.stringWidth(" $it") }
            .maxOrNull()
            ?: return

        val attrs = SimpleAttributeSet()
        val pos = tabWidth.toFloat() + JBUI.scale(2)
        val tabSet = TabSet(arrayOf(TabStop(pos, TabStop.ALIGN_RIGHT, TabStop.LEAD_NONE)))
        StyleConstants.setTabSet(attrs, tabSet)
        styledDocument.setParagraphAttributes(0, styledDocument.length, attrs, true)
    }

    private fun StyledDocument.appendContents() {
        appendStrings(wordStrings) { wordString ->
            if (wordString.style == POS_STYLE) "\t $wordString\t" else wordString.toString()
        }

        if (variantStrings.isNotEmpty()) {
            newLine()
            setParagraphStyle(length - 1, 1, SPACE_BELOW_STYLE, false)
            appendStrings(variantStrings)
        }
    }

    private inline fun StyledDocument.appendStrings(
        strings: List<CharSequence>,
        transform: (StyledString) -> String = { it.toString() }
    ) {
        for (string in strings) {
            if (string is StyledString) {
                appendString(transform(string), string.style)
            } else {
                appendString(string.toString(), REGULAR_STYLE)
            }
        }
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        for (wordString in wordStrings) {
            stringBuilder.append(wordString)
        }

        if (variantStrings.isNotEmpty()) {
            stringBuilder.append("\n\n")
            for (variantString in variantStrings) {
                stringBuilder.append(variantString)
            }
        }

        return stringBuilder.toString()
    }

    enum class WordType { WORD, VARIANT }

    object Factory : TranslationDocument.Factory<YoudaoTranslation, YoudaoDictDocument> {

        private val REGEX_WORD = Regex("[a-zA-Z]+([ -]?[a-zA-Z]+)*")
        private val REGEX_EXPLANATION = Regex(
            "^((a|adj|prep|pron|n|v|conj|s|sc|o|oc|vi|vt|aux|ad|adv|art|num|int|u|c|pl|abbr|det)\\.)(.+)$"
        )
        private val REGEX_WORDS_SEPARATOR = Regex("[,;，；]")
        private val REGEX_VARIANTS_SEPARATOR = Regex("\\s*或\\s*")
        private val REGEX_WORD_ANNOTATION = Regex("\\(.*?\\)|（.*?）|\\[.*?]|【.*?】|<.*?>")

        private const val GROUP_PART_OF_SPEECH = 1
        private const val GROUP_WORDS = 3

        override fun getDocument(input: YoudaoTranslation): YoudaoDictDocument? {
            val basicExplain = input.basicExplain ?: return null
            val explanations = basicExplain.explains?.takeIf { it.isNotEmpty() } ?: return null
            val variantStrings = getVariantStrings(basicExplain.wordForms)

            val wordStrings = ArrayList<CharSequence>()
            val translations = LinkedHashSet<String>()
            val newWordsStringBuilder = StringBuilder()
            val annotationMap = HashMap<String, String>()
            for (i in explanations.indices) {
                annotationMap.clear()
                if (i > 0) {
                    wordStrings += "\n"
                }

                val explanation = explanations[i]
                val matchResult = REGEX_EXPLANATION.find(explanation)
                val wordsString = if (matchResult != null) {
                    wordStrings += StyledString(matchResult.groups[GROUP_PART_OF_SPEECH]!!.value, POS_STYLE)
                    wordStrings += " "

                    matchResult.groups[GROUP_WORDS]!!.value.trim()
                } else explanation.trim()

                // issue: #581 (https://github.com/YiiGuxing/TranslationPlugin/issues/581)
                // 将词条中的注释部分使用占位符`[$index]`代替，以防止注释中有逗号等字符而被拆分。
                var annotationIndex = 0
                newWordsStringBuilder.setLength(0)
                wordsString.blocks(REGEX_WORD_ANNOTATION) { block, isAnnotation ->
                    if (isAnnotation) {
                        val annotationPlaceholder = "[${annotationIndex++}]"
                        newWordsStringBuilder.append(annotationPlaceholder)
                        annotationMap[annotationPlaceholder] = block
                    } else {
                        newWordsStringBuilder.append(block)
                    }
                }

                newWordsStringBuilder.toString().blocks(REGEX_WORDS_SEPARATOR) { separatorOrWord, isSeparator ->
                    if (isSeparator) {
                        val separator = when (separatorOrWord) {
                            ",", ";" -> "$separatorOrWord "
                            else -> separatorOrWord
                        }
                        wordStrings += StyledString(separator, SEPARATOR_STYLE)
                    } else {
                        separatorOrWord.trim().blocks(REGEX_WORD_ANNOTATION) { wordOrAnnotation, isAnnotation ->
                            if (isAnnotation) {
                                annotationMap
                                    .getValue(wordOrAnnotation)
                                    .blocks(REGEX_WORD) { annotationString, isWord ->
                                        wordStrings += if (isWord) {
                                            StyledString(annotationString, WORD_STYLE, WordType.VARIANT)
                                        } else {
                                            annotationString
                                        }
                                    }
                            } else {
                                translations += wordOrAnnotation
                                wordStrings += StyledString(wordOrAnnotation, WORD_STYLE, WordType.WORD)
                            }
                        }
                    }
                }
            }

            return YoudaoDictDocument(wordStrings, variantStrings, translations)
        }

        private fun getVariantStrings(wordForms: Array<out YWordFormWrapper>?): List<CharSequence> {
            val variantStrings = ArrayList<CharSequence>()
            wordForms?.takeIf { it.isNotEmpty() }
                ?.map { it.wordForm }
                ?.sortedBy { it.name.length }
                ?.forEachIndexed { i, wordForm ->
                    if (i > 0) {
                        variantStrings += "\n"
                    }

                    variantStrings += StyledString("${wordForm.name}: ", VARIANT_NAME_STYLE)
                    wordForm.value.split(REGEX_VARIANTS_SEPARATOR).forEachIndexed { index, value ->
                        if (index > 0) {
                            variantStrings += StyledString(", ", SEPARATOR_STYLE)
                        }
                        variantStrings += StyledString(value, VARIANT_STYLE, WordType.VARIANT)
                    }
                }

            return variantStrings
        }

        private fun String.blocks(regex: Regex, onBlock: (block: String, isMatched: Boolean) -> Unit) {
            var cursor = 0
            regex.findAll(this).forEach { matched ->
                val start = matched.range.first
                if (start != 0) {
                    val block = substring(cursor, matched.range.first)
                    onBlock(block, false)
                }
                onBlock(matched.value, true)
                cursor = matched.range.last + 1
            }
            if (cursor < length) {
                onBlock(substring(cursor, length), false)
            }
        }
    }

    companion object {
        private const val REGULAR_STYLE = "yd_dict_regular"
        private const val POS_STYLE = "yd_dict_part_of_speech"
        private const val WORD_BASE_STYLE = "yd_dict_word_base"
        private const val WORD_STYLE = "yd_dict_word"
        private const val VARIANT_STYLE = "yd_dict_variant"
        private const val SEPARATOR_STYLE = "yd_dict_separator"
        private const val VARIANT_NAME_STYLE = "yd_dict_variant_name"
        private const val SPACE_BELOW_STYLE = "yd_space_below"

        private val WORD_COLOR = JBColor(0x3333E8, 0xFFC66D)
        private val WORD_HOVER_COLOR = JBColor(0x762DFF, 0xDF7000)

        private fun StyledViewer.initStyle() {
            val styledDocument = styledDocument
            val defaultStyle = getStyle(StyleContext.DEFAULT_STYLE)

            styledDocument.getStyleOrAdd(REGULAR_STYLE, defaultStyle) { style ->
                StyleConstants.setForeground(style, JBColor(0x2A237A, 0xA9B7C6))
            }
            styledDocument.getStyleOrAdd(POS_STYLE, defaultStyle) { style ->
                StyleConstants.setBold(style, true)
                StyleConstants.setItalic(style, true)

                val fg = JBColor(0x9C27B0, 0xDF7CFF)
                val bg = fg.alphaBlend(getBackgroundColor(), 0.08f)
                StyleConstants.setForeground(style, fg)
                StyleConstants.setBackground(style, bg)
            }
            val wordBaseStyle = styledDocument.getStyleOrAdd(WORD_BASE_STYLE, defaultStyle) { style ->
                StyleConstants.setForeground(style, WORD_COLOR)
            }
            styledDocument.getStyleOrAdd(WORD_STYLE, wordBaseStyle) { style ->
                StyledViewer.StyleConstants.setClickable(style, WORD_COLOR, WORD_HOVER_COLOR, WordType.WORD)
            }
            styledDocument.getStyleOrAdd(VARIANT_STYLE, wordBaseStyle) { style ->
                StyleConstants.setItalic(style, true)
                StyledViewer.StyleConstants.setClickable(style, WORD_COLOR, WORD_HOVER_COLOR, WordType.VARIANT)
            }
            styledDocument.getStyleOrAdd(SEPARATOR_STYLE, defaultStyle) { style ->
                StyleConstants.setForeground(style, JBColor(0xFF5555, 0x2196F3))
            }
            styledDocument.getStyleOrAdd(SPACE_BELOW_STYLE, defaultStyle) { style ->
                StyleConstants.setSpaceBelow(style, JBUIScale.scale(14f))
            }
            styledDocument.getStyleOrAdd(VARIANT_NAME_STYLE, defaultStyle) { style ->
                StyleConstants.setItalic(style, true)
                StyleConstants.setForeground(style, JBColor(0x067D17, 0xA8C023))
            }
        }

        private tailrec fun JComponent.getBackgroundColor(): Color {
            val bg = if (isOpaque) background else null
            if (bg != null) {
                return bg
            }

            val parent = parent as? JComponent ?: return UIUtil.getLabelBackground()
            return parent.getBackgroundColor()
        }
    }
}