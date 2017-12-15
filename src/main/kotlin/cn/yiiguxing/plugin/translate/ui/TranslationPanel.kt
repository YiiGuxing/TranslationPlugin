package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.util.addStyle
import cn.yiiguxing.plugin.translate.util.appendString
import cn.yiiguxing.plugin.translate.util.clear
import cn.yiiguxing.plugin.translate.util.isNullOrBlank
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.JBColor
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Font
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTextPane
import javax.swing.event.PopupMenuEvent
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import kotlin.properties.Delegates

/**
 * TranslationPanel
 *
 * Created by Yii.Guxing on 2017/12/10
 */
abstract class TranslationPanel<T : JComponent>(protected val settings: Settings, maxWidth: Int) {

    protected val sourceLangComponent: T by lazy { onCreateLanguageComponent() }
    protected val targetLangComponent: T by lazy { onCreateLanguageComponent() }

    private val originalViewer = Viewer()
    private val originalPhonetic = JLabel()
    private val transViewer = Viewer()
    private val transPhonetic = JLabel()
    private val dictViewer = StyledDictViewer()
    private val otherExplainLabel = JLabel("网络释义:")
    private val otherExplainViewer = Viewer()

    private lateinit var sourceLangRow: Row
    private lateinit var targetLangRow: Row

    private var onNewTranslateHandler: ((String) -> Unit)? = null
    private var onRevalidateHandler: (() -> Unit)? = null
    private var onTextToSpeechHandler: ((String, Lang) -> Unit)? = null
    private var onFixLanguageHandler: ((Lang) -> Unit)? = null

    private val originalTTSLink = ttsLinkLabel {
        translation?.run {
            onTextToSpeechHandler?.invoke(original, srcLang)
        }
    }

    private val transTTSLink = ttsLinkLabel {
        translation?.run {
            if (trans != null) {
                onTextToSpeechHandler?.invoke(trans, targetLang)
            }
        }
    }

    private val fixLanguageLink = ActionLink {
        translation?.run {
            onFixLanguageHandler?.invoke(srcLang)
        }
    }

    var translation: Translation?
            by Delegates.observable(null) { _, oldValue: Translation?, newValue: Translation? ->
                if (oldValue !== newValue) {
                    update(newValue)
                }
            }

    var srcLang: Lang? by Delegates.observable(null) { _, oldValue: Lang?, newValue: Lang? ->
        if (oldValue !== newValue) {
            sourceLangComponent.updateLanguage(newValue)
            checkSourceLanguage()
        }
    }

    val component: JComponent by lazy {
        initFont()
        initColorScheme()
        initMaxSize(maxWidth)
        initActions()

        panel {
            sourceLangRow = row {
                originalTTSLink()
                sourceLangComponent()
                fixLanguageLink()
            }

            row { originalViewer(CCFlags.grow) }
            row { originalPhonetic(CCFlags.grow) }

            targetLangRow = row {
                transTTSLink()
                targetLangComponent()
            }

            row { transViewer(CCFlags.grow) }
            row { transPhonetic(CCFlags.grow) }
            row { dictViewer.component(CCFlags.grow) }
            row { otherExplainLabel() }
            row { otherExplainViewer(CCFlags.grow) }
        }
    }

    protected abstract fun onCreateLanguageComponent(): T

    private fun initFont() {
        getOverrideFonts(settings).let { (primaryFont, phoneticFont) ->
            sourceLangComponent.font = primaryFont
            targetLangComponent.font = primaryFont
            fixLanguageLink.font = primaryFont
            originalViewer.font = primaryFont.deriveFont(Font.ITALIC or Font.BOLD, FONT_SIZE_LARGE.toFloat())
            transViewer.font = primaryFont.deriveFont(FONT_SIZE_LARGE.toFloat())
            dictViewer.font = primaryFont
            otherExplainViewer.font = primaryFont
            otherExplainLabel.font = primaryFont
            originalPhonetic.font = phoneticFont
            transPhonetic.font = phoneticFont
        }
    }

