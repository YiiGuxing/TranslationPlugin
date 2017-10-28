package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.model.QueryResult
import cn.yiiguxing.plugin.translate.model.WebExplain
import cn.yiiguxing.plugin.translate.tts.TextToSpeech
import cn.yiiguxing.plugin.translate.ui.PhoneticButton
import cn.yiiguxing.plugin.translate.util.TranslationResultUtils
import cn.yiiguxing.plugin.translate.util.appendString
import cn.yiiguxing.plugin.translate.util.clear
import cn.yiiguxing.plugin.translate.util.trimEnd
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.JBColor
import com.intellij.util.Consumer
import java.awt.Font
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.regex.Pattern
import javax.swing.JTextPane
import javax.swing.text.*

/**
 * 文本样式
 */
object Styles {

    private val LOGGER = Logger.getInstance("#" + Styles::class.java.canonicalName)

    private val ATTR_QUERY = SimpleAttributeSet()
    private val ATTR_EXPLAIN_BASE = SimpleAttributeSet()
    private val ATTR_EXPLAIN = SimpleAttributeSet()
    private val ATTR_PRE_EXPLAINS = SimpleAttributeSet()
    private val ATTR_EXPLAINS = SimpleAttributeSet()
    private val ATTR_EXPLAINS_HOVER = SimpleAttributeSet()
    private val ATTR_WEB_EXPLAIN_TITLE = SimpleAttributeSet()
    private val ATTR_WEB_EXPLAIN_KEY = SimpleAttributeSet()
    private val ATTR_WEB_EXPLAIN_VALUES = SimpleAttributeSet()

    private const val QUERY_FONT_SCALE = 1.35f
    private const val PRE_EXPLAINS_FONT_SCALE = 1.15f
    private const val EXPLAINS_FONT_SCALE = 1.15f

    private val PATTERN_WORD = Pattern.compile("[a-zA-Z]+")

    init {
        ATTR_QUERY.let {
            StyleConstants.setItalic(it, true)
            StyleConstants.setBold(it, true)
            StyleConstants.setForeground(it, JBColor(0xFFEE6000.toInt(), 0xFFCC7832.toInt()))
        }

        val fg = JBColor(0xFF3E7EFF.toInt(), 0xFF8CBCE1.toInt())
        StyleConstants.setForeground(ATTR_EXPLAIN_BASE, fg)
        StyleConstants.setForeground(ATTR_EXPLAIN, fg)

        StyleConstants.setItalic(ATTR_PRE_EXPLAINS, true)
        StyleConstants.setForeground(ATTR_PRE_EXPLAINS, JBColor(0xFF7F0055.toInt(), 0xFFEAB1FF.toInt()))

        StyleConstants.setForeground(ATTR_EXPLAINS, JBColor(0xFF170591.toInt(), 0xFFFFC66D.toInt()))

        StyleConstants.setForeground(ATTR_EXPLAINS_HOVER, JBColor(0xA60EFF, 0xDF531F))

        StyleConstants.setForeground(ATTR_WEB_EXPLAIN_TITLE, JBColor(0xFF707070.toInt(), 0xFF808080.toInt()))
        StyleConstants.setForeground(ATTR_WEB_EXPLAIN_KEY, JBColor(0xFF4C4C4C.toInt(), 0xFF77B767.toInt()))
        StyleConstants.setForeground(ATTR_WEB_EXPLAIN_VALUES, JBColor(0xFF707070.toInt(), 0xFF6A8759.toInt()))
    }

    private fun setMouseListeners(textPane: JTextPane) {
        textPane.mouseListeners
                .filterIsInstance<ClickableStyleListener>()
                .forEach { textPane.removeMouseListener(it) }
        textPane.mouseMotionListeners
                .filterIsInstance<ClickableStyleListener>()
                .forEach { textPane.removeMouseMotionListener(it) }

        val listener = ClickableStyleListener()
        textPane.addMouseListener(listener)
        textPane.addMouseMotionListener(listener)
    }

