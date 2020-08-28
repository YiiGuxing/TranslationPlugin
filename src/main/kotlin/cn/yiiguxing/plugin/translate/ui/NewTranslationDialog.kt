package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.trans.BaiduTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.LanguagePair
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument
import cn.yiiguxing.plugin.translate.trans.text.setup
import cn.yiiguxing.plugin.translate.ui.StyledViewer.Companion.setupActions
import cn.yiiguxing.plugin.translate.ui.UI.setIcons
import cn.yiiguxing.plugin.translate.ui.icon.LangComboBoxLink
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.util.text.clear
import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.IdeGlassPane
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.WindowMoveListener
import com.intellij.ui.WindowResizeListener
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Alarm
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import icons.Icons
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Point
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent

class NewTranslationDialog(private val project: Project?,
                           val ui: NewTranslationDialogUI = NewTranslationDialogUiImpl(UIProvider())) :
        DialogWrapper(project),
        NewTranslationDialogUI by ui,
        View,
        Disposable,
        SettingsChangeListener {

    private val presenter: Presenter = TranslationPresenter(this)
    private val focusManager: IdeFocusManager = IdeFocusManager.getInstance(project)

    private val alarm: Alarm = Alarm(this)
    private val translateAction = Runnable { onTranslate() }

    private var currentRequest: Presenter.Request? = null
    private var lastTranslation: Translation? = null
    private var historyShowing: Boolean = false

    private var _disposed = false
    override val disposed get() = _disposed

    private inline val sourceLang: Lang get() = sourceLangComboBox.selected
    private inline val targetLang: Lang get() = targetLangComboBox.selected

    init {
        setUndecorated(true)
        isModal = false
        window.minimumSize = JBDimension(0, 0)
        initComponents()
        addWindowListeners()
        addMouseListeners()
        peer.setContentPane(createCenterPanel())

        Application.messageBus
                .connect(this)
                .subscribe(SettingsChangeListener.TOPIC, this)
    }

    override fun createCenterPanel(): JComponent? {
        return createMainPanel()
    }

    private fun initComponents() {
        initLangComboBoxes()
        initTextAreas()
        initButtons()
        initFonts(UI.getFonts(FONT_SIZE_DEFAULT, FONT_SIZE_PHONETIC))
        initDictViewer()
        updateOnTranslation(null)
    }

    private fun addMouseListeners() {
        topPanel.apply {
            WindowMoveListener(this).let {
                addMouseListener(it)
                addMouseMotionListener(it)
            }
        }
        val glassPane = rootPane.glassPane as IdeGlassPane

        //may resize only width, height is adjusted automatically
        val resizeListener = object : WindowResizeListener(rootPane, JBUI.insets(0, 6, 0, 6), null) {
            var myCursor: Cursor? = null

            override fun setCursor(content: Component, cursor: Cursor) {
                if (myCursor !== cursor || myCursor !== Cursor.getDefaultCursor()) {
                    glassPane.setCursor(cursor, this)
                    myCursor = cursor
                    if (content is JComponent) {
                        IdeGlassPaneImpl.savePreProcessedCursor(content, content.getCursor())
                    }
                    super.setCursor(content, cursor)
                }
            }

            override fun mouseReleased(event: MouseEvent?) {
                super.mouseReleased(event)
                if (windowWidthChanged()) {
                    invokeLater { fixWindowHeight() }
                }
                storeWindowLocation()
            }
        }
        glassPane.addMouseMotionPreprocessor(resizeListener, this.disposable)
        glassPane.addMousePreprocessor(resizeListener, this.disposable)
    }

    private fun addWindowListeners() {
        val window = peer.window
        window.addWindowListener(object : WindowAdapter() {
            override fun windowOpened(e: WindowEvent) {
                window.addWindowFocusListener(object : WindowAdapter() {
                    override fun windowLostFocus(e: WindowEvent) {
                        val oppositeWindow = e.oppositeWindow
                        if (oppositeWindow === window || oppositeWindow != null && oppositeWindow.owner === window) {
                            return
                        }
                        if (!Settings.pinNewTranslationDialog && oppositeWindow != null) {
                            doCancelAction()
                        }
                    }
                })
            }
        })
    }


    private fun initLangComboBoxes() {
        fun addListener(comboBox: LangComboBoxLink) {
            comboBox.addItemListener {
                AppStorage.lastInstantLanguages.let { pair ->
                    pair.source = sourceLang
                    pair.target = targetLang
                }
                requestTranslate()
            }
        }
        updateLanguages(AppStorage.lastInstantLanguages)

        addListener(sourceLangComboBox)
        addListener(targetLangComboBox)
    }

    private fun initTextAreas() {
        fun JTextArea.addListener(onDocumentChange: (DocumentEvent) -> Unit) {
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    onDocumentChange(e)
                }
            })
        }

        //resize window on document size change
        listOf(inputTextArea, translationTextArea).forEach { textArea ->
            textArea.addListener {
                val lineCountKey = "lineCount"
                val lastLineCount = textArea.getClientProperty(lineCountKey)
                if (lastLineCount != textArea.lineCount) {
                    textArea.putClientProperty(lineCountKey, textArea.lineCount)
                    fixWindowHeight()
                }
            }
        }

        inputTextArea.addListener { e ->
            clearButton.isEnabled = e.document.length > 0
            requestTranslate()
        }
        translationTextArea.addListener { e ->
            copyButton.isEnabled = e.document.length > 0
        }
    }

    private fun initButtons() {
        inputTTSButton.isEnabled = false
        translationTTSButton.isEnabled = false

        inputTTSButton.dataSource { lastTranslation?.run { original to srcLang } }
        translationTTSButton.dataSource { lastTranslation?.run { translation?.let { it to targetLang } } }

        clearButton.apply {
            isEnabled = false
            toolTipText = "Clear Text"
            setListener({ _, _ ->
                inputTextArea.text = ""
                translationTextArea.text = ""
            }, null)
        }
        copyButton.apply {
            isEnabled = false
            toolTipText = "Copy Text"
            setListener({ _, _ ->
                val textToCopy = translationTextArea
                        .selectedText
                        .takeUnless { it.isNullOrEmpty() }
                        ?: translationTextArea.text
                if (!textToCopy.isNullOrEmpty()) {
                    CopyPasteManager.getInstance().setContents(StringSelection(textToCopy))
                }
            }, null)
        }

        historyButton.apply {
            setListener({ _, _ ->
                showHistoryPopup()
            }, null)
        }

        updateStarButton(null)
        initSwapButton()

        spellComponent.onSpellFixed {
            inputTextArea.text = it
            sourceLangComboBox.selected = Lang.AUTO
        }
    }

    private fun initSwapButton() = with(swapButton) {
        setListener({ _, _ ->
            val srcLang = lastTranslation?.srcLang ?: sourceLang
            val targetLang = lastTranslation?.targetLang ?: targetLang

            if (srcLang != targetLang) {
                presenter.supportedLanguages.let { (src, target) ->
                    sourceLangComboBox.selected = targetLang.takeIf { src.contains(it) } ?: presenter.primaryLanguage
                    targetLangComboBox.selected = srcLang.takeIf { target.contains(it) } ?: presenter.primaryLanguage
                }

                lastTranslation?.translation?.let { inputTextArea.text = it }
            }
        }, null)
    }

    private fun initDictViewer() {
        dictViewer.setupActions(this::lastTranslation) { text, src, target ->
            sourceLangComboBox.selected = src
            targetLangComboBox.selected = target
            inputTextArea.text = text
        }
        dictViewerCollapsible.isCollapsed = Settings.newTranslationDialogCollapseDictViewer
        dictViewerCollapsible.setExpandCollapseListener {
            fixWindowHeight()
            Settings.newTranslationDialogCollapseDictViewer = dictViewerCollapsible.isCollapsed
        }
    }

    private fun updateOnTranslation(translation: Translation?) {
        updateStarButton(translation)
        updateDetectedLangLabel(translation)
        updateTransliterations(translation)
        updateDictViewer(translation?.dictDocument)
        spellComponent.spell = translation?.spell
        fixWindowHeight()
    }

    private fun updateStarButton(translation: Translation?) {
        fun updatePresentation(favoriteId: Long?) {
            val icon = if (favoriteId == null) Icons.GrayStarOff else Icons.StarOn
            starButton.setIcons(icon)
            starButton.toolTipText = StarButtons.toolTipText(favoriteId)
        }

        updatePresentation(translation?.favoriteId)

        starButton.isEnabled = translation != null

        starButton.setListener(StarButtons.listener, translation)
        translation?.observableFavoriteId?.observe(this@NewTranslationDialog) { favoriteId, _ ->
            updatePresentation(favoriteId)
        }
    }

    private fun updateDetectedLangLabel(translation: Translation?) {
        val detected = translation?.srcLang?.langName?.takeIf { sourceLang == Lang.AUTO }

        detectedLanguageLabel.text = detected
        detectedLanguageLabel.isVisible = detected != null
    }

    private fun updateTransliterations(translation: Translation?) {
        srcTransliterationLabel.text = translation?.srcTransliteration
        targetTransliterationLabel.text = translation?.transliteration
    }

    private fun updateDictViewer(dictDocument: TranslationDocument?) {
        dictViewer.document.clear()
        if (dictDocument != null) {
            dictViewer.setup(dictDocument)
            dictViewerCollapsible.panel.isVisible = true
        } else {
            dictViewerCollapsible.panel.isVisible = false
        }
        dictViewer.caretPosition = 0
    }

    private fun requestTranslate(delay: Int = if (presenter.translatorId == BaiduTranslator.id) 1000 else 500) {
        alarm.apply {
            cancelAllRequests()
            addRequest(translateAction, delay)
        }
    }

    private fun onTranslate() {
        inputTextArea.text.takeUnless { it.isNullOrBlank() }?.let {
            if (!historyShowing) {
                presenter.translate(it, sourceLang, targetLang)
            }
        } ?: clearTranslation()
    }

    private fun clearTranslation() {
        swapButton.isEnabled = true
        inputTTSButton.isEnabled = false
        translationTTSButton.isEnabled = false
        currentRequest = null
        lastTranslation = null
        translationTextArea.text = null
        updateOnTranslation(null)
    }

    override fun showStartTranslate(request: Presenter.Request, text: String) {
        currentRequest = request
        swapButton.isEnabled = false
        inputTTSButton.isEnabled = false
        translationTTSButton.isEnabled = false
        translationTextArea.text = "${lastTranslation?.translation ?: ""}..."
    }

    override fun showTranslation(request: Presenter.Request, translation: Translation, fromCache: Boolean) {
        if (currentRequest != request && !fromCache) {
            return
        }

        currentRequest = null
        lastTranslation = translation
        swapButton.isEnabled = true
        inputTTSButton.isEnabled = TextToSpeech.isSupportLanguage(translation.srcLang)
        translationTTSButton.isEnabled = TextToSpeech.isSupportLanguage(translation.targetLang)
        translationTextArea.text = translation.translation
        updateOnTranslation(translation)
    }

    override fun showError(request: Presenter.Request, errorMessage: String, throwable: Throwable) {
        if (currentRequest == request) {
            clearTranslation()
        }
        Notifications.showErrorNotification(
                project, NOTIFICATION_DISPLAY_ID,
                "Translate Error", errorMessage, throwable
        )
    }

    override fun onTranslatorChanged(settings: Settings, translatorId: String) {
        updateLanguages()
        requestTranslate(0)
    }

    private fun updateLanguages(languagePair: LanguagePair? = null) {
        presenter.supportedLanguages.let { (src, target) ->
            sourceLangComboBox.apply {
                val srcSelected = (languagePair?.source ?: selected)
                    .takeIf { src.contains(it) }
                    ?: src.first()
                model = LanguageListModel.sorted(src, srcSelected)
            }
            targetLangComboBox.apply {
                val targetSelected = (languagePair?.target ?: selected)
                    .takeIf { target.contains(it) }
                    ?: Lang.ENGLISH
                model = LanguageListModel.sorted(target, targetSelected)
            }
        }
    }

    override fun show() {
        if (!isShowing) {
            super.show()
            restoreWindowLocation()
        }

        focusManager.requestFocus(inputTextArea, true)
    }

    fun close() {
        close(CLOSE_EXIT_CODE)
    }

    override fun dispose() {
        if (disposed) {
            return
        }

        super.dispose()
        _disposed = true

        Disposer.dispose(this)
        println("Instant translate dialog disposed.")
    }

    private fun showHistoryPopup() {
        val currentInput = inputTextArea.text
        var chosen: String? = null

        return JBPopupFactory.getInstance().createPopupChooserBuilder(presenter.histories)
                .setVisibleRowCount(7)
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                .setItemSelectedCallback { inputTextArea.text = it }
                .setItemChosenCallback { chosen = it }
                .setRenderer(HistoryRenderer({ sourceLangComboBox.selected }, { targetLangComboBox.selected }, presenter))
                .addListener(object : JBPopupListener {
                    override fun beforeShown(event: LightweightWindowEvent) {
                        historyShowing = true
                        val popup = event.asPopup()
                        popup.size = Dimension(300, popup.size.height)
                        val relativePoint = RelativePoint(historyButton, Point(0, -JBUI.scale(3)))
                        val screenPoint = Point(relativePoint.screenPoint).apply { translate(0, -popup.size.height) }

                        popup.setLocation(screenPoint)
                    }

                    override fun onClosed(event: LightweightWindowEvent) {
                        historyShowing = false
                        invokeLater {
                            inputTextArea.text = chosen ?: currentInput
                        }
                    }
                })
                .createPopup()
                .show(historyButton)

    }

    private fun fixWindowHeight(width: Int = window.width) {
        window.preferredSize = null

        window.minimumSize = Dimension(width, 0)
        window.maximumSize = Dimension(width, Int.MAX_VALUE)

        try {
            window.setSize(width, window.preferredSize.height)
        }
        finally {
            window.minimumSize = Dimension(0, 0)
            window.preferredSize = Dimension(0, 0)
            window.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        }
    }

    private fun windowWidthChanged(): Boolean {
        return Settings.newTranslationDialogWidth != window.width
    }

    private fun storeWindowLocation() {
        Settings.newTranslationDialogX = window.location.x
        Settings.newTranslationDialogY = window.location.y
        Settings.newTranslationDialogWidth = window.width
    }

    private fun restoreWindowLocation() {
        val savedX = Settings.newTranslationDialogX
        val savedY = Settings.newTranslationDialogY
        if (savedX != null && savedY != null) {
            window.location = Point(savedX, savedY)
        }
        fixWindowHeight(Settings.newTranslationDialogWidth)
    }

    private class UIProvider: NewTranslationDialogUiProvider {
        override fun createPinButton(): JComponent = actionButton(MyPinAction())

        override fun createSettingsButton(): JComponent = actionButton(MySettingsAction())

        private fun actionButton(action: AnAction): ActionButton =
            ActionButton(action, action.templatePresentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
    }

    private class MyPinAction : ToggleAction(
        IdeBundle.messagePointer("action.ToggleAction.text.pin.window"),
        IdeBundle.messagePointer("action.ToggleAction.description.pin.window"),
        AllIcons.General.Pin_tab
    ) {
        override fun isDumbAware(): Boolean {
            return true
        }

        override fun isSelected(e: AnActionEvent): Boolean {
            return Settings.pinNewTranslationDialog
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            Settings.pinNewTranslationDialog = state
        }
    }

    private class MySettingsAction : AnAction(
        message("settings.title.translate"),
        message("settings.title.translate"),
        AllIcons.General.GearPlain
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            //TODO("Not yet implemented")
        }
    }


    companion object {
        val defaultWidth = 600

        private const val FONT_SIZE_DEFAULT = 14
        private const val FONT_SIZE_PHONETIC = 12

        private const val NOTIFICATION_DISPLAY_ID = "Instant Translate Error"
    }
}