package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.TTSSource.ORIGINAL
import cn.yiiguxing.plugin.translate.TTSSource.TRANSLATION
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.text.append
import cn.yiiguxing.plugin.translate.trans.text.apply
import cn.yiiguxing.plugin.translate.ui.StyledViewer.Companion.setupActions
import cn.yiiguxing.plugin.translate.ui.UI.disabled
import cn.yiiguxing.plugin.translate.ui.util.ScrollSynchronizer
import cn.yiiguxing.plugin.translate.util.TextToSpeech
import cn.yiiguxing.plugin.translate.util.WordBookService
import cn.yiiguxing.plugin.translate.util.splitSentence
import cn.yiiguxing.plugin.translate.util.text.appendString
import cn.yiiguxing.plugin.translate.util.text.clear
import cn.yiiguxing.plugin.translate.util.text.replace
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
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import icons.TranslationIcons
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
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
    private val originalTransliterationLabel = TransliterationLabel()
    private val transliterationLabel = TransliterationLabel()
    private val extraViewer = StyledViewer().apply {
        dragEnabled = false
        disableSelection()
    }
    private val spellComponent: SpellComponent = SpellComponent()

    private lateinit var sourceLangRowComponent: JComponent
    private lateinit var fixLanguageComponent: JComponent
    private lateinit var targetRowComponent: JComponent
    private lateinit var originalComponent: JComponent
    private lateinit var translationComponent: JComponent
    private lateinit var extraComponent: JComponent

    private var onNewTranslateHandler: ((String, Lang, Lang) -> Unit)? = null

    private var onSpellFixedHandler: ((String) -> Unit)? = null

    @Suppress("SpellCheckingInspection")
    private var onRevalidateHandler: (() -> Unit)? = null
    private var onFixLanguageHandler: ((Lang) -> Unit)? = null

    private val originalTTSLink = createTTSButton {
        translation?.run { original to srcLang }
    }
    private val transTTSLink = createTTSButton {
        translation?.run {
            translation?.let { it to targetLang }
        }
    }

    private val fixLanguageLabel = JLabel("${message("tip.label.sourceLanguage")}: ")
    private val fixLanguageLink = ActionLink {
        translation?.sourceLanguages?.firstOrNull()?.let { lang -> onFixLanguageHandler?.invoke(lang) }
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
        spellComponent.spell = translation?.spell
        sourceLangRowComponent = spaceBetween(flow(originalTTSLink, sourceLangComponent), fixLanguageComponent)
        targetRowComponent = flow(transTTSLink, targetLangComponent)
        originalComponent = onWrapViewer(originalViewer)
        translationComponent = onWrapViewer(translationViewer)

        if (originalComponent is JScrollPane && translationComponent is JScrollPane) {
            ScrollSynchronizer.syncScroll(
                (originalComponent as JScrollPane).verticalScrollBar,
                (translationComponent as JScrollPane).verticalScrollBar
            )
        }

        onRowCreated(sourceLangRowComponent)
        onRowCreated(originalComponent)
        onRowCreated(spellComponent)
        onRowCreated(originalTransliterationLabel)
        onRowCreated(targetRowComponent)
        onRowCreated(translationComponent)
        onRowCreated(transliterationLabel)

        add(sourceLangRowComponent)
        add(originalComponent)
        add(spellComponent)
        add(originalTransliterationLabel)
        add(targetRowComponent)
        add(translationComponent)
        add(transliterationLabel)

        extraComponent = onWrapViewer(extraViewer)
        extraComponent.border = TOP_MARGIN_BORDER
        onRowCreated(extraComponent)
        add(extraComponent)

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
        return viewer === extraViewer
    }

    private fun initFont() {
        UI.getFonts(FONT_SIZE_DEFAULT, FONT_SIZE_PHONETIC).let { (primaryFont, phoneticFont) ->
            sourceLangComponent.font = primaryFont
            targetLangComponent.font = primaryFont
            fixLanguageLabel.font = primaryFont
            fixLanguageLink.font = primaryFont
            spellComponent.apply {
                spellLabel.font = primaryFont.lessOn(2f)
                spellText.font = primaryFont.deriveFont(Font.BOLD, spellLabel.font.size.toFloat())
            }
            originalViewer.font = primaryFont.deriveScaledFont(Font.ITALIC or Font.BOLD, FONT_SIZE_LARGE)
            translationViewer.font = primaryFont.deriveScaledFont(FONT_SIZE_LARGE)
            extraViewer.font = primaryFont.biggerOn(1f)
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
        fixLanguageLabel.foreground = JBColor(0x666666, 0x909090)

        fixLanguageLink.apply {
            setPaintUnderline(false)
            normalColor = JBColor(0xF00000, 0xFF0000)
            activeColor = JBColor(0xA00000, 0xCC0000)
        }

        spellComponent.spellLabel.foreground = JBColor(0x666666, 0x909090)
        spellComponent.spellText.apply {
            setPaintUnderline(false)
            normalColor = JBColor(0x4285F4, 0x2196F3)
            activeColor = JBColor(0x3B65CA, 0x03A9F4)
        }

        JBColor(0x555555, 0xACACAC).let {
            sourceLangComponent.foreground = it
            targetLangComponent.foreground = it
        }
    }

    private fun initActions() {
        originalViewer.setupPopupMenu()
        originalViewer.setFocusListener(translationViewer, extraViewer)
        translationViewer.setupPopupMenu()
        translationViewer.setFocusListener(originalViewer, extraViewer)
        extraViewer.setFocusListener(originalViewer, translationViewer)

        extraViewer.setupActions(::translation) { text: String, srcLang: Lang, targetLang: Lang ->
            onNewTranslateHandler?.invoke(text, srcLang, targetLang)
        }
        extraViewer.onBeforeFoldingExpand { _, _ ->
            onBeforeFoldingExpand()
        }
        extraViewer.onFoldingExpanded {
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

    fun onSpellFixed(handler: (spell: String) -> Unit) {
        spellComponent.onSpellFixed(handler)
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
            val copy = JBMenuItem(message("menu.item.copy"), AllIcons.Actions.Copy).apply {
                disabledIcon = AllIcons.Actions.Copy.disabled()
                addActionListener { copy() }
            }
            val translate = JBMenuItem(message("menu.item.translate"), TranslationIcons.Translation).apply {
                disabledIcon = TranslationIcons.Translation.disabled()
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
            val copyAll = JBMenuItem(message("menu.item.copy.all"), AllIcons.Actions.Copy).apply {
                disabledIcon = AllIcons.Actions.Copy.disabled()
                addActionListener {
                    CopyPasteManager.getInstance().setContents(StringSelection(this@setupPopupMenu.text))
                }
            }

            add(translate)
            add(copy)
            add(copyAll)
            addPopupMenuListener(object : PopupMenuListenerAdapter() {
                override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                    val hasSelectedText = !selectedText.isNullOrBlank()
                    copy.isEnabled = hasSelectedText
                    translate.isEnabled = hasSelectedText
                    copyAll.isEnabled = !this@setupPopupMenu.text.isNullOrBlank()
                }
            })
        }
    }

    private fun JLabel.setupPopupMenu() {
        val copy = JBMenuItem(message("menu.item.copy"), AllIcons.Actions.Copy)
        copy.addActionListener { CopyPasteManager.getInstance().setContents(StringSelection(text)) }
        componentPopupMenu = JBPopupMenu().apply { add(copy) }
    }

    private fun checkSourceLanguage() {
        val translation = translation
        val sourceLanguages = translation?.sourceLanguages
        if (sourceLanguages != null && !sourceLanguages.contains(translation.srcLang)) {
            val visible = sourceLanguages.firstOrNull()?.langName.let {
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
        sourceLangComponent.updateLanguage(translation.srcLang.takeIf { it != Lang.UNKNOWN } ?: Lang.AUTO)
        targetLangComponent.updateLanguage(translation.targetLang)

        originalTTSLink.isEnabled = TextToSpeech.isSupportLanguage(translation.srcLang)
        transTTSLink.isEnabled =
            !translation.translation.isNullOrEmpty() && TextToSpeech.isSupportLanguage(translation.targetLang)

        updateOriginalViewer(translation)
        spellComponent.spell = translation.spell
        updateViewer(translationViewer, translationComponent, translation.translation)

        originalTransliterationLabel.apply {
            val srcTransliteration = translation.srcTransliteration
            updateText(srcTransliteration)
        }
        transliterationLabel.apply {
            val transliteration = translation.transliteration
            updateText(transliteration)
        }

        updateExtraViewer(translation)
    }

    private fun updateOriginalViewer(translation: Translation) {
        val text = translation.original
        val viewer = originalViewer
        if (text.isEmpty()) {
            viewer.empty()
            originalComponent.isVisible = false
            return
        }

        if (settings.foldOriginal && (text.length > originalFoldingLength || text.count { it == '\n' } >= 3)) {
            viewer.setFoldedText(text)
        } else {
            viewer.text = text
        }

        if ((project != null || WordBookService.isInitialized) && WordBookService.canAddToWordbook(text)) {
            viewer.appendStarButton(translation)
        }

        viewer.caretPosition = 0
        originalComponent.isVisible = true
    }

    @Suppress("DuplicatedCode")
    private fun Viewer.appendStarButton(translation: Translation) {
        val starIcon = if (translation.favoriteId == null) TranslationIcons.StarOff else TranslationIcons.StarOn
        val starLabel = LinkLabel("", starIcon, StarButtons.listener, translation)
        starLabel.alignmentY = 0.9f
        starLabel.toolTipText = getStarButtonToolTipText(translation.favoriteId)
        translation.observableFavoriteId.observe(this@TranslationPane) { favoriteId, _ ->
            starLabel.icon = if (favoriteId == null) TranslationIcons.StarOff else TranslationIcons.StarOn
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
        spellComponent.isVisible = false
        originalComponent.isVisible = false
        translationComponent.isVisible = false
        extraComponent.isVisible = false

        originalViewer.empty()
        originalTransliterationLabel.empty()
        translationViewer.empty()
        transliterationLabel.empty()

        updateExtraViewer(null)
    }

    private fun updateViewer(viewer: Viewer, wrapper: JComponent, text: String?) {
        with(viewer) {
            updateText(text)
            wrapper.isVisible = isVisible
        }
    }

    private fun updateExtraViewer(translation: Translation?) {
        val viewer = extraViewer
        viewer.document.clear()

        var hasExtraContent = false
        if (translation != null) {
            translation.dictDocument?.let {
                hasExtraContent = true
                viewer.apply(it)
            }
            for (extraDocument in translation.extraDocuments) {
                hasExtraContent = true
                viewer.append(extraDocument)
            }
        }

        viewer.caretPosition = 0
        viewer.isVisible = hasExtraContent
        extraComponent.isVisible = hasExtraContent
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

        private val TOP_MARGIN_BORDER = JBUI.Borders.emptyTop(8)

        private fun flow(vararg components: JComponent): JComponent {
            val gap = JBUI.scale(GAP)
            val panel = NonOpaquePanel(FlowLayout(FlowLayout.LEFT, gap, 0))
            panel.border = JBUI.Borders.emptyLeft(-GAP)

            for (component in components) {
                panel.add(component)
            }

            return panel
        }

        @Suppress("unused")
        private fun flow2(left: JComponent, right: JComponent): JComponent {
            return BorderLayoutPanel()
                .andTransparent()
                .addToLeft(left)
                .addToCenter(right)
        }

        private fun spaceBetween(left: JComponent, right: JComponent): JComponent {
            return BorderLayoutPanel()
                .andTransparent()
                .addToLeft(left)
                .addToRight(right)
        }

        private fun getStarButtonToolTipText(favoriteId: Long?): String {
            return if (favoriteId == null) {
                message("tooltip.addToWordBook")
            } else {
                message("tooltip.removeFromWordBook")
            }
        }
    }
}