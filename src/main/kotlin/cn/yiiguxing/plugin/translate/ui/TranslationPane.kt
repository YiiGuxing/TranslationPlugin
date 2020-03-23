package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.TTSSource.ORIGINAL
import cn.yiiguxing.plugin.translate.TTSSource.TRANSLATION
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.text.*
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.util.text.appendString
import cn.yiiguxing.plugin.translate.util.text.clear
import cn.yiiguxing.plugin.translate.util.text.replace
import cn.yiiguxing.plugin.translate.util.text.text
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
    private val originalTransliterationLabel = JLabel()
    private val transliterationLabel = JLabel()
    private val dictViewer = StyledViewer()
    private val extraLabel = JLabel()
    private val extraViewer = StyledViewer()

    private lateinit var sourceLangRowComponent: JComponent
    private lateinit var fixLanguageComponent: JComponent
    private lateinit var spellComponent: JComponent
    private lateinit var targetRowComponent: JComponent
    private lateinit var originalComponent: JComponent
    private lateinit var translationComponent: JComponent
    private lateinit var dictComponent: JComponent
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
        translation?.srclangs?.firstOrNull()?.let { lang -> onFixLanguageHandler?.invoke(lang) }
    }

    private val spellLabel = JLabel(
        message("translation.ui.pane.label.spell"),
        Icons.AutoAwesome,
        SwingConstants.LEADING
    )
    private val spellText = ActionLink("是不是Are you OK?") {
        val handler = onSpellFixedHandler ?: return@ActionLink
        translation?.spell?.let { handler(it) }
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
        spellComponent = flow2(spellLabel, spellText)
        sourceLangRowComponent = spaceBetween(flow(originalTTSLink, sourceLangComponent), fixLanguageComponent)
        targetRowComponent = flow(transTTSLink, targetLangComponent)
        originalComponent = onWrapViewer(originalViewer)
        translationComponent = onWrapViewer(translationViewer)

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

        dictComponent = onWrapViewer(dictViewer)
        dictComponent.border = TOP_MARGIN_BORDER
        onRowCreated(dictComponent)
        add(dictComponent)

        extraComponent = onWrapViewer(extraViewer)

        extraLabel.border = TOP_MARGIN_BORDER
        onRowCreated(extraLabel)
        onRowCreated(extraComponent)

        add(extraLabel)
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
        return viewer === dictViewer
    }

    private fun initFont() {
        UI.getFonts(FONT_SIZE_DEFAULT, FONT_SIZE_PHONETIC).let { (primaryFont, phoneticFont) ->
            sourceLangComponent.font = primaryFont
            targetLangComponent.font = primaryFont
            fixLanguageLabel.font = primaryFont
            fixLanguageLink.font = primaryFont
            spellLabel.font = primaryFont.lessOn(2f)
            spellText.font = primaryFont.deriveFont(Font.BOLD or Font.ITALIC, spellLabel.font.size.toFloat())
            originalViewer.font = primaryFont.deriveScaledFont(Font.ITALIC or Font.BOLD, FONT_SIZE_LARGE)
            translationViewer.font = primaryFont.deriveScaledFont(FONT_SIZE_LARGE)
            dictViewer.font = primaryFont.biggerOn(1f)
            extraViewer.font = primaryFont
            extraLabel.font = primaryFont
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
        extraLabel.foreground = JBColor(0x535F53, 0xA9B7C6)
        fixLanguageLabel.foreground = JBColor(0x666666, 0x909090)

        fixLanguageLink.apply {
            setPaintUnderline(false)
            normalColor = JBColor(0xF00000, 0xFF0000)
            activeColor = JBColor(0xA00000, 0xCC0000)
        }

        spellLabel.foreground = JBColor(0x666666, 0x909090)
        spellText.apply {
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
        originalViewer.setFocusListener(translationViewer, dictViewer, extraViewer)
        translationViewer.setupPopupMenu()
        translationViewer.setFocusListener(originalViewer, dictViewer, extraViewer)
        dictViewer.setFocusListener(originalViewer, translationViewer, extraViewer)
        extraViewer.setFocusListener(originalViewer, translationViewer, dictViewer)

        dictViewer.setupActions()
        dictViewer.onBeforeFoldingExpand { _, _ ->
            onBeforeFoldingExpand()
        }
        dictViewer.onFoldingExpanded {
            onFoldingExpanded()
            onRevalidateHandler?.invoke()
        }

        extraViewer.setupActions()

        originalTransliterationLabel.setupPopupMenu()
        transliterationLabel.setupPopupMenu()
    }

    private fun StyledViewer.setupActions() {
        addPopupMenuItem(message("menu.item.copy"), AllIcons.Actions.Copy) { _, element, _ ->
            CopyPasteManager.getInstance().setContents(StringSelection(element.text))
        }
        onClick { element, data ->
            translation?.run {
                val src: Lang
                val target: Lang
                when (data) {
                    GoogleDictDocument.WordType.WORD,
                    YoudaoDictDocument.WordType.WORD,
                    YoudaoWebTranslationDocument.WordType.WEB_VALUE -> {
                        src = targetLang
                        target = srcLang
                    }
                    GoogleDictDocument.WordType.REVERSE,
                    YoudaoDictDocument.WordType.VARIANT,
                    YoudaoWebTranslationDocument.WordType.WEB_KEY -> {
                        src = srcLang
                        target = targetLang
                    }
                    else -> return@onClick
                }

                onNewTranslateHandler?.invoke(element.text, src, target)
            }
        }
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
        onSpellFixedHandler = handler
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
                disabledIcon = AllIcons.Actions.Copy
                addActionListener { copy() }
            }
            val translate = JBMenuItem(message("menu.item.translate"), Icons.Translate).apply {
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
            !translation.translation.isNullOrEmpty() && TextToSpeech.isSupportLanguage(translation.targetLang)

        updateOriginalViewer(translation)
        updateSpell(translation)
        updateViewer(translationViewer, translationComponent, translation.translation)

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
        updateExtraViewer(translation.extraDocument)
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

        if (WordBookService.canAddToWordbook(text)) {
            viewer.appendStarButton(translation)
        }

        viewer.caretPosition = 0
        originalComponent.isVisible = true
    }

    private fun Viewer.appendStarButton(translation: Translation) {
        val starIcon = if (translation.favoriteId == null) Icons.StarOff else Icons.StarOn
        val starLabel = LinkLabel<Translation>("", starIcon, { starLabel, trans ->
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
        spellComponent.isVisible = false
        originalComponent.isVisible = false
        translationComponent.isVisible = false
        dictComponent.isVisible = false
        extraComponent.isVisible = false

        originalViewer.empty()
        originalTransliterationLabel.empty()
        translationViewer.empty()
        transliterationLabel.empty()
        extraViewer.empty()

        extraLabel.isVisible = false
        updateDictViewer(null)
    }

    private fun updateSpell(translation: Translation) {
        val spell = translation.spell
        spellComponent.isVisible = spell != null
        spellText.text = spell
        spellText.toolTipText = spell
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

    private fun updateExtraViewer(extraDocument: NamedTranslationDocument?) {
        extraViewer.document.clear()
        if (extraDocument != null) {
            extraLabel.text = extraDocument.name
            extraViewer.setup(extraDocument)

            extraLabel.isVisible = true
            extraViewer.isVisible = true
            extraComponent.isVisible = true
        } else {
            extraLabel.isVisible = false
            extraViewer.isVisible = false
            extraComponent.isVisible = false
        }
        extraViewer.caretPosition = 0
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
            panel.border = JBUI.Borders.empty(0, -GAP, 0, 0)

            for (component in components) {
                panel.add(component)
            }

            return panel
        }

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

            if (!translation.isNullOrBlank()) {
                explainsBuilder.append(translation)
                if (dictText.isNotEmpty()) {
                    explainsBuilder.append("\n\n")
                }
            }
            explainsBuilder.append(dictText)

            return WordBookItem(
                null,
                original.trim(),
                srcLang,
                targetLang,
                srcTransliteration,
                explainsBuilder.toString(),
                null
            )
        }
    }
}