package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.Dict
import cn.yiiguxing.plugin.translate.trans.DictEntry
import cn.yiiguxing.plugin.translate.ui.icon.Icons
import cn.yiiguxing.plugin.translate.util.addStyle
import cn.yiiguxing.plugin.translate.util.appendString
import cn.yiiguxing.plugin.translate.util.clear
import cn.yiiguxing.plugin.translate.util.insert
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Cursor
import java.awt.Font
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.text.*
import kotlin.properties.Delegates

/**
 * StyledDictViewer
 *
 * Created by Yii.Guxing on 2017/11/29
 */
class StyledDictViewer {

    var dictionaries: List<Dict>?
            by Delegates.observable(null) { _, oldValue: List<Dict>?, newValue: List<Dict>? ->
                if (oldValue != newValue) {
                    update()
                }
            }

    enum class EntryType { WORD, REVERSE_TRANSLATION }
    data class Entry(val entryType: EntryType, val value: String)

    private var onEntryClickedHandler: ((entry: Entry) -> Unit)? = null
    private var onFoldingExpandedHandler: ((List<DictEntry>) -> Unit)? = null

    private val viewer: Viewer = Viewer()
    private val copyItem: CopyItem = CopyItem()

    val component: JComponent get() = viewer

    var font: Font
        get() = viewer.font
        set(value) {
            viewer.font = value
        }

    init {
        viewer.apply {
            dragEnabled = false
            disableSelection()

            ViewerMouseAdapter().let {
                addMouseListener(it)
                addMouseMotionListener(it)
            }
            setupPopupMenu()
        }
    }

    private enum class StyleConstant {
        MOUSE_LISTENER
    }

    private val defaultStyle: Style by lazy { viewer.getStyle(StyleContext.DEFAULT_STYLE) }

    // 词性样式
    private val posStyle: Style by lazy {
        viewer.styledDocument.addStyle(POS_STYLE, defaultStyle) {
            StyleConstants.setForeground(this, JBColor(0x293B2B, 0xDF7CFF))
            StyleConstants.setItalic(this, true)
        }
    }
    private val posParagraphStyleFirst: Style by lazy {
        viewer.styledDocument.addStyle(POS_PARAGRAPH_STYLE_FIRST, defaultStyle) {
            StyleConstants.setSpaceBelow(this, JBUI.scale(2f))
        }
    }
    private val posParagraphStyle: Style by lazy {
        viewer.styledDocument.addStyle(POS_PARAGRAPH_STYLE, defaultStyle) {
            StyleConstants.setSpaceAbove(this, JBUI.scale(10f))
            StyleConstants.setSpaceBelow(this, JBUI.scale(3f))
        }
    }

    // Entry style
    private val entryParagraphStyle: Style by lazy {
        viewer.styledDocument.addStyle(ENTRY_PARAGRAPH_STYLE, defaultStyle) {
            StyleConstants.setLeftIndent(this, JBUI.scale(12f))
            StyleConstants.setSpaceBelow(this, JBUI.scale(8f))
        }
    }
    private val entryEndParagraphStyle: Style by lazy {
        viewer.styledDocument.addStyle(ENTRY_END_PARAGRAPH_STYLE, entryParagraphStyle) {
            StyleConstants.setSpaceBelow(this, JBUI.scale(0f))
        }
    }

    // 单词样式
    private val wordStyle: Style by lazy {
        viewer.styledDocument.addStyle(WORD_STYLE, defaultStyle) {
            JBColor(0x9C27B0, 0xFFFF00).let {
                StyleConstants.setForeground(this, it)

                val hoverColor = JBColor(0x7400FF, 0xDF9B00)
                val mouseListener = EntryMouseListener(EntryType.WORD, it, hoverColor)
                addAttribute(StyleConstant.MOUSE_LISTENER, mouseListener)
            }
        }
    }

    // 反向翻译样式
    private val reverseTranslationStyle: Style by lazy {
        viewer.styledDocument.addStyle(REVERSE_TRANSLATION_STYLE, defaultStyle) {
            StyleConstants.setItalic(this, true)
            JBColor(0x3333E8, 0xFFC66D).let {
                StyleConstants.setForeground(this, it)

                val hoverColor = JBColor(0x762DFF, 0xDF7000)
                val mouseListener = EntryMouseListener(EntryType.REVERSE_TRANSLATION, it, hoverColor)
                addAttribute(StyleConstant.MOUSE_LISTENER, mouseListener)
            }
        }
    }
    private val separatorStyle: Style by lazy {
        viewer.styledDocument.addStyle(SEPARATOR_STYLE, defaultStyle) {
            StyleConstants.setForeground(this, JBColor(0xFF5555, 0x2196F3))
        }
    }

