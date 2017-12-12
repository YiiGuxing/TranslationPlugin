package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.util.addStyle
import cn.yiiguxing.plugin.translate.util.appendString
import cn.yiiguxing.plugin.translate.util.clear
import com.intellij.ui.JBColor
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import kotlin.properties.Delegates

/**
 * TranslationPanel
 *
 * Created by Yii.Guxing on 2017/12/10
 */
abstract class TranslationPanel(protected val settings: Settings, maxWidth: Int) {

    var translation: Translation?
            by Delegates.observable(null) { _, oldValue: Translation?, newValue: Translation? ->
                if (oldValue !== newValue) {
                    update(newValue)
                }
            }

    protected abstract val sourceLangRowInitializer: Row.() -> Unit
    protected abstract val targetLangRowInitializer: Row.() -> Unit

    private val originalViewer = Viewer()
    private val originalPhonetic = JLabel()
    private val transViewer = Viewer()
    private val transPhonetic = JLabel()
    private val dictViewer = StyledDictViewer()
    private val otherExplainLabel = JLabel("网络释义:")
    private val otherExplainViewer = Viewer()

    val component: JComponent = panel {
        row(init = sourceLangRowInitializer)
        row { originalViewer(CCFlags.grow) }
        row { originalPhonetic(CCFlags.grow) }
        row(init = targetLangRowInitializer)
        row { transViewer(CCFlags.grow) }
        row { transPhonetic(CCFlags.grow) }
        row { dictViewer.component(CCFlags.grow) }
        row { otherExplainLabel() }
        row { otherExplainViewer(CCFlags.grow) }
    }

    init {
        initFont()
        initForeground()
    }

    private fun initFont() {
        var primaryFont: JBFont = Styles.defaultFont.deriveFont(JBUI.scale(FONT_SIZE_DEFAULT.toFloat()))
        var phoneticFont: JBFont = Styles.defaultFont.deriveFont(JBUI.scale(FONT_SIZE_PHONETIC.toFloat()))

        with(settings) {
            if (isOverrideFont) {
                primaryFont = primaryFontFamily?.let { JBUI.Fonts.create(it, FONT_SIZE_DEFAULT) } ?: primaryFont
                phoneticFont = phoneticFontFamily?.let { JBUI.Fonts.create(it, FONT_SIZE_PHONETIC) } ?: phoneticFont
            }
        }

        originalViewer.font = primaryFont.deriveFont(Font.ITALIC, FONT_SIZE_LARGE.toFloat())
        transViewer.font = primaryFont.deriveFont(FONT_SIZE_LARGE.toFloat())
        dictViewer.font = primaryFont
        otherExplainViewer.font = primaryFont
        originalPhonetic.font = phoneticFont
        transPhonetic.font = phoneticFont

        onUpdateFont(primaryFont, phoneticFont)
    }

    protected open fun onUpdateFont(primaryFont: JBFont, phoneticFont: JBFont) = Unit

    private fun initForeground() {
        originalViewer.foreground = JBColor(0xEE6000, 0xCC7832)
        transViewer.foreground = JBColor(0x170591, 0xFFC66D)
        JBColor(0x3E7EFF, 0x8CBCE1).let {
            originalPhonetic.foreground = it
            transPhonetic.foreground = it
        }
        otherExplainLabel.foreground = JBColor(0x707070, 0x808080)

        with(otherExplainViewer) {
            foreground = JBColor(0x555555, 0xACACAC)
            val defaultStyle = getStyle(StyleContext.DEFAULT_STYLE)
            styledDocument.addStyle(EXPLAIN_KEY_STYLE, defaultStyle) {
                StyleConstants.setForeground(this, JBColor(0x4C4C4C, 0x77B767))
            }
            styledDocument.addStyle(EXPLAIN_VALUE_STYLE, defaultStyle) {
                StyleConstants.setForeground(this, JBColor(0x707070, 0x6A8759))
            }
        }
    }

    protected open fun update(translation: Translation?) {
        with(translation) {
            if (this != null) {
                originalViewer.updateText(original)
                transViewer.updateText(trans)

                phoneticSymbol?.let { (src, trans) ->
                    originalPhonetic.updateText(src)
                    transPhonetic.updateText(trans)
                }

                dictViewer.dictionaries = dictionaries
                dictViewer.component.isVisible = true

                insertOtherExplain(otherExplain)
            } else {
                originalViewer.empty()
                originalPhonetic.empty()
                transViewer.empty()
                transPhonetic.empty()
                otherExplainViewer.empty()

                otherExplainLabel.isVisible = false
                dictViewer.component.isVisible = false
                dictViewer.dictionaries = null
            }
        }
    }

    private fun insertOtherExplain(explain: Map<String, String>) {
        with(otherExplainViewer) {
            styledDocument.clear()

            if (explain.isEmpty()) {
                isVisible = false
                otherExplainLabel.isVisible = false
                return
            }

            styledDocument.apply {
                val keyStyle = getStyle(EXPLAIN_KEY_STYLE)
                val valueStyle = getStyle(EXPLAIN_VALUE_STYLE)

                val lastIndex = explain.size - 1
                var index = 0
                for ((key, value) in explain) {
                    appendString(key, keyStyle)
                    appendString(" - ")
                    appendString(value, valueStyle)
                    if (index++ < lastIndex) {
                        appendString("\n")
                    }
                }
            }

            isVisible = true
            otherExplainLabel.isVisible = true
        }
    }

    private fun Viewer.updateText(text: String?) {
        this.text = text
        isVisible = !text.isNullOrEmpty()
    }

    private fun JLabel.updateText(text: String?) {
        this.text = text
        isVisible = !text.isNullOrEmpty()
    }

    private fun Viewer.empty() {
        isVisible = false
        document.clear()
    }

    private fun JLabel.empty() {
        isVisible = false
        text = null
    }

    companion object {
        private const val FONT_SIZE_LARGE = 16
        private const val FONT_SIZE_DEFAULT = 14
        private const val FONT_SIZE_PHONETIC = 12

        private const val EXPLAIN_KEY_STYLE = "explain_key"
        private const val EXPLAIN_VALUE_STYLE = "explain_value"
    }

}