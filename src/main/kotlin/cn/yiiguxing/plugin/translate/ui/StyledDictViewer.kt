package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.Dict
import cn.yiiguxing.plugin.translate.trans.DictEntry
import cn.yiiguxing.plugin.translate.util.addStyle
import cn.yiiguxing.plugin.translate.util.appendString
import cn.yiiguxing.plugin.translate.util.clear
import cn.yiiguxing.plugin.translate.util.insert
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Cursor
import java.awt.Font
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JTextPane
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

    private var onEntryClickListener: ((entry: Entry) -> Unit)? = null
    private var onFoldingExpandedListener: ((List<DictEntry>) -> Unit)? = null

    private val viewer: JTextPane = Viewer()

    private val defaultStyle: Style by lazy { viewer.getStyle(StyleContext.DEFAULT_STYLE) }
    private val posParagraphStyle: Style by lazy { viewer.getStyle(POS_PARAGRAPH_STYLE) }
    private val entryParagraphStyle: Style by lazy { viewer.getStyle(ENTRY_PARAGRAPH_STYLE) }
    private val posStyle: Style by lazy { viewer.getStyle(POS_STYLE) }
    private val wordStyle: Style by lazy { viewer.getStyle(WORD_STYLE) }
    private val reverseTranslationStyle: Style by lazy { viewer.getStyle(REVERSE_TRANSLATION_STYLE) }
    private val separatorStyle: Style by lazy { viewer.getStyle(SEPARATOR_STYLE) }
    private val foldingStyle: Style by lazy { viewer.getStyle(FOLDING_STYLE) }

    val component: JComponent get() = viewer

    var font: Font
        get() = viewer.font
        set(value) {
            viewer.font = value
        }

    init {
        viewer.apply {
            ViewerMouseAdapter().let {
                addMouseListener(it)
                addMouseMotionListener(it)
            }

            styledDocument.apply {
                initParagraphStyles()
                initPartOfSpeechStyle()
                initDictEntryStyles()
                initFoldStyle()
            }
        }
    }

    private enum class StyleConstant {
        MOUSE_LISTENER
    }

    /**
     * 段落样式
     */
    private fun StyledDocument.initParagraphStyles() {
        addStyle(POS_PARAGRAPH_STYLE, defaultStyle) {
            StyleConstants.setSpaceAbove(this, JBUI.scale(12f))
            StyleConstants.setSpaceBelow(this, JBUI.scale(2f))
        }
        addStyle(ENTRY_PARAGRAPH_STYLE, defaultStyle) {
            StyleConstants.setLeftIndent(this, JBUI.scale(12f))
        }
    }

    /**
     * 词性样式
     */
    private fun StyledDocument.initPartOfSpeechStyle() {
        addStyle(POS_STYLE, defaultStyle) {
            StyleConstants.setForeground(this, JBColor(0x293B2B, 0xDF7CFF))
            StyleConstants.setItalic(this, true)
        }
    }

    /**
     * 词、反向翻译样式
     */
    private fun StyledDocument.initDictEntryStyles() {
        addStyle(WORD_STYLE, defaultStyle) {
            JBColor(0x3C0078, 0xFFFF00).let {
                StyleConstants.setForeground(this, it)

                val hoverColor = JBColor(0x7400FF, 0xDF9B00)
                val mouseListener = EntryMouseListener(EntryType.WORD, it, hoverColor)
                addAttribute(StyleConstant.MOUSE_LISTENER, mouseListener)
            }
        }
        addStyle(REVERSE_TRANSLATION_STYLE, defaultStyle) {
            JBColor(0x3333E8, 0xFFC66D).let {
                StyleConstants.setForeground(this, it)

                val hoverColor = JBColor(0x762DFF, 0xDF7000)
                val mouseListener = EntryMouseListener(EntryType.REVERSE_TRANSLATION, it, hoverColor)
                addAttribute(StyleConstant.MOUSE_LISTENER,
                        mouseListener)
            }
        }
        addStyle(SEPARATOR_STYLE, defaultStyle) {
            StyleConstants.setForeground(this, JBColor(0xFF5555, 0x02B1DB))
        }
    }

    /**
     * 折叠样式
     */
    private fun StyledDocument.initFoldStyle() {
        addStyle(FOLDING_STYLE, defaultStyle) {
            StyleConstants.setForeground(this, JBColor(0x777777, 0x888888))
            val background = JBColor(Color(0, 0, 0, 0x18), Color(0xFF, 0xFF, 0xFF, 0x10))
            StyleConstants.setBackground(this, background)
        }
    }

    /**
     * 设置点击监听器
     */
    fun setOnEntryClickListener(listener: ((entry: Entry) -> Unit)?) {
        onEntryClickListener = listener
    }

    /**
     * 设置展开折叠的监听器
     */
    fun setOnFoldingExpandedListener(listener: ((List<DictEntry>) -> Unit)?) {
        onFoldingExpandedListener = listener
    }

    private fun update() {
        viewer.styledDocument.apply {
            clear()
            dictionaries?.let { insertDictionaries(it) }
        }
    }

    private fun StyledDocument.insertDictionaries(dictionaries: List<Dict>) {
        with(dictionaries) {
            if (isEmpty()) {
                return
            }

            val lastIndex = size - 1
            forEachIndexed { index, dict ->
                insertDict(dict, index < lastIndex)
            }
        }
    }

    private fun StyledDocument.insertDict(dict: Dict, breakEnd: Boolean) {
        with(dict.entries) {
            var paragraphOffset = length
            appendString(dict.partOfSpeech, posStyle)
            appendString("\n")
            setParagraphAttributes(paragraphOffset, length - paragraphOffset, posParagraphStyle, true)

            paragraphOffset = length

            take(3).forEach {
                insertDictEntry(length, it)
                appendString("\n")
            }
            if (size > 3) {
                val foldingEntries = takeLast(size - 3)
                val placeholder = foldingEntries.run {
                    take(3).joinToString(prefix = " ", postfix = if (size > 3) ", ... " else " ") { it.word }
                }

                SimpleAttributeSet(foldingStyle).let {
                    it.addAttribute(StyleConstant.MOUSE_LISTENER, FoldingMouseListener(foldingEntries))
                    appendString(placeholder, it)
                }
            }

            if (breakEnd) {
                appendString("\n")
            }
            setParagraphAttributes(paragraphOffset, length - paragraphOffset, entryParagraphStyle, true)
        }
    }

    private fun StyledDocument.insertDictEntry(offset: Int, dictEntry: DictEntry): Int {
        var currentOffset = insert(offset, dictEntry.word, wordStyle)
        currentOffset = insert(currentOffset, "\n")

        dictEntry.reverseTranslation.forEachIndexed { index, reverseTrans ->
            if (index > 0) {
                currentOffset = insert(currentOffset, ", ", separatorStyle)
            }
            currentOffset = insert(currentOffset, reverseTrans, reverseTranslationStyle)
        }

        return currentOffset
    }


    private inner class ViewerMouseAdapter : MouseAdapter() {

        private var lastElement: Element? = null

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
            lastElement?.run {
                mouseListener?.mouseExited(this)
                lastElement = null
            }
        }

        override fun mouseExited(event: MouseEvent) = exitLastElement()

        override fun mouseClicked(event: MouseEvent) {
            with(event) {
                if (modifiers and InputEvent.BUTTON1_MASK == 0 || clickCount > 1) {
                    return
                }

                consume()
                with(characterElement) {
                    (attributes.getAttribute(StyleConstant.MOUSE_LISTENER) as? BaseMouseListener)
                            ?.mouseClicked(this)
                }
            }
        }
    }

    private inner abstract class BaseMouseListener {
        abstract fun mouseClicked(element: Element)

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
            onEntryClickListener?.run {
                val text = with(element) {
                    document.getText(startOffset, offsetLength)
                }
                invoke(Entry(entryType, text))
            }
        }

    }

    private inner class FoldingMouseListener(private val foldingEntries: List<DictEntry>) : BaseMouseListener() {

        override fun mouseClicked(element: Element) {
            with(element) {
                styledDocument.apply {
                    var startOffset = startOffset
                    remove(startOffset, offsetLength)
                    foldingEntries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            startOffset = insert(startOffset, "\n")
                        }
                        startOffset = insertDictEntry(startOffset, entry)
                    }
                }
            }
            onFoldingExpandedListener?.invoke(foldingEntries)
        }
    }

    companion object {
        private const val POS_PARAGRAPH_STYLE = "part_of_speech_paragraph"
        private const val ENTRY_PARAGRAPH_STYLE = "entry_paragraph"
        private const val POS_STYLE = "part_of_speech"
        private const val WORD_STYLE = "word"
        private const val REVERSE_TRANSLATION_STYLE = "reverse_translation"
        private const val SEPARATOR_STYLE = "separator"
        private const val FOLDING_STYLE = "folding"

        private inline val Element.styledDocument: StyledDocument
            get() = document as StyledDocument
        private inline val Element.offsetLength: Int
            get() = endOffset - startOffset
    }

}