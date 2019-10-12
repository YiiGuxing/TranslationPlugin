package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.TTSSource.ORIGINAL
import cn.yiiguxing.plugin.translate.TTSSource.TRANSLATION
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.text.GoogleDictDocument
import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument
import cn.yiiguxing.plugin.translate.trans.text.setup
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.util.text.*
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import icons.Icons
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.lang.ref.WeakReference
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTextPane
import javax.swing.event.PopupMenuEvent
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import kotlin.properties.Delegates

abstract class TranslationPane<T : JComponent>(
    private val project: Project?,
    protected val settings: Settings
) : NonOpaquePanel(VerticalFlowLayout(JBUI.scale(GAP), JBUI.scale(GAP))), Disposable {

    protected lateinit var sourceLangComponent: T
        private set
    protected lateinit var targetLangComponent: T
        private set

    private val originalViewer = Viewer()
    private val translationViewer = Viewer()
    private val originalTransliterationLabel = JLabel()
    private val transliterationLabel = JLabel()
    private val dictViewer = StyledViewer()
    private val basicExplanationViewer = Viewer()
    private val otherExplanationLabel = JLabel(message("tip.label.webInterpretation"))
    private val otherExplanationViewer = Viewer()

    private lateinit var sourceLangRowComponent: JComponent
    private lateinit var fixLanguageComponent: JComponent
    private lateinit var targetRowComponent: JComponent
    private lateinit var originalComponent: JComponent
    private lateinit var translationComponent: JComponent
    private lateinit var dictComponent: JComponent
    private lateinit var basicExplanationComponent: JComponent
    private lateinit var otherExplanationComponent: JComponent

    private var onNewTranslateHandler: ((String, Lang, Lang) -> Unit)? = null
    @Suppress("SpellCheckingInspection")
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

    private val fixLanguageLabel = JLabel("${message("tip.label.sourceLanguage")}: ")
    private val fixLanguageLink = ActionLink {
        translation?.srclangs?.firstOrNull()?.let { lang -> onFixLanguageHandler?.invoke(lang) }
    }

    var translation: Translation?
            by Delegates.observable(null) { _, oldValue: Translation?, newValue: Translation? ->
                if (oldValue !== newValue) {
                    update()
                }
            }

    init {
        init()
    }

    private fun init() {
        sourceLangComponent = onCreateLanguageComponent()
        targetLangComponent = onCreateLanguageComponent()

        fixLanguageComponent = flow(fixLanguageLabel, fixLanguageLink)
        sourceLangRowComponent = spaceBetween(flow(originalTTSLink, sourceLangComponent), fixLanguageComponent)
        targetRowComponent = flow(transTTSLink, targetLangComponent)
        originalComponent = onWrapViewer(originalViewer)
        translationComponent = onWrapViewer(translationViewer)

        onRowCreated(sourceLangRowComponent)
        onRowCreated(originalComponent)
        onRowCreated(originalTransliterationLabel)
        onRowCreated(targetRowComponent)
        onRowCreated(translationComponent)
        onRowCreated(transliterationLabel)

        add(sourceLangRowComponent)
        add(originalComponent)
        add(originalTransliterationLabel)
        add(targetRowComponent)
        add(translationComponent)
        add(transliterationLabel)

        dictComponent = onWrapViewer(dictViewer)
        onRowCreated(dictComponent)
        add(dictComponent)

        basicExplanationComponent = onWrapViewer(basicExplanationViewer)
        otherExplanationComponent = onWrapViewer(otherExplanationViewer)

        onRowCreated(basicExplanationComponent)
        onRowCreated(otherExplanationLabel)
        onRowCreated(otherExplanationComponent)

        add(basicExplanationComponent)
        add(otherExplanationLabel)
        add(otherExplanationComponent)

        initFont()
        initColorScheme()
        initActions()

        minimumSize = Dimension(0, 0)
    }

    private fun createTTSButton(block: () -> Pair<String, Lang>?): TTSButton {
        val translationPanel = this
        return TTSButton().apply {
            project = translationPanel.project
            dataSource(block)
            Disposer.register(translationPanel, this)
        }
    }

    protected open fun onRowCreated(row: JComponent) {}

    protected abstract fun onCreateLanguageComponent(): T

    protected open fun onWrapViewer(viewer: Viewer): JComponent = viewer

    protected fun isOriginalOrTranslationViewer(viewer: Viewer): Boolean {
        return viewer === originalViewer || viewer === translationViewer
    }

    protected fun isDictViewer(viewer: Viewer): Boolean {
        return viewer === dictViewer
    }

    private fun initFont() {
        UI.getFonts(FONT_SIZE_DEFAULT, FONT_SIZE_PHONETIC).let { (primaryFont, phoneticFont) ->
            sourceLangComponent.font = primaryFont
            targetLangComponent.font = primaryFont
            fixLanguageLabel.font = primaryFont
            fixLanguageLink.font = primaryFont
            originalViewer.font = primaryFont.deriveScaledFont(Font.ITALIC or Font.BOLD, FONT_SIZE_LARGE)
            translationViewer.font = primaryFont.deriveScaledFont(FONT_SIZE_LARGE)
            dictViewer.font = primaryFont.biggerOn(1f)
            basicExplanationViewer.font = primaryFont.biggerOn(1f)
            otherExplanationViewer.font = primaryFont
            otherExplanationLabel.font = primaryFont
            originalTransliterationLabel.font = phoneticFont
            transliterationLabel.font = phoneticFont
        }
    }

    private fun initColorScheme() {
        originalViewer.foreground = JBColor(0xEE6000, 0xCC7832)
        translationViewer.foreground = JBColor(0x170591, 0xFFC66D)
        originalTransliterationLabel.foreground = JBColor(
            Color(0xEE, 0x60, 0x00, 0xA0),
            Color(0xCC, 0x78, 0x32, 0xA0)
        )
        transliterationLabel.foreground = JBColor(
            Color(0x17, 0x05, 0x91, 0xA0),
            Color(0xFF, 0xC6, 0x6D, 0xA0)
        )
        basicExplanationViewer.foreground = JBColor(0x2A237A, 0xFFDB89)
        otherExplanationLabel.foreground = JBColor(0x707070, 0x808080)
        fixLanguageLabel.foreground = JBColor(0x666666, 0x909090)

        fixLanguageLink.apply {
            setPaintUnderline(false)
            normalColor = JBColor(0xF00000, 0xFF0000)
            activeColor = JBColor(0xA00000, 0xCC0000)
        }

        JBColor(0x555555, 0xACACAC).let {
            sourceLangComponent.foreground = it
            targetLangComponent.foreground = it
        }

        with(otherExplanationViewer) {
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

    private fun initActions() {
        originalViewer.setupPopupMenu()
        originalViewer.setFocusListener(translationViewer, basicExplanationViewer, otherExplanationViewer)
        translationViewer.setupPopupMenu()
        translationViewer.setFocusListener(originalViewer, basicExplanationViewer, otherExplanationViewer)
        basicExplanationViewer.setupPopupMenu()
        basicExplanationViewer.setFocusListener(originalViewer, translationViewer, otherExplanationViewer)
        otherExplanationViewer.setupPopupMenu()
        otherExplanationViewer.setFocusListener(originalViewer, translationViewer, basicExplanationViewer)

        dictViewer.addPopupMenuItem("Copy", AllIcons.Actions.Copy) { _, element, _ ->
            CopyPasteManager.getInstance().setContents(StringSelection(element.text))
        }
        dictViewer.onClick { element, data ->
            translation?.run {
                val src: Lang
                val target: Lang
                when (data) {
                    GoogleDictDocument.EntryType.WORD -> {
                        src = targetLang
                        target = srcLang
                    }
                    GoogleDictDocument.EntryType.REVERSE_TRANSLATION -> {
                        src = srcLang
                        target = targetLang
                    }
                    else -> return@onClick
                }

                onNewTranslateHandler?.invoke(element.text, src, target)
            }
        }
        dictViewer.onBeforeFoldingExpand { _, _ ->
            onBeforeFoldingExpand()
        }
        dictViewer.onFoldingExpanded {
            onFoldingExpanded()
            onRevalidateHandler?.invoke()
        }

        originalTransliterationLabel.setupPopupMenu()
        transliterationLabel.setupPopupMenu()
    }

    protected open fun onBeforeFoldingExpand() {}

    protected open fun onFoldingExpanded() {}

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

    @Suppress("SpellCheckingInspection")
    fun onRevalidate(handler: () -> Unit) {
        onRevalidateHandler = handler
    }

    fun onFixLanguage(handler: (lang: Lang) -> Unit) {
        onFixLanguageHandler = handler
    }

    private fun JTextPane.setupPopupMenu() {
        componentPopupMenu = JBPopupMenu().apply {
            val copy = JBMenuItem("Copy", AllIcons.Actions.Copy).apply {
                disabledIcon = AllIcons.Actions.Copy
                addActionListener { copy() }
            }
            val translate = JBMenuItem("Translate", Icons.Translate).apply {
                disabledIcon = Icons.Translate
                addActionListener {
                    translation?.let { translation ->
                        selectedText.takeUnless { txt -> txt.isNullOrBlank() }?.let { selectedText ->
                            lateinit var src: Lang
                            lateinit var target: Lang
                            if (this@setupPopupMenu === originalViewer) {
                                src = translation.srcLang
                                target = translation.targetLang
                            } else {
                                src = translation.targetLang
                                target = translation.srcLang
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
        val copy = JBMenuItem("Copy", AllIcons.Actions.Copy)
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
            fixLanguageLabel.isVisible = visible
            fixLanguageLink.isVisible = visible
        } else {
            fixLanguageLabel.isVisible = false
            fixLanguageLink.isVisible = false
            fixLanguageLink.text = null
        }
    }

    protected abstract fun T.updateLanguage(lang: Lang?)

    protected abstract val originalFoldingLength: Int

    private fun update() {
        checkSourceLanguage()
        translation.let {
            if (it != null) {
                updateComponents(it)
                if (settings.autoPlayTTS) {
                    when (settings.ttsSource) {
                        ORIGINAL -> originalTTSLink
                        TRANSLATION -> transTTSLink
                    }.play()
                }
            } else {
                resetComponents()
                TextToSpeech.stop()
            }
        }
    }

    private fun updateComponents(translation: Translation) {
        sourceLangComponent.updateLanguage(translation.srcLang)
        targetLangComponent.updateLanguage(translation.targetLang)

        originalTTSLink.isEnabled = TextToSpeech.isSupportLanguage(translation.srcLang)
        transTTSLink.isEnabled =
            !translation.trans.isNullOrEmpty() && TextToSpeech.isSupportLanguage(translation.targetLang)

        updateOriginalViewer(translation)
        updateViewer(translationViewer, translationComponent, translation.trans)

        originalTransliterationLabel.apply {
            val srcTransliteration = translation.srcTransliteration
            updateText(srcTransliteration)
            toolTipText = srcTransliteration
        }
        transliterationLabel.apply {
            val transliteration = translation.transliteration
            updateText(transliteration)
            toolTipText = transliteration
        }

        updateDictViewer(translation.dictDocument)
        updateViewer(basicExplanationViewer, basicExplanationComponent, translation.basicExplains.joinToString("\n"))
        updateOtherExplains(translation.otherExplains)
    }

    private fun updateOriginalViewer(translation: Translation) {
        val text = translation.original
        val viewer = originalViewer
        if (text.isEmpty()) {
            viewer.empty()
            originalComponent.isVisible = false
            return
        }

        if (settings.foldOriginal && text.length > originalFoldingLength) {
            viewer.setFoldedText(text)
        } else {
            viewer.text = text
        }

        if (WordBookService.canAddToWordbook(text)) {
            viewer.appendStarButton(translation)
        }

        viewer.caretPosition = 0
        originalComponent.isVisible = true
    }

    private fun Viewer.appendStarButton(translation: Translation) {
        val starIcon = if (translation.favoriteId == null) Icons.StarOff else Icons.StarOn
        val starLabel = LinkLabel<Translation>("", starIcon, LinkListener { starLabel, trans ->
            starLabel.isEnabled = false
            val starLabelRef = WeakReference(starLabel)
            executeOnPooledThread {
                val favoriteId = trans.favoriteId
                if (favoriteId == null) {
                    val newFavoriteId = WordBookService.addWord(trans.toWordBookItem())
                    invokeLater {
                        if (trans.favoriteId == null) {
                            trans.favoriteId = newFavoriteId
                        }
                        starLabelRef.get()?.isEnabled = true
                    }
                } else {
                    WordBookService.removeWord(favoriteId)
                    invokeLater { starLabelRef.get()?.isEnabled = true }
                }
            }
        }, translation)
        starLabel.alignmentY = 0.9f
        starLabel.toolTipText = getStarButtonToolTipText(translation.favoriteId)
        translation.observableFavoriteId.observe(this@TranslationPane) { favoriteId, _ ->
            starLabel.icon = if (favoriteId == null) Icons.StarOff else Icons.StarOn
            starLabel.toolTipText = getStarButtonToolTipText(favoriteId)
        }

        val starAttribute = SimpleAttributeSet().also { StyleConstants.setComponent(it, starLabel) }
        styledDocument.appendString("  ").appendString(" ", starAttribute)
    }

    private fun Viewer.setFoldedText(text: String) {
        val foldedText = text.splitSentence(originalFoldingLength).first()
        setText(foldedText)

        val foldedLength = foldedText.length
        val foldingAttribute = SimpleAttributeSet()
        StyleConstants.setComponent(foldingAttribute, FoldingButton {
            styledDocument.replace(0, foldedLength + 2, text)
            caretPosition = 0
            onRevalidateHandler?.invoke()
        })
        styledDocument.appendString(" ").appendString(" ", foldingAttribute)
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
        sourceLangComponent.updateLanguage(null)
        targetLangComponent.updateLanguage(null)

        sourceLangRowComponent.isVisible = false
        targetRowComponent.isVisible = false
        fixLanguageComponent.isVisible = false
        originalComponent.isVisible = false
        translationComponent.isVisible = false
        dictComponent.isVisible = false
        basicExplanationComponent.isVisible = false
        otherExplanationComponent.isVisible = false

        originalViewer.empty()
        originalTransliterationLabel.empty()
        translationViewer.empty()
        transliterationLabel.empty()
        otherExplanationViewer.empty()

        otherExplanationLabel.isVisible = false
        updateDictViewer(null)
    }

    private fun updateViewer(viewer: Viewer, wrapper: JComponent, text: String?) {
        with(viewer) {
            updateText(text)
            wrapper.isVisible = isVisible
        }
    }

    private fun updateDictViewer(dictDocument: TranslationDocument?) {
        dictViewer.document.clear()
        if (dictDocument != null) {
            dictViewer.setup(dictDocument)
            dictViewer.isVisible = true
            dictComponent.isVisible = true
        } else {
            dictViewer.isVisible = false
            dictComponent.isVisible = false
        }
        dictViewer.caretPosition = 0
    }

    private fun updateOtherExplains(explains: Map<String, String>) {
        with(otherExplanationViewer) {
            styledDocument.clear()

            if (explains.isEmpty()) {
                isVisible = false
                otherExplanationLabel.isVisible = false
                otherExplanationComponent.isVisible = false
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
            otherExplanationLabel.isVisible = true
            otherExplanationComponent.isVisible = true
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
        const val GAP = 5

        private const val FONT_SIZE_LARGE = 18f
        private const val FONT_SIZE_DEFAULT = 14
        private const val FONT_SIZE_PHONETIC = 12

        private const val EXPLAIN_KEY_STYLE = "explain_key"
        private const val EXPLAIN_VALUE_STYLE = "explain_value"

        private fun flow(vararg components: JComponent): JComponent {
            val gap = JBUI.scale(GAP)
            val panel = NonOpaquePanel(FlowLayout(FlowLayout.LEFT, gap, 0))
            panel.border = JBUI.Borders.empty(0, -GAP, 0, 0)

            for (component in components) {
                panel.add(component)
            }

            return panel
        }

        private fun spaceBetween(left: JComponent, right: JComponent): JComponent {
            return BorderLayoutPanel()
                .andTransparent()
                .addToLeft(left)
                .addToRight(right)
        }

        @Suppress("InvalidBundleOrProperty")
        private fun getStarButtonToolTipText(favoriteId: Long?): String {
            return if (favoriteId == null) {
                message("tooltip.addToWordBook")
            } else {
                message("tooltip.removeFormWordBook")
            }
        }

        private fun Translation.toWordBookItem(): WordBookItem {
            val explainsBuilder = StringBuilder()
            val dictText = dictDocument?.text ?: ""

            if (!trans.isNullOrBlank()) {
                explainsBuilder.append(trans)
                if (dictText.isNotEmpty() || basicExplains.isNotEmpty()) {
                    explainsBuilder.append("\n\n")
                }
            }

            explainsBuilder.append(dictText)

            if (dictText.isNotEmpty() && basicExplains.isNotEmpty()) {
                explainsBuilder.append("\n\n")
            }

            basicExplains.joinTo(explainsBuilder, "\n")

            return WordBookItem(
                null,
                original,
                srcLang,
                targetLang,
                srcTransliteration,
                explainsBuilder.toString(),
                null
            )
        }
    }
}