    private fun initColorScheme() {
        originalViewer.foreground = JBColor(0xEE6000, 0xCC7832)
        transViewer.foreground = JBColor(0x170591, 0xFFC66D)
        originalPhonetic.foreground = JBColor(0xEEA985, 0xC79582)
        transPhonetic.foreground = JBColor(0xC79464, 0xCFBAA5)
        otherExplainLabel.foreground = JBColor(0x707070, 0x808080)

        fixLanguageLink.apply {
            setPaintUnderline(false)
            normalColor = JBColor(0xF00000, 0xFF0000)
            activeColor = JBColor(0xDD0000, 0xEE0000)
        }

        JBColor(0x3E7EFF, 0x8CBCE1).let {
            sourceLangComponent.foreground = it
            targetLangComponent.foreground = it
        }

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

    private fun initMaxSize(maxWidth: Int) {
        val maximumSize = JBDimension(maxWidth, Int.MAX_VALUE)

        originalViewer.maximumSize = maximumSize
        originalPhonetic.maximumSize = maximumSize
        transViewer.maximumSize = maximumSize
        transPhonetic.maximumSize = maximumSize
        dictViewer.component.maximumSize = maximumSize
        otherExplainLabel.maximumSize = maximumSize
        otherExplainViewer.maximumSize = maximumSize
    }

    private fun initActions() {
        otherExplainViewer.setupPopupMenu()
        originalViewer.apply {
            setupPopupMenu()
            setFocusListener(transViewer, dictViewer.component as Viewer)
        }
        transViewer.apply {
            setupPopupMenu()
            setFocusListener(originalViewer, dictViewer.component as Viewer)
        }
        dictViewer.apply {
            onEntryClicked {
                onNewTranslateHandler?.invoke(it.value)
            }
            onFoldingExpanded {
                onRevalidateHandler?.invoke()
            }
            (component as Viewer).setFocusListener(originalViewer, transViewer)
        }
    }

    private fun Viewer.setFocusListener(vararg vs: Viewer) {
        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                for (v in vs) {
                    v.select(0, 0)
                }
            }
        })
    }

    open fun reset() {
        srcLang = null
        translation = null
    }

    fun onNewTranslate(handler: (text: String) -> Unit) {
        onNewTranslateHandler = handler
    }

    fun onRevalidate(handler: () -> Unit) {
        onRevalidateHandler = handler
    }

    fun onTextToSpeech(handler: (text: String, lang: Lang) -> Unit) {
        onTextToSpeechHandler = handler
    }

    fun onFixLanguage(handler: (lang: Lang) -> Unit) {
        onFixLanguageHandler = handler
    }

    private fun JTextPane.setupPopupMenu() {
        componentPopupMenu = JBPopupMenu().apply {
            val copy = JBMenuItem("Copy", Icons.Copy).apply {
                addActionListener { copy() }
            }
            val translate = JBMenuItem("Translate", Icons.Translate).apply {
                addActionListener {
                    selectedText.let {
                        if (!it.isNullOrBlank()) {
                            onNewTranslateHandler?.invoke(it)
                        }
                    }
                }
            }

            add(copy)
            add(translate)
            addPopupMenuListener(object : PopupMenuListenerAdapter() {
                override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                    (!selectedText.isNullOrBlank()).let {
                        copy.isEnabled = it
                        translate.isEnabled = it
                    }
                }
            })
        }
    }

    private fun checkSourceLanguage() {
        if (srcLang != null && srcLang != Lang.AUTO && translation?.srcLang != srcLang) {
            fixLanguageLink.text = translation?.srcLang?.langName
        } else {
            fixLanguageLink.text = null
        }
    }

    protected abstract fun T.updateLanguage(lang: Lang?)

    private fun update(translation: Translation?) {
        component // initialize components
        checkSourceLanguage()
        with(translation) {
            if (this != null) {
                this@TranslationPanel.srcLang.let {
                    if (it == null || Lang.AUTO == it) {
                        sourceLangComponent.updateLanguage(srcLang)
                    }
                }
                targetLangComponent.updateLanguage(targetLang)

                sourceLangRow.visible = true
                targetLangRow.visible = true
                transTTSLink.isEnabled = trans != null

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
                targetLangComponent.updateLanguage(null)

                sourceLangRow.visible = false
                targetLangRow.visible = false

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
        private const val FONT_SIZE_LARGE = 18
        private const val FONT_SIZE_DEFAULT = 14
        private const val FONT_SIZE_PHONETIC = 12

        private const val EXPLAIN_KEY_STYLE = "explain_key"
        private const val EXPLAIN_VALUE_STYLE = "explain_value"

        private fun getOverrideFonts(settings: Settings): Pair<JBFont, JBFont> {
            var primaryFont: JBFont = UI.defaultFont.deriveFont(JBUI.scale(FONT_SIZE_DEFAULT.toFloat()))
            var phoneticFont: JBFont = UI.defaultFont.deriveFont(JBUI.scale(FONT_SIZE_PHONETIC.toFloat()))

            with(settings) {
                if (isOverrideFont) {
                    primaryFont = primaryFontFamily?.let { JBUI.Fonts.create(it, FONT_SIZE_DEFAULT) } ?: primaryFont
                    phoneticFont = phoneticFontFamily?.let { JBUI.Fonts.create(it, FONT_SIZE_PHONETIC) } ?: phoneticFont
                }
            }

            return primaryFont to phoneticFont
        }

        private fun ttsLinkLabel(action: (ActionLink) -> Unit): ActionLink = ActionLink(action = action).apply {
            icon = Icons.Speech
            setHoveringIcon(Icons.SpeechPressed)
        }
    }

}