    fun insertStylishResultText(textPane: JTextPane,
                                result: QueryResult,
                                explainsClickListener: OnTextClickListener?) {
        setMouseListeners(textPane)

        val document = textPane.styledDocument
        document.clear()

        val query = result.query
        if (query == null || query.isBlank()) {
            document.appendString("Noting to show.")
            return
        }

        insertHeader(textPane, query, result)

        val basicExplain = result.basicExplain
        if (basicExplain != null) {
            insertExplain(textPane, document, basicExplain.explains, true, explainsClickListener)
        } else {
            insertExplain(textPane, document, result.translation, false, explainsClickListener)
        }

        val webExplains = result.webExplains
        insertWebExplain(document, webExplains)

        document.trimEnd()
    }

    // 不能静态设置，否则scale改变时不能即时更新
    private fun updateFontSize(attr: MutableAttributeSet, font: Font, scale: Float): MutableAttributeSet = attr.apply {
        StyleConstants.setFontSize(this, Math.round(font.size * scale))
    }

    enum class Phonetic(val value: Int) {
        /**
         * 英式发音
         */
        UK(1),
        /**
         * 美式发音
         */
        US(2)
    }

    private fun insertHeader(textPane: JTextPane, query: String, result: QueryResult) {
        val document = textPane.document

        try {
            val title = query.trim { it <= ' ' }
            document.appendString(title.capitalize() + "\n",
                    updateFontSize(ATTR_QUERY, textPane.font, QUERY_FONT_SCALE))


            result.basicExplain?.let { (pho, phoUK, phoUS) ->
                var hasPhonetic = false

                if (!phoUK.isNullOrBlank()) {
                    insertPhonetic(document, title, phoUK!!, Phonetic.UK)
                    hasPhonetic = true
                }
                if (!phoUS.isNullOrBlank()) {
                    insertPhonetic(document, title, phoUS!!, Phonetic.US)
                    hasPhonetic = true
                }

                if (!hasPhonetic && !pho.isNullOrBlank()) {
                    document.appendString("[$pho]", ATTR_EXPLAIN)
                    hasPhonetic = true
                }

                if (hasPhonetic) {
                    document.appendString("\n")
                }
            }

            document.appendString("\n")
        } catch (e: BadLocationException) {
            LOGGER.error("insertHeader ", e)
        }
    }

    private fun insertPhonetic(document: Document,
                               query: String,
                               phoneticText: String,
                               phonetic: Phonetic) {
        document.appendString(if (phonetic === Phonetic.UK) "英[" else "美[", ATTR_EXPLAIN_BASE)

        val settings = Settings.instance
        val fontFamily = settings.phoneticFontFamily
        if (!settings.isOverrideFont || fontFamily.isNullOrBlank()) {
            ATTR_EXPLAIN.removeAttribute(StyleConstants.FontFamily)
        } else {
            StyleConstants.setFontFamily(ATTR_EXPLAIN, fontFamily)
        }

        document.appendString(phoneticText, ATTR_EXPLAIN)
        document.appendString("]", ATTR_EXPLAIN_BASE)

        val attr = SimpleAttributeSet()
        StyleConstants.setComponent(attr, PhoneticButton(Consumer { mouseEvent ->
            if (mouseEvent.clickCount == 1) {
                TextToSpeech.INSTANCE.speak(query)
            }
        }))
        document.appendString(" ", attr)
    }

