package cn.yiiguxing.plugin.translate.trans.text

import cn.yiiguxing.plugin.translate.ui.StyledViewer
import cn.yiiguxing.plugin.translate.util.text.*
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import java.awt.Color
import javax.swing.text.*

private const val POS_PARAGRAPH_STYLE_FIRST = "dict-pos-paragraph--first"
private const val POS_PARAGRAPH_STYLE = "dict-pos-paragraph"
private const val ENTRY_PARAGRAPH_STYLE = "dict-entry-paragraph"
private const val ENTRY_END_PARAGRAPH_STYLE = "dict-entry-end-paragraph"
private const val POS_STYLE = "dict-pos"
private const val WORD_STYLE = "dict-word"
private const val REVERSE_TRANSLATION_STYLE = "dict-reverse-translation"
private const val SEPARATOR_STYLE = "dict-separator"
private const val FOLDING_STYLE = "dict-folding"

/**
 * Dictionary Document
 */
class DictionaryDocument(private val dictionaryGroups: List<DictionaryGroup>) : TranslationDocument {

    override val translations: Set<String>
        get() = dictionaryGroups.asSequence()
            .map { it.entries }
            .flatten()
            .sortedByDescending { it.score }
            .map { it.word }
            .toSet()

    override val text: String
        get() = dictionaryGroups.toText()

    override fun applyTo(viewer: StyledViewer) {
        viewer.styledDocument.apply {
            initStyle()
            appendDictionaries(dictionaryGroups)
        }
    }

    enum class WordType { WORD, REVERSE }
}

/**
 * Dictionary Group
 *
 * @property partOfSpeech the part-of-speech
 * @property entries the dictionary entries
 */
data class DictionaryGroup(
    val partOfSpeech: String,
    val entries: List<DictionaryEntry>
)

/**
 * Entry of Dictionary
 */
data class DictionaryEntry(
    val word: String,
    val reverseTranslation: List<String> = emptyList(),
    val score: Float = 1f
)

private fun List<DictionaryGroup>.toText(): String {
    val wordsBuilder = StringBuilder()
    return joinToString("\n") { group ->
        wordsBuilder.also { builder ->
            builder.setLength(0)
            builder.append(group.partOfSpeech, ": ")
            group.entries.joinTo(builder, "; ") { it.word }
        }
    }
}

private fun StyledDocument.initStyle() {
    val defaultStyle = getStyle(StyleContext.DEFAULT_STYLE)
    initPosStyle(defaultStyle)
    initEntryStyle(defaultStyle)
    initWordStyle(defaultStyle)
    initReverseTranslationStyle(defaultStyle)
    initFoldingStyle(defaultStyle)
}

private fun StyledDocument.initPosStyle(defaultStyle: Style?) {
    getStyleOrAdd(POS_STYLE, defaultStyle) { style ->
        StyleConstants.setForeground(style, JBColor(0x293B2B, 0xDF7CFF))
        StyleConstants.setItalic(style, true)
    }
    getStyleOrAdd(POS_PARAGRAPH_STYLE_FIRST, defaultStyle) { style ->
        StyleConstants.setSpaceBelow(style, JBUIScale.scale(2f))
    }
    getStyleOrAdd(POS_PARAGRAPH_STYLE, defaultStyle) { style ->
        StyleConstants.setSpaceAbove(style, JBUIScale.scale(10f))
        StyleConstants.setSpaceBelow(style, JBUIScale.scale(3f))
    }
}

private fun StyledDocument.initEntryStyle(defaultStyle: Style?) {
    val entryParagraphStyle = getStyleOrAdd(ENTRY_PARAGRAPH_STYLE, defaultStyle) { style ->
        StyleConstants.setLeftIndent(style, JBUIScale.scale(12f))
        StyleConstants.setSpaceBelow(style, JBUIScale.scale(8f))
    }
    getStyleOrAdd(ENTRY_END_PARAGRAPH_STYLE, entryParagraphStyle) { style ->
        StyleConstants.setSpaceBelow(style, 0f)
    }
}

private fun StyledDocument.initWordStyle(defaultStyle: Style?) {
    getStyleOrAdd(WORD_STYLE, defaultStyle) { style ->
        val color = JBColor(0x9C27B0, 0xFFFF00)
        val hoverColor = JBColor(0x7400FF, 0xDF9B00)
        StyleConstants.setForeground(style, color)
        StyledViewer.StyleConstants.setClickable(style, color, hoverColor, DictionaryDocument.WordType.WORD)
    }
}

private fun StyledDocument.initReverseTranslationStyle(defaultStyle: Style?) {
    getStyleOrAdd(REVERSE_TRANSLATION_STYLE, defaultStyle) { style ->
        StyleConstants.setItalic(style, true)
        val color = JBColor(0x3333E8, 0xFFC66D)
        val hoverColor = JBColor(0x762DFF, 0xDF7000)
        StyleConstants.setForeground(style, color)
        StyledViewer.StyleConstants.setClickable(style, color, hoverColor, DictionaryDocument.WordType.REVERSE)
    }
    getStyleOrAdd(SEPARATOR_STYLE, defaultStyle) { style ->
        StyleConstants.setForeground(style, JBColor(0xFF5555, 0x2196F3))
    }
}

