package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Dict
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.tts.TextToSpeech
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.Disposer
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
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import kotlin.properties.Delegates

/**
 * TranslationPanel
 *
 * Created by Yii.Guxing on 2017/12/10
 */
abstract class TranslationPanel<T : JComponent>(
        private val project: Project?,
        protected val settings: Settings,
        private val maxWidth: Int = MAX_WIDTH
) : Disposable {

    protected val sourceLangComponent: T by lazy { onCreateLanguageComponent() }
    protected val targetLangComponent: T by lazy { onCreateLanguageComponent() }

    protected val originalViewer = Viewer()
    protected val transViewer = Viewer()
    private val srcTransliterationLabel = JLabel()
    private val transliterationLabel = JLabel()
    private val dictViewer = StyledDictViewer()
    private val basicExplainViewer = Viewer()
    private val otherExplainLabel = JLabel("网络释义:")
    private val otherExplainViewer = Viewer()

    private var dictViewerScrollWrapper: JScrollPane? = null
    private var lastScrollValue: Int = 0

    private lateinit var sourceLangRow: Row
    private lateinit var targetLangRow: Row
    private lateinit var originalViewerRow: Row
    private lateinit var transViewerRow: Row
    private lateinit var dictViewerRow: Row
    private lateinit var basicExplainsViewerRow: Row
    private lateinit var otherExplainsViewerRow: Row

    private var onNewTranslateHandler: ((String, Lang, Lang) -> Unit)? = null
    private var onRevalidateHandler: (() -> Unit)? = null
    private var onFixLanguageHandler: ((Lang) -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech.instance
    private var ttsDisposable: Disposable? = null

    private val originalTTSLink = createTTSLinkLabel {
        translation?.run { original to srcLang }
    }

    private val transTTSLink = createTTSLinkLabel {
        translation?.run {
            trans?.let { it to targetLang }
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
                    update()
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
            row { srcTransliterationLabel(CCFlags.grow) }

            targetLangRow = row {
                createRow(transTTSLink, targetLangComponent).apply {
                    border = JBEmptyBorder(10, 0, 0, 0)
                }()
            }

            transViewerRow = row { onWrapViewer(transViewer)(CCFlags.grow) }
            row { transliterationLabel(CCFlags.grow) }

            val wrapped = onWrapViewer(dictViewer.component as Viewer).apply {
                border = JBEmptyBorder(10, 0, 0, 0)
            }
            dictViewerScrollWrapper = (wrapped as? JScrollPane)?.apply {
                verticalScrollBar.addAdjustmentListener { lastScrollValue = it.value }
            }
            dictViewerRow = row { wrapped(CCFlags.grow) }

            basicExplainsViewerRow = row {
                onWrapViewer(basicExplainViewer).apply {
                    border = JBEmptyBorder(10, 0, 0, 0)
                }(CCFlags.grow)
            }

            row { otherExplainLabel() }
            otherExplainsViewerRow = row { onWrapViewer(otherExplainViewer)(CCFlags.grow) }
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

    private fun createTTSLinkLabel(block: () -> Pair<String, Lang>?): ActionLink {
        var myDisposable: Disposable? = null
        return ActionLink { link ->
            block()?.let block@ { (text, lang) ->
                ttsDisposable?.let {
                    Disposer.dispose(it)
                    if (it === myDisposable) {
                        return@block
                    }
                }

                link.icon = Icons.TTSSuspend
                link.setHoveringIcon(Icons.TTSSuspendHovering)
                tts.speak(project, text, lang).let { disposable ->
                    myDisposable = disposable
                    ttsDisposable = disposable
                    Disposer.register(disposable, Disposable {
                        if (ttsDisposable === disposable) {
                            ttsDisposable = null
                        }
                        link.icon = Icons.Audio
                        link.setHoveringIcon(Icons.AudioPressed)
                    })
                }
            }
        }.apply {
            icon = Icons.Audio
            disabledIcon = Icons.AudioDisabled
            setHoveringIcon(Icons.AudioPressed)
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
            dictViewer.font = primaryFont.biggerOn(1f)
            basicExplainViewer.font = primaryFont.biggerOn(1f)
            otherExplainViewer.font = primaryFont
            otherExplainLabel.font = primaryFont
            srcTransliterationLabel.font = phoneticFont
            transliterationLabel.font = phoneticFont
        }
    }

    private fun initColorScheme() {
        originalViewer.foreground = JBColor(0xEE6000, 0xCC7832)
        transViewer.foreground = JBColor(0x170591, 0xFFC66D)
        srcTransliterationLabel.foreground = JBColor(
                Color(0xEE, 0x60, 0x00, 0xA0),
                Color(0xCC, 0x78, 0x32, 0xA0))
        transliterationLabel.foreground = JBColor(
                Color(0x17, 0x05, 0x91, 0xA0),
                Color(0xFF, 0xC6, 0x6D, 0xA0))
        basicExplainViewer.foreground = JBColor(0x2A237A, 0xFFDB89)
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
        val maximumSize = JBDimension(maxWidth, Int.MAX_VALUE)

        originalViewer.maximumSize = maximumSize
        srcTransliterationLabel.maximumSize = maximumSize
        transViewer.maximumSize = maximumSize
        transliterationLabel.maximumSize = maximumSize
        dictViewer.component.maximumSize = maximumSize
        otherExplainLabel.maximumSize = maximumSize
        otherExplainViewer.maximumSize = maximumSize
    }

    private fun initActions() {
        originalViewer.apply {
            setupPopupMenu()
            setFocusListener(transViewer, basicExplainViewer, otherExplainViewer)
        }
        transViewer.apply {
            setupPopupMenu()
            setFocusListener(originalViewer, basicExplainViewer, otherExplainViewer)
        }
        basicExplainViewer.apply {
            setupPopupMenu()
            setFocusListener(originalViewer, transViewer, otherExplainViewer)
        }
        otherExplainViewer.apply {
            setupPopupMenu()
            setFocusListener(originalViewer, transViewer, basicExplainViewer)
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
                dictViewerScrollWrapper?.verticalScrollBar?.run {
                    lastScrollValue.let {
                        invokeLater { value = it }
                    }
                }
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

    override fun dispose() {
        reset()
        ttsDisposable?.let { Disposer.dispose(it) }
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

    fun onFixLanguage(handler: (lang: Lang) -> Unit) {
        onFixLanguageHandler = handler
    }

    private fun JTextPane.setupPopupMenu() {
        componentPopupMenu = JBPopupMenu().apply {
            val copy = JBMenuItem("Copy", Icons.Copy).apply {
                disabledIcon = Icons.Copy
                addActionListener { copy() }
            }
            val translate = JBMenuItem("Translate", Icons.Translate).apply {
                disabledIcon = Icons.Translate
                addActionListener {
                    translation?.run {
                        selectedText.let {
                            if (!it.isNullOrBlank()) {
                                onNewTranslateHandler?.invoke(it, srcLang, targetLang)
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

    private fun update() {
        component // initialize components
        checkSourceLanguage()
        translation?.let { updateComponents(it) } ?: resetComponents()
    }

    private fun updateComponents(translation: Translation) {
        val updateSrcLang = this.srcLang.let { null == it || Lang.AUTO == it }
        with(translation) {
            if (updateSrcLang) {
                sourceLangComponent.updateLanguage(srcLang)
            }
            targetLangComponent.updateLanguage(targetLang)

            sourceLangRow.visible = true
            targetLangRow.visible = true

            originalTTSLink.isEnabled = tts.isSupportLanguage(srcLang)
            transTTSLink.isEnabled = !trans.isNullOrEmpty() && tts.isSupportLanguage(targetLang)

            updateViewer(originalViewer, originalViewerRow, original)
            updateViewer(transViewer, transViewerRow, trans)

            srcTransliterationLabel.updateText(srcTransliteration)
            transliterationLabel.updateText(transliteration)

            updateDictViewer(dictionaries)
            updateViewer(basicExplainViewer, basicExplainsViewerRow, basicExplains.joinToString("\n"))
            updateOtherExplains(otherExplains)
        }
    }

    private fun resetComponents() {
        targetLangComponent.updateLanguage(null)

        sourceLangRow.visible = false
        targetLangRow.visible = false
        originalViewerRow.visible = false
        transViewerRow.visible = false
        dictViewerRow.visible = false
        basicExplainsViewerRow.visible = false
        otherExplainsViewerRow.visible = false

        originalViewer.empty()
        srcTransliterationLabel.empty()
        transViewer.empty()
        transliterationLabel.empty()
        otherExplainViewer.empty()

        otherExplainLabel.isVisible = false
        dictViewer.component.isVisible = false
        dictViewer.dictionaries = null
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

    private fun updateOtherExplains(explains: Map<String, String>) {
        with(otherExplainViewer) {
            styledDocument.clear()

            if (explains.isEmpty()) {
                isVisible = false
                otherExplainLabel.isVisible = false
                otherExplainsViewerRow.visible = false
                return
            }

            styledDocument.apply {
                val keyStyle = getStyle(EXPLAIN_KEY_STYLE)
                val valueStyle = getStyle(EXPLAIN_VALUE_STYLE)

                val lastIndex = explains.size - 1
                var index = 0
                for ((key, value) in explains) {
                    appendString(key, keyStyle)
                    appendString(" - ")
                    appendString(value, valueStyle)
                    if (index++ < lastIndex) {
                        appendString("\n")
                    }
                }
            }

            caretPosition = 0
            isVisible = true
            otherExplainLabel.isVisible = true
            otherExplainsViewerRow.visible = true
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
    }
}