    // 折叠样式
    private val foldingStyle: Style by lazy {
        viewer.styledDocument.addStyle(FOLDING_STYLE, defaultStyle) {
            StyleConstants.setForeground(this, JBColor(0x777777, 0x888888))
            val background = JBColor(Color(0, 0, 0, 0x18), Color(0xFF, 0xFF, 0xFF, 0x10))
            StyleConstants.setBackground(this, background)
        }
    }

    private fun Viewer.setupPopupMenu() {
        componentPopupMenu = JBPopupMenu().apply {
            add(copyItem)
        }
    }

    fun onEntryClicked(handler: ((entry: Entry) -> Unit)?) {
        onEntryClickedHandler = handler
    }

    fun onFoldingExpanded(handler: ((List<DictEntry>) -> Unit)?) {
        onFoldingExpandedHandler = handler
    }

    private fun update() {
        viewer.styledDocument.apply {
            clear()
            dictionaries?.let { insertDictionaries(it) }
        }
        viewer.caretPosition = 0
    }

    private fun StyledDocument.insertDictionaries(dictionaries: List<Dict>) {
        with(dictionaries) {
            if (isEmpty()) {
                return
            }

            val lastIndex = size - 1
            forEachIndexed { index, dict ->
                insertDict(dict, index == 0, index == lastIndex)
            }
        }
    }

