package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Dict
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.icon.Icons
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.ide.CopyPasteManager
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
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Font
import java.awt.datatransfer.StringSelection
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.text.SimpleAttributeSet
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
    @Suppress("InvalidBundleOrProperty")
    private val otherExplainLabel = JLabel(message("tip.label.webInterpretation"))
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

    private val originalTTSLink = createTTSButton {
        translation?.run { original to srcLang }
    }

    private val transTTSLink = createTTSButton {
        translation?.run {
            trans?.let { it to targetLang }
        }
    }

    @Suppress("InvalidBundleOrProperty")
    private val fixLanguageLinkLabel = JLabel("${message("tip.label.sourceLanguage")}: ")
    private val fixLanguageLink = ActionLink {
        translation?.srclangs?.firstOrNull()?.let { lang -> onFixLanguageHandler?.invoke(lang) }
    }

    var translation: Translation?
            by Delegates.observable(null) { _, oldValue: Translation?, newValue: Translation? ->
                if (oldValue !== newValue) {
                    update()
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

    private fun createTTSButton(block: () -> Pair<String, Lang>?): TTSButton {
        val translationPanel = this
        return TTSButton().apply {
            project = translationPanel.project
            dataSource(block)
            Disposer.register(translationPanel, this)
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
            originalViewer.font = primaryFont.deriveScaledFont(Font.ITALIC or Font.BOLD, FONT_SIZE_LARGE)
            transViewer.font = primaryFont.deriveScaledFont(FONT_SIZE_LARGE)
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
                    invokeLater { value = lastScrollValue }
                }
                onRevalidateHandler?.invoke()
            }
        }

        srcTransliterationLabel.setupPopupMenu()
        transliterationLabel.setupPopupMenu()
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
    }

    open fun reset() {
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
                addActionListener { _ ->
                    translation?.let { translation ->
                        selectedText.takeUnless { it.isNullOrBlank() }?.let { selectedText ->
                            lateinit var src: Lang
                            lateinit var target: Lang
                            if (this@setupPopupMenu === originalViewer) {
                                src = translation.targetLang
                                target = translation.srcLang
                            } else {
                                src = translation.srcLang
                                target = translation.targetLang
                            }

                            onNewTranslateHandler?.invoke(selectedText, src, target)
                        }
                    }
                }
            }

            add(copy)
            add(translate)
            addPopupMenuListener(object : PopupMenuListenerAdapter() {
                override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                    val hasSelectedText = !selectedText.isNullOrBlank()
                    copy.isEnabled = hasSelectedText
                    translate.isEnabled = hasSelectedText
                }
            })
        }
    }

    private fun JLabel.setupPopupMenu() {
        val copy = JBMenuItem("Copy", Icons.Copy)
        copy.addActionListener { CopyPasteManager.getInstance().setContents(StringSelection(text)) }
        componentPopupMenu = JBPopupMenu().apply { add(copy) }
    }

    private fun checkSourceLanguage() {
        val translation = translation
        if (translation != null && !translation.srclangs.contains(translation.srcLang)) {
            val visible = translation.srclangs.firstOrNull()?.langName.let {
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

    protected abstract val originalFoldingLength: Int

    private fun update() {
        component // initialize components
        checkSourceLanguage()
        translation?.let { updateComponents(it) } ?: resetComponents()
    }

    private fun updateComponents(translation: Translation) {
        translation.let {
            sourceLangComponent.updateLanguage(it.srcLang)
            targetLangComponent.updateLanguage(it.targetLang)

            sourceLangRow.visible = true
            targetLangRow.visible = true

            originalTTSLink.isEnabled = TextToSpeech.isSupportLanguage(it.srcLang)
            transTTSLink.isEnabled = !it.trans.isNullOrEmpty() && TextToSpeech.isSupportLanguage(it.targetLang)

            updateOriginalViewer(it.original)
            updateViewer(transViewer, transViewerRow, it.trans)

            srcTransliterationLabel.apply {
                val srcTransliteration = it.srcTransliteration
                updateText(srcTransliteration)
                toolTipText = srcTransliteration
            }
            transliterationLabel.apply {
                val transliteration = it.transliteration
                updateText(transliteration)
                toolTipText = transliteration
            }

            updateDictViewer(it.dictionaries)
            updateViewer(basicExplainViewer, basicExplainsViewerRow, it.basicExplains.joinToString("\n"))
            updateOtherExplains(it.otherExplains)
        }
    }

    private fun updateOriginalViewer(text: String) {
        if (text.isEmpty()) {
            originalViewer.empty()
            originalViewerRow.visible = false
            return
        }

        with(originalViewer) {
            if (settings.foldOriginal && text.length > originalFoldingLength) {
                val display = text.splitSentence(originalFoldingLength).first()
                setText(display)

                val attr = SimpleAttributeSet()
                StyleConstants.setComponent(attr, FoldingButton {
                    setText(text)
                    caretPosition = 0
                    onRevalidateHandler?.invoke()
                })
                styledDocument.appendString(" ").appendString(" ", attr)
            } else {
                setText(text)
            }

            caretPosition = 0
        }
        originalViewerRow.visible = true
    }

    private class FoldingButton(private val action: () -> Unit) : JButton("..."), MouseListener {

        init {
            isOpaque = false
            alignmentY = 0.8f

            JBDimension(35, 23).let {
                minimumSize = it
                maximumSize = it
                preferredSize = it
            }

            foreground = JBColor(0x555555, 0xb2b2b2)
            addMouseListener(this)
        }

        override fun mouseReleased(e: MouseEvent) {}
        override fun mousePressed(e: MouseEvent) {}

        override fun mouseClicked(e: MouseEvent) {
            action()
        }

        override fun mouseEntered(e: MouseEvent) {
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }

        override fun mouseExited(e: MouseEvent) {
            cursor = Cursor.getDefaultCursor()
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

        private const val FONT_SIZE_LARGE = 18f
        private const val FONT_SIZE_DEFAULT = 14f
        private const val FONT_SIZE_PHONETIC = 12f

        private const val EXPLAIN_KEY_STYLE = "explain_key"
        private const val EXPLAIN_VALUE_STYLE = "explain_value"

        private fun getOverrideFonts(settings: Settings): Pair<JBFont, JBFont> {
            var primaryFont: JBFont = UI.defaultFont.deriveScaledFont(FONT_SIZE_DEFAULT)
            var phoneticFont: JBFont = UI.defaultFont.deriveScaledFont(FONT_SIZE_PHONETIC)

            with(settings) {
                if (isOverrideFont) {
                    primaryFont = primaryFontFamily
                            ?.let { JBUI.Fonts.create(it, FONT_SIZE_DEFAULT.toInt()) }
                            ?: primaryFont
                    phoneticFont = phoneticFontFamily
                            ?.let { JBUI.Fonts.create(it, FONT_SIZE_PHONETIC.toInt()) }
                            ?: phoneticFont
                }
            }

            return primaryFont to phoneticFont
        }
    }
}