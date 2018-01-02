package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Dict
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
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.util.containers.isNullOrEmpty
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
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
abstract class TranslationPanel<T : JComponent>(protected val settings: Settings) {

    protected val sourceLangComponent: T by lazy { onCreateLanguageComponent() }
    protected val targetLangComponent: T by lazy { onCreateLanguageComponent() }

    protected val originalViewer = Viewer()
    protected val transViewer = Viewer()
    private val originalPhonetic = JLabel()
    private val transPhonetic = JLabel()
    private val dictViewer = StyledDictViewer()
    private val otherExplainLabel = JLabel("网络释义:")
    private val otherExplainViewer = Viewer()

    private lateinit var sourceLangRow: Row
    private lateinit var targetLangRow: Row
    private lateinit var originalViewerRow: Row
    private lateinit var transViewerRow: Row
    private lateinit var dictViewerRow: Row
    private lateinit var otherExplainViewerRow: Row

    private var onNewTranslateHandler: ((String, Lang, Lang) -> Unit)? = null
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

    private val fixLanguageLinkLabel = JLabel("源语言: ")
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
        initMaxSize()
        initActions()

        panel {
            sourceLangRow = row {
                createRow(originalTTSLink, sourceLangComponent, fixLanguageLinkLabel, fixLanguageLink)()
            }
            originalViewerRow = row { onWrapViewer(originalViewer)(CCFlags.grow) }
            row { originalPhonetic(CCFlags.grow) }
            targetLangRow = row {
                createRow(transTTSLink, targetLangComponent).apply {
                    border = JBEmptyBorder(10, 0, 0, 0)
                }()
            }
            transViewerRow = row { onWrapViewer(transViewer)(CCFlags.grow) }
            row { transPhonetic(CCFlags.grow) }
            dictViewerRow = row {
                onWrapViewer(dictViewer.component as Viewer).apply {
                    border = JBEmptyBorder(10, 0, 0, 0)
                }(CCFlags.grow)
            }
            row { otherExplainLabel() }
            otherExplainViewerRow = row { onWrapViewer(otherExplainViewer)(CCFlags.grow) }
        }.apply { isOpaque = false /* 可使淡入淡出动画流畅自然 */ }
    }

    init {
        otherExplainLabel.border = JBEmptyBorder(10, 0, 0, 0)
        fixLanguageLinkLabel.border = JBEmptyBorder(0, 10, 0, 0)
        JBEmptyBorder(0, 0, 0, 5).let {
            originalTTSLink.border = it
            transTTSLink.border = it
        }
    }

    private fun createRow(vararg components: JComponent) // 默认的布局组件的间隔太大了，又不能改。。。
            : JPanel = NonOpaquePanel(FlowLayout(FlowLayout.LEFT, 0, 0))
            .apply {
                for (c in components) {
                    add(c)
                }
            }

    protected abstract fun onCreateLanguageComponent(): T

    protected open fun onWrapViewer(viewer: Viewer): JComponent = viewer

    private fun initFont() {
        getOverrideFonts(settings).let { (primaryFont, phoneticFont) ->
            sourceLangComponent.font = primaryFont
            targetLangComponent.font = primaryFont
            fixLanguageLinkLabel.font = primaryFont
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
        originalPhonetic.foreground = JBColor(
                Color(0xEE, 0x60, 0x00, 0xA0),
                Color(0xCC, 0x78, 0x32, 0xA0))
        transPhonetic.foreground = JBColor(
                Color(0x17, 0x05, 0x91, 0xA0),
                Color(0xFF, 0xC6, 0x6D, 0xA0))
        otherExplainLabel.foreground = JBColor(0x707070, 0x808080)
        fixLanguageLinkLabel.foreground = JBColor(0x666666, 0x909090)

        fixLanguageLink.apply {
            setPaintUnderline(false)
            normalColor = JBColor(0xF00000, 0xFF0000)
            activeColor = JBColor(0xA00000, 0xCC0000)
        }

        JBColor(0x555555, 0xACACAC).let {
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

    private fun initMaxSize() {
        val maximumSize = JBDimension(MAX_WIDTH, Int.MAX_VALUE)

        originalViewer.maximumSize = maximumSize
        originalPhonetic.maximumSize = maximumSize
        transViewer.maximumSize = maximumSize
        transPhonetic.maximumSize = maximumSize
        dictViewer.component.maximumSize = maximumSize
        otherExplainLabel.maximumSize = maximumSize
        otherExplainViewer.maximumSize = maximumSize
    }

    private fun initActions() {
        originalViewer.apply {
            setupPopupMenu()
            setFocusListener(transViewer, otherExplainViewer)
        }
        transViewer.apply {
            setupPopupMenu()
            setFocusListener(originalViewer, otherExplainViewer)
        }
        otherExplainViewer.apply {
            setupPopupMenu()
            setFocusListener(originalViewer, transViewer)
        }
        dictViewer.apply {
            onEntryClicked { entry ->
                translation?.run {
                    val src: Lang
                    val target: Lang
                    when (entry.entryType) {
                        StyledDictViewer.EntryType.WORD -> {
                            src = targetLang
                            target = srcLang
                        }
                        StyledDictViewer.EntryType.REVERSE_TRANSLATION -> {
                            src = srcLang
                            target = targetLang
                        }
                    }

                    onNewTranslateHandler?.invoke(entry.value, src, target)
                }
            }
            onFoldingExpanded {
                onRevalidateHandler?.invoke()
            }
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

    fun onNewTranslate(handler: (text: String, src: Lang, target: Lang) -> Unit) {
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
                    translation?.run {
                        selectedText.let {
                            if (translation != null && !it.isNullOrBlank()) {
                                onNewTranslateHandler?.invoke(it, targetLang, srcLang)
                            }
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
            val visible = translation?.srcLang?.langName.let {
                fixLanguageLink.text = it
                !it.isNullOrEmpty()
            }
            fixLanguageLinkLabel.isVisible = visible
            fixLanguageLink.isVisible = visible
        } else {
            fixLanguageLinkLabel.isVisible = false
            fixLanguageLink.isVisible = false
            fixLanguageLink.text = null
        }
    }

    protected abstract fun T.updateLanguage(lang: Lang?)

    private fun update(translation: Translation?) {
        component // initialize components
        checkSourceLanguage()
        with(translation) translation@ {
            if (this != null) {
                this@TranslationPanel.srcLang.let {
                    if (it == null || Lang.AUTO == it) {
                        sourceLangComponent.updateLanguage(srcLang)
                    }
                }
                targetLangComponent.updateLanguage(targetLang)

                sourceLangRow.visible = true
                targetLangRow.visible = true
                transTTSLink.isEnabled = !trans.isNullOrEmpty()

                updateViewer(originalViewer, originalViewerRow, original)
                updateViewer(transViewer, transViewerRow, trans)

                originalPhonetic.updateText(srcPhoneticSymbol)
                transPhonetic.updateText(transPhoneticSymbol)

                updateDictViewer(dictionaries)
                insertOtherExplain(otherExplain)
            } else {
                targetLangComponent.updateLanguage(null)

                sourceLangRow.visible = false
                targetLangRow.visible = false
                originalViewerRow.visible = false
                transViewerRow.visible = false
                dictViewerRow.visible = false
                otherExplainViewerRow.visible = false

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

    private fun updateViewer(viewer: Viewer, row: Row, text: String?) {
        with(viewer) {
            updateText(text)
            row.visible = isVisible
        }
    }

    private fun updateDictViewer(dictionaries: List<Dict>?) {
        with(dictViewer) {
            this.dictionaries = dictionaries
            (!dictionaries.isNullOrEmpty()).let {
                component.isVisible = it
                dictViewerRow.visible = it
            }
        }
    }

    private fun insertOtherExplain(explain: Map<String, String>) {
        with(otherExplainViewer) {
            styledDocument.clear()

            if (explain.isEmpty()) {
                isVisible = false
                otherExplainLabel.isVisible = false
                otherExplainViewerRow.visible = false
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
            otherExplainViewerRow.visible = true
        }
    }

    private fun Viewer.updateText(text: String?) {
        this.text = text
        isVisible = !text.isNullOrEmpty()
        caretPosition = 0
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
        const val MAX_WIDTH = 500

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
            icon = Icons.Audio
            disabledIcon = Icons.AudioDisabled
            setHoveringIcon(Icons.AudioPressed)
        }
    }

}