private fun StyledDocument.initFoldingStyle(defaultStyle: Style?) {
    getStyleOrAdd(FOLDING_STYLE, defaultStyle) { style ->
        StyleConstants.setFontSize(style, getFont(style).size - 1)
        StyleConstants.setForeground(style, JBColor(0x777777, 0x888888))
        val background = JBColor(Color(0, 0, 0, 0x18), Color(0xFF, 0xFF, 0xFF, 0x10))
        StyleConstants.setBackground(style, background)
    }
}

private fun StyledDocument.appendDictionaries(groups: List<DictionaryGroup>) {
    with(groups) {
        if (isEmpty()) {
            return
        }

        val lastIndex = size - 1
        forEachIndexed { index, dict ->
            appendDict(dict, index == 0, index == lastIndex)
        }
    }
}

private fun StyledDocument.appendDict(groups: DictionaryGroup, isFirst: Boolean, isLast: Boolean) {
    setParagraphStyle(if (isFirst) POS_PARAGRAPH_STYLE_FIRST else POS_PARAGRAPH_STYLE)
    appendString(groups.partOfSpeech, POS_STYLE)
    appendString("\n")

    setParagraphStyle(if (isLast) ENTRY_END_PARAGRAPH_STYLE else ENTRY_PARAGRAPH_STYLE)
    val hasWordOnly = groups.entries
        .asSequence()
        .filter { it.reverseTranslation.isEmpty() }
        .map { it.word }
        .toList()
        .run {
            forEachIndexed { index, word ->
                if (index > 0) {
                    appendString(", ", SEPARATOR_STYLE)
                }
                appendString(word, WORD_STYLE)
            }

            isNotEmpty()
        }

    groups.entries
        .filter { it.reverseTranslation.isNotEmpty() }
        .takeIf { it.isNotEmpty() }
        ?.let { entries ->
            if (hasWordOnly) {
                appendString("\n")
            }

            val displayCount = if (hasWordOnly) 2 else 3
            val lastIndex = entries.size - 1
            entries.take(displayCount).forEachIndexed { index, dictEntry ->
                if (index > 0) {
                    appendString("\n")
                }
                insertDictEntry(length, dictEntry, index == lastIndex)
            }

            if (entries.size > displayCount) {
                appendString("\n")

                val foldingEntries = entries.takeLast(entries.size - displayCount)
                appendFolding(foldingEntries)
            }
        }

    if (!isLast) {
        appendString("\n")
    }
}

private fun StyledDocument.appendFolding(foldingEntries: List<DictionaryEntry>) {
    val placeholder = foldingEntries.run {
        take(3).joinToString(prefix = " ", postfix = if (size > 3) ", ... " else " ") { it.word }
    }

    setParagraphStyle(ENTRY_END_PARAGRAPH_STYLE)
    val foldingAttr = SimpleAttributeSet(getStyle(FOLDING_STYLE))
    StyledViewer.StyleConstants.setMouseListener(foldingAttr, createFoldingMouseListener(foldingEntries))
    appendString(placeholder, foldingAttr)
}

private fun createFoldingMouseListener(foldingEntries: List<DictionaryEntry>): StyledViewer.FoldingMouseListener {
    return StyledViewer.FoldingMouseListener(foldingEntries) { viewer, element, _ ->
        viewer.styledDocument.apply {
            var startOffset = element.startOffset
            remove(startOffset, element.rangeSize)
            val last = foldingEntries.size - 1
            foldingEntries.forEachIndexed { index, entry ->
                if (index > 0) {
                    startOffset = insert(startOffset, "\n")
                }
                startOffset = insertDictEntry(startOffset, entry, index == last)
            }
        }
    }
}

private fun StyledDocument.insertDictEntry(offset: Int, dictEntry: DictionaryEntry, isLast: Boolean): Int {
    var currentOffset = insert(offset, dictEntry.word, WORD_STYLE)
    currentOffset = insert(currentOffset, "\n")
    setParagraphStyle(ENTRY_END_PARAGRAPH_STYLE, offset, currentOffset - offset, true)

    val paragraphStart = currentOffset
    dictEntry.reverseTranslation.forEachIndexed { index, reverseTranslation ->
        if (index > 0) {
            currentOffset = insert(currentOffset, ", ", SEPARATOR_STYLE)
        }
        currentOffset = insert(currentOffset, reverseTranslation, REVERSE_TRANSLATION_STYLE)
    }

    val style = if (isLast) ENTRY_END_PARAGRAPH_STYLE else ENTRY_PARAGRAPH_STYLE
    setParagraphStyle(style, paragraphStart, currentOffset - paragraphStart, true)

    return currentOffset
}