    private fun StyledDocument.insertDict(dict: Dict, isFirst: Boolean, isLast: Boolean) {
        setParagraphStyle(style = if (isFirst) posParagraphStyleFirst else posParagraphStyle)
        dict.partOfSpeech.let {
            appendString(it, posStyle)
            appendString("\n")
        }

        setParagraphStyle(style = if (isLast) entryEndParagraphStyle else entryParagraphStyle)
        val hasWordOnly = dict.entries
                .asSequence()
                .filter { it.reverseTranslation.isEmpty() }
                .map { it.word }
                .toList()
                .run {
                    forEachIndexed { index, word ->
                        if (index > 0) {
                            appendString(", ", separatorStyle)
                        }
                        appendString(word, wordStyle)
                    }

                    isNotEmpty()
                }

        dict.entries
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
                        val placeholder = foldingEntries.run {
                            take(3).joinToString(prefix = " ", postfix = if (size > 3) ", ... " else " ") { it.word }
                        }

                        setParagraphStyle(style = entryEndParagraphStyle)
                        SimpleAttributeSet(foldingStyle).let {
                            it.addAttribute(StyleConstant.MOUSE_LISTENER, FoldingMouseListener(foldingEntries))
                            appendString(placeholder, it)
                        }
                    }
                }

        if (!isLast) {
            appendString("\n")
        }
    }

    private fun StyledDocument.insertDictEntry(offset: Int, dictEntry: DictEntry, isLast: Boolean): Int {
        var currentOffset = insert(offset, dictEntry.word, wordStyle)
        currentOffset = insert(currentOffset, "\n")
        setParagraphAttributes(offset, currentOffset - offset, entryEndParagraphStyle, true)

        val paragraphStart = currentOffset
        dictEntry.reverseTranslation.forEachIndexed { index, reverseTrans ->
            if (index > 0) {
                currentOffset = insert(currentOffset, ", ", separatorStyle)
            }
            currentOffset = insert(currentOffset, reverseTrans, reverseTranslationStyle)
        }

        val style = if (isLast) entryEndParagraphStyle else entryParagraphStyle
        setParagraphAttributes(paragraphStart, currentOffset - paragraphStart, style, true)

        return currentOffset
    }

    private class CopyItem : JBMenuItem("Copy", Icons.Copy) {

        var textSelection: String? = null
            set(value) {
                field = value
                isEnabled = !value.isNullOrEmpty()
            }

        init {
            isEnabled = false
            disabledIcon = Icons.Copy
            addActionListener {
                if (!textSelection.isNullOrEmpty()) {
                    CopyPasteManager.getInstance().setContents(StringSelection(textSelection))
                    textSelection = null
                }
            }
        }
    }

    private inner class ViewerMouseAdapter : MouseAdapter() {

        private var lastElement: Element? = null
        private var activeElement: Element? = null

        private inline val MouseEvent.characterElement: Element
            get() = viewer.styledDocument.getCharacterElement(viewer.viewToModel(point))

        private inline val Element.mouseListener: BaseMouseListener?
            get() = attributes.getAttribute(StyleConstant.MOUSE_LISTENER) as? BaseMouseListener

        override fun mouseMoved(event: MouseEvent) {
            val element = event.characterElement
            if (element !== lastElement) {
                exitLastElement()

                lastElement = element.mouseListener?.run {
                    mouseEntered(element)
                    element
                }
            }
        }

        private fun exitLastElement() {
            activeElement = null
            lastElement?.run {
                mouseListener?.mouseExited(this)
                lastElement = null
            }
        }

        override fun mouseExited(event: MouseEvent) = exitLastElement()

        override fun mousePressed(event: MouseEvent) {
            activeElement = event.characterElement
        }

        override fun mouseReleased(event: MouseEvent) { // 使用`mouseClicked`在MacOS下会出现事件丢失的情况...
            with(event) {
                characterElement.takeIf { it === activeElement }?.let { elem ->
                    (elem.attributes.getAttribute(StyleConstant.MOUSE_LISTENER) as? BaseMouseListener)?.run {
                        if (modifiers and MouseEvent.BUTTON1_MASK != 0) {
                            mouseClicked(elem)
                        }

                        if (isMetaDown) {
                            mouseRightButtonClicked(elem)
                        }
                    }
                }
            }
            activeElement = null
        }
    }

    private abstract inner class BaseMouseListener {
        abstract fun mouseClicked(element: Element)

        open fun mouseRightButtonClicked(element: Element) {}

        open fun mouseEntered(element: Element) {
            viewer.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }

        open fun mouseExited(element: Element) {
            viewer.cursor = Cursor.getDefaultCursor()
        }
    }

    private inner class EntryMouseListener(
            private val entryType: EntryType,
            color: Color,
            hoverColor: Color? = null
    ) : BaseMouseListener() {

        private val regularAttributes = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, color)
        }
        private val hoverAttributes = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, hoverColor ?: color)
        }

        override fun mouseEntered(element: Element) {
            super.mouseEntered(element)

            with(element) {
                styledDocument.setCharacterAttributes(startOffset, offsetLength, hoverAttributes, false)
            }
        }

        override fun mouseExited(element: Element) {
            super.mouseExited(element)

            with(element) {
                styledDocument.setCharacterAttributes(startOffset, offsetLength, regularAttributes, false)
            }
        }

        override fun mouseClicked(element: Element) {
            onEntryClickedHandler?.invoke(Entry(entryType, element.text))
        }

        override fun mouseRightButtonClicked(element: Element) {
            copyItem.textSelection = element.text
        }
    }

    private inner class FoldingMouseListener(private val foldingEntries: List<DictEntry>) : BaseMouseListener() {

        override fun mouseClicked(element: Element) {
            with(element) {
                styledDocument.apply {
                    var startOffset = startOffset
                    remove(startOffset, offsetLength)
                    val last = foldingEntries.size - 1
                    foldingEntries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            startOffset = insert(startOffset, "\n")
                        }
                        startOffset = insertDictEntry(startOffset, entry, index == last)
                    }
                }
            }

            onFoldingExpandedHandler?.invoke(foldingEntries)
        }
    }

    companion object {
        private const val POS_PARAGRAPH_STYLE_FIRST = "part_of_speech_paragraph_first"
        private const val POS_PARAGRAPH_STYLE = "part_of_speech_paragraph"
        private const val ENTRY_PARAGRAPH_STYLE = "entry_paragraph"
        private const val ENTRY_END_PARAGRAPH_STYLE = "entry_end_paragraph"
        private const val POS_STYLE = "part_of_speech"
        private const val WORD_STYLE = "word"
        private const val REVERSE_TRANSLATION_STYLE = "reverse_translation"
        private const val SEPARATOR_STYLE = "separator"
        private const val FOLDING_STYLE = "folding"

        private inline val Element.styledDocument: StyledDocument
            get() = document as StyledDocument
        private inline val Element.offsetLength: Int
            get() = endOffset - startOffset
        private inline val Element.text: String
            get() = document.getText(startOffset, offsetLength)

        @Suppress("NOTHING_TO_INLINE")
        private inline fun StyledDocument.setParagraphStyle(offset: Int? = null,
                                                            style: Style,
                                                            replace: Boolean = true) {
            setParagraphAttributes(offset ?: length, 0, style, replace)
        }
    }
}