    private fun insertExplain(textPane: JTextPane,
                              doc: StyledDocument,
                              explains: Array<String>?,
                              splitLabel: Boolean,
                              explainsClickListener: OnTextClickListener?) {
        if (explains == null || explains.isEmpty())
            return

        val attrPre = updateFontSize(ATTR_PRE_EXPLAINS, textPane.font, PRE_EXPLAINS_FONT_SCALE)
        val attr = updateFontSize(ATTR_EXPLAINS, textPane.font, EXPLAINS_FONT_SCALE)
        try {
            explains.filter { it.isNotBlank() }.forEach {
                if (splitLabel) {
                    val (wordClass, explain) = TranslationResultUtils.splitExplain(it)

                    wordClass?.let {
                        doc.appendString("$it ", attrPre)
                    }

                    val offset = doc.length
                    doc.appendString("$explain\n", attr)

                    val wordMatcher = PATTERN_WORD.matcher(explain)
                    while (wordMatcher.find()) {
                        val text = wordMatcher.group()
                        val start = wordMatcher.start() + offset
                        val style = ClickableStyle(textPane, text, start, explainsClickListener)
                        doc.setCharacterAttributes(start, text.length, setClickableStyle(attr, style), true)
                    }
                } else {
                    doc.appendString("$it\n", attr)
                }
            }
            doc.appendString("\n")
        } catch (e: BadLocationException) {
            LOGGER.error("insertExplain ", e)
        }
    }

    private fun insertWebExplain(doc: Document, webExplains: Array<WebExplain>?) {
        if (webExplains == null || webExplains.isEmpty())
            return

        try {
            doc.appendString("网络释义:\n", ATTR_WEB_EXPLAIN_TITLE)

            for ((key, values) in webExplains) {
                if (key == null) continue

                doc.appendString(key, ATTR_WEB_EXPLAIN_KEY)

                if (values != null && values.isNotEmpty()) {
                    doc.appendString(" -")

                    for (i in values.indices) {
                        doc.appendString(
                                " " + values[i] + if (i < values.size - 1) ";" else "",
                                ATTR_WEB_EXPLAIN_VALUES
                        )
                    }
                }

                doc.appendString("\n")
            }
        } catch (e: BadLocationException) {
            LOGGER.error("insertWebExplain ", e)
        }
    }

    private fun setClickableStyle(attrSet: MutableAttributeSet, style: ClickableStyle): MutableAttributeSet {
        return (attrSet.copyAttributes() as MutableAttributeSet).apply {
            addAttribute(ClickableStyle::class.java, style)
        }
    }

    interface OnTextClickListener {
        fun onTextClick(textPane: JTextPane, text: String)
    }

    private class ClickableStyle(
            private val textPane: JTextPane,
            private val text: String,
            private val startOffset: Int,
            private val listener: OnTextClickListener?
    ) {

        private var hover: Boolean = false

        internal fun performClick() {
            listener?.onTextClick(textPane, text)
        }

        internal fun onHover() {
            if (!hover) {
                var attr = updateFontSize(ATTR_EXPLAINS_HOVER, textPane.font, EXPLAINS_FONT_SCALE)
                attr = setClickableStyle(attr, this)
                textPane.styledDocument.setCharacterAttributes(startOffset, text.length, attr, true)

                hover = true
            }
        }

        internal fun clearHover() {
            if (hover) {
                var attr = updateFontSize(ATTR_EXPLAINS, textPane.font, EXPLAINS_FONT_SCALE)
                attr = setClickableStyle(attr, this)
                textPane.styledDocument.setCharacterAttributes(startOffset, text.length, attr, true)

                hover = false
            }
        }

    }

    private class ClickableStyleListener : MouseAdapter() {
        private var mLastHover: ClickableStyle? = null

        override fun mouseClicked(e: MouseEvent) {
            if (e.modifiers and InputEvent.BUTTON1_MASK == 0 || e.clickCount > 1)
                return

            getClickableStyle(e)?.performClick()
        }

        private fun getClickableStyle(e: MouseEvent): ClickableStyle? {
            val textPane = e.component as JTextPane
            val elem = textPane.styledDocument.getCharacterElement(textPane.viewToModel(e.point))
            val attr = elem.attributes as MutableAttributeSet
            return attr.getAttribute(ClickableStyle::class.java) as? ClickableStyle
        }

        override fun mouseMoved(e: MouseEvent) {
            val lastHover = mLastHover
            val hover = getClickableStyle(e)

            if (lastHover != hover) {
                mLastHover = hover

                lastHover?.clearHover()
                hover?.onHover()
            }
        }

        override fun mouseExited(e: MouseEvent) {
            mLastHover?.apply {
                clearHover()
                mLastHover = null
            }
        }
    }

}
