package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.LanguagePair
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.text.NamedTranslationDocument
import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument
import cn.yiiguxing.plugin.translate.trans.text.setup
import cn.yiiguxing.plugin.translate.ui.StyledViewer.Companion.setupActions
import cn.yiiguxing.plugin.translate.ui.UI.disabled
import cn.yiiguxing.plugin.translate.ui.UI.setIcons
import cn.yiiguxing.plugin.translate.ui.settings.OptionsConfigurable
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.util.text.clear
import cn.yiiguxing.plugin.translate.util.text.newLine
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.IdeGlassPane
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.ui.WindowMoveListener
import com.intellij.ui.WindowResizeListener
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Alarm
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import icons.Icons
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.PopupMenuEvent
import kotlin.properties.Delegates

class NewTranslationDialog(
    private val project: Project?,
    val ui: NewTranslationDialogUI = NewTranslationDialogUiImpl(UIProvider())
) :
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

    private var ignoreLanguageEvent: Boolean = false
    private var ignoreInputEvent: Boolean = false

    // If user selects a specific target language, the value is true
    private var unequivocalTargetLang: Boolean by Delegates.observable(false) { _, _, _ ->
        updateLightningLabel()
    }

    private var _disposed = false
    override val disposed get() = _disposed

    private inline val sourceLang: Lang get() = sourceLangComboBox.selected!!
    private inline val targetLang: Lang get() = targetLangComboBox.selected!!

    private lateinit var escListener: AWTEventListener

    init {
        setUndecorated(true)
        isModal = false
        window.minimumSize = JBDimension(0, 0)
        val panel = createCenterPanel()
        initComponents()
        addWindowListeners()
        addMouseListeners()
        peer.setContentPane(panel)

        registerShortcuts()
        registerESCListener()
        Application.messageBus
            .connect(this)
            .subscribe(SettingsChangeListener.TOPIC, this)
    }

    private fun registerESCListener() {
        val win = window
        escListener = AWTEventListener { event ->
            if (event is KeyEvent &&
                event.keyCode == KeyEvent.VK_ESCAPE &&
                !PopupUtil.handleEscKeyEvent() &&
                !win.isFocused // close the displayed popup window first
            ) {
                doCancelAction()
            }
        }
        Toolkit.getDefaultToolkit().addAWTEventListener(escListener, AWTEvent.KEY_EVENT_MASK)
    }

    private fun registerShortcuts() {
        val rootPane = rootPane

        // Title buttons
        DumbAwareAction.create { sourceLangComboBox.togglePopup() }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString("alt S"), rootPane, this
            )
        DumbAwareAction.create { targetLangComboBox.togglePopup() }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString("alt T"), rootPane, this
            )
        DumbAwareAction.create { swapButton.takeIf { it.isEnabled }?.doClick() }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString("alt shift S"), rootPane, this
            )
        DumbAwareAction.create { (pinButton as? ActionButton)?.click() }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString("alt P"), rootPane, this
            )

        // Toolbar buttons
        DumbAwareAction.create { inputTTSButton.play() }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString("alt ENTER", "meta ENTER"), rootPane, this
            )
        DumbAwareAction.create { translationTTSButton.play() }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString("shift ENTER"), rootPane, this
            )
        DumbAwareAction.create { starButton.takeIf { it.isEnabled }?.doClick() }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString("control F", "meta F"), rootPane, this
            )
        DumbAwareAction.create { showHistoryPopup() }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString("control H", "meta H"), rootPane, this
            )
        DumbAwareAction.create { clearText() }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString(
                    "control shift BACK_SPACE",
                    "meta shift BACK_SPACE",
                    "control shift DELETE",
                    "meta shift DELETE"
                ), rootPane, this
            )
        DumbAwareAction.create { copyTranslation() }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString("control shift C", "meta shift C"), rootPane, this
            )

        DumbAwareAction.create { collapseDictViewerButton.takeIf { it.isVisible }?.doClick() }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString("control UP", "meta UP"), rootPane, this
            )
        DumbAwareAction.create { expandDictViewerButton.takeIf { it.isVisible }?.doClick() }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString("control DOWN", "meta DOWN"), rootPane, this
            )
    }

    override fun createCenterPanel(): JComponent {
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

        val resizeListener = object : WindowResizeListener(rootPane, JBUI.insets(6), null) {
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
                storeWindowLocationAndSize()
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
                    override fun windowGainedFocus(e: WindowEvent) {
                        setActive(true)
                    }

                    override fun windowLostFocus(e: WindowEvent) {
                        setActive(false)
                        val oppositeWindow = e.oppositeWindow
                        if (oppositeWindow === window || oppositeWindow != null && oppositeWindow.owner === window) {
                            return
                        }
                        if (!AppStorage.pinNewTranslationDialog && oppositeWindow != null) {
                            doCancelAction()
                        }
                    }
                })
            }
        })
    }

    // Close the dialog when the ESC key is pressed
    override fun createCancelAction(): ActionListener {
        return ActionListener { doCancelAction() }
    }

    private fun initLangComboBoxes() {
        fun addListener(comboBox: LangComboBoxLink) {
            comboBox.addItemListener { _, _, fromUser ->
                if (comboBox === sourceLangComboBox) {
                    updateLightningLabel()
                }
                if (fromUser && comboBox === targetLangComboBox) {
                    unequivocalTargetLang = true
                }

                AppStorage.lastLanguages.let { pair ->
                    pair.source = sourceLang
                    pair.target = targetLang
                }
                if (!ignoreLanguageEvent) {
                    requestTranslate()
                }
            }
        }

        updateLanguages()
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

        inputTextArea.addListener { e ->
            clearButton.isEnabled = e.document.length > 0
            if (!ignoreInputEvent) {
                if (!unequivocalTargetLang && sourceLang == Lang.AUTO) {
                    targetLangComboBox.setSelectLangIgnoreEvent(presenter.getTargetLang(inputTextArea.text))
                }
                requestTranslate()
            }
        }
        translationTextArea.addListener { e ->
            copyButton.isEnabled = e.document.length > 0
        }

        fun JTextArea.setupPopupMenu() {
            componentPopupMenu = JBPopupMenu().apply {
                val copy = JBMenuItem(message("menu.item.copy"), AllIcons.Actions.Copy).apply {
                    disabledIcon = AllIcons.Actions.Copy.disabled()
                    addActionListener { copy() }
                }
                val translate = JBMenuItem(message("menu.item.translate"), Icons.Translation).apply {
                    disabledIcon = Icons.Translation.disabled()
                    addActionListener {
                        selectedText.takeUnless { txt -> txt.isNullOrBlank() }?.let { selectedText ->
                            if (this@setupPopupMenu === inputTextArea) {
                                inputTextArea.text = selectedText
                            } else lastTranslation?.let { translation ->
                                translate(selectedText, translation.targetLang, translation.srcLang)
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

        inputTextArea.setupPopupMenu()
        translationTextArea.setupPopupMenu()
    }

    private fun initButtons() {
        inputTTSButton.isEnabled = false
        translationTTSButton.isEnabled = false

        inputTTSButton.dataSource { lastTranslation?.run { original to srcLang } }
        translationTTSButton.dataSource { lastTranslation?.run { translation?.let { it to targetLang } } }

        clearButton.apply {
            isEnabled = false
            toolTipText = message("translation.dialog.clear.text")
            setListener({ _, _ -> clearText() }, null)
        }
        copyButton.apply {
            isEnabled = false
            toolTipText = message("translation.dialog.copy.translation")
            setListener({ _, _ -> copyTranslation() }, null)
        }

        historyButton.apply {
            toolTipText = message("translation.dialog.history")
            setListener({ _, _ -> showHistoryPopup() }, null)
        }

        updateStarButton(null)
        initSwapButton()

        spellComponent.onSpellFixed {
            inputTextArea.text = it
            sourceLangComboBox.selected = Lang.AUTO
        }

        fixLangComponent.onFixLanguage {
            sourceLangComboBox.selected = it
        }
    }

    private fun initSwapButton() = with(swapButton) {
        toolTipText = message("translation.dialog.swap.languages")
        setListener({ _, _ ->
            val srcLang = lastTranslation?.srcLang ?: sourceLang
            val targetLang = lastTranslation?.targetLang ?: targetLang

            if (srcLang != targetLang) {
                ignoreLanguageEvent = true
                sourceLangComboBox.selected =
                    targetLang.takeIf { presenter.isSupportedSourceLanguage(it) } ?: presenter.primaryLanguage
                targetLangComboBox.selected =
                    srcLang.takeIf { presenter.isSupportedTargetLanguage(it) } ?: presenter.primaryLanguage
                ignoreLanguageEvent = false

                lastTranslation?.translation?.let { inputTextArea.text = it }
                detectedLanguageLabel.isVisible = false
            }
        }, null)
    }

    private fun initDictViewer() {
        dictViewer.apply {
            setupActions(this@NewTranslationDialog::lastTranslation) { text, src, target ->
                translate(text, src, target)
            }
            onBeforeFoldingExpand { _, _ ->
                dictViewerPanel.putClientProperty("lastScroll", dictViewerPanel.verticalScrollBar.value)
            }
            onFoldingExpanded {
                val lastScrollValue = dictViewerPanel.getClientProperty("lastScroll") as Int
                fixWindowHeight()
                invokeLater { dictViewerPanel.verticalScrollBar.value = lastScrollValue }
            }
        }
        expandDictViewerButton.setListener({ _, _ ->
            expandDictViewer()
            AppStorage.newTranslationDialogCollapseDictViewer = false
            fixWindowHeight()
        }, null)
        collapseDictViewerButton.setListener({ _, _ ->
            collapseDictViewer()
            AppStorage.newTranslationDialogCollapseDictViewer = true
            fixWindowHeight()
        }, null)
    }

    private fun updateOnTranslation(translation: Translation?) {
        updateStarButton(translation)
        updateDetectedLangLabel(translation)
        updateTransliterations(translation)
        updateDictViewer(translation?.dictDocument, translation?.extraDocument)
        spellComponent.spell = translation?.spell
        fixLangComponent.updateOnTranslation(translation)
        fixWindowHeight()
    }

    private fun updateStarButton(translation: Translation?) {
        fun updatePresentation(favoriteId: Long?) {
            val icon = if (favoriteId == null) Icons.GrayStarOff else Icons.StarOn
            starButton.setIcons(icon)
            starButton.toolTipText = StarButtons.toolTipText(favoriteId)
        }

        updatePresentation(translation?.favoriteId)

        starButton.isEnabled = translation != null && WordBookService.canAddToWordbook(translation.original)
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

    private fun updateLightningLabel() {
        lightningLabel.isVisible = sourceLang == Lang.AUTO && !unequivocalTargetLang
    }

    private fun updateTransliterations(translation: Translation?) {
        srcTransliterationLabel.text = translation?.srcTransliteration
        targetTransliterationLabel.text = translation?.transliteration
    }

    private fun updateDictViewer(dictDocument: TranslationDocument?, extraDocument: NamedTranslationDocument?) {
        dictViewer.document.clear()
        dictDocument?.let {
            dictViewer.setup(it)
        }
        extraDocument?.let {
            if (dictDocument != null) {
                dictViewer.document.newLine()
                dictViewer.document.newLine()
            }
            dictViewer.setup(it)
        }
        val hasContent = dictDocument != null || extraDocument != null

        if (hasContent && AppStorage.newTranslationDialogCollapseDictViewer)
            collapseDictViewer()
        else if (hasContent)
            expandDictViewer()
        else
            hideDictViewer()

        dictViewer.size = dictViewer.preferredSize
        fixWindowHeight()
        dictViewer.caretPosition = 0
    }

    private fun requestTranslate(delay: Int = presenter.translator.intervalLimit) {
        alarm.apply {
            cancelAllRequests()
            addRequest(translateAction, maxOf(delay, 500))
        }
    }

    private fun onTranslate() {
        inputTextArea.text.takeUnless { it.isNullOrBlank() }?.let {
            if (!historyShowing) {
                presenter.translate(it, sourceLang, targetLang)
            }
        } ?: clearTranslation()
    }

    private fun clearText() {
        inputTextArea.text = ""
        translationTextArea.text = ""
        clearTranslation()
    }

    private fun copyTranslation() {
        val textToCopy = translationTextArea
            .selectedText
            .takeUnless { it.isNullOrEmpty() }
            ?: translationTextArea.text
        if (!textToCopy.isNullOrEmpty()) {
            CopyPasteManager.getInstance().setContents(StringSelection(textToCopy))
        }
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

        // forcibly modify the target language
        if (translation.srcLang != Lang.AUTO &&
            translation.targetLang != Lang.AUTO &&
            translation.srcLang == translation.targetLang &&
            presenter.primaryLanguage != Lang.ENGLISH
        ) {
            val newTargetLang = if (translation.targetLang == Lang.ENGLISH) presenter.primaryLanguage else Lang.ENGLISH
            targetLangComboBox.selected = newTargetLang
        } else {
            onTranslationFinished(translation)
        }
    }

    fun applyTranslation(translation: Translation) {
        ignoreLanguageEvent = true
        ignoreInputEvent = true
        try {
            inputTextArea.text = translation.original
            sourceLangComboBox.selected = translation.srcLang
            targetLangComboBox.selected = translation.targetLang
        } finally {
            ignoreLanguageEvent = false
            ignoreInputEvent = false
        }
        unequivocalTargetLang = true
        onTranslationFinished(translation)
    }

    private fun onTranslationFinished(translation: Translation) {
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
        Notifications.showErrorNotification(project, message("error.title"), errorMessage, throwable)
    }

    override fun onTranslatorChanged(settings: Settings, translationEngine: TranslationEngine) {
        updateLanguages()
        requestTranslate(0)
    }

    private fun updateLanguages(languagePair: LanguagePair? = null) {
        presenter.supportedLanguages.let { (src, target) ->
            sourceLangComboBox.apply {
                val srcSelected = (languagePair?.source ?: selected ?: Lang.AUTO).takeIf { src.contains(it) }
                model = LanguageListModel.sorted(src, srcSelected)
            }
            targetLangComboBox.apply {
                val targetSelected = (languagePair?.target ?: selected)
                    ?.takeIf { target.contains(it) }
                    ?: presenter.getTargetLang(inputTextArea.text)
                model = LanguageListModel.sorted(target, targetSelected)
            }
        }
    }

    override fun show() {
        if (!isShowing) {
            super.show()
            restoreWindowLocationAndSize()
        }

        focusManager.requestFocus(inputTextArea, true)
    }

    fun close() {
        storeWindowLocationAndSize()
        close(CLOSE_EXIT_CODE)
    }

    override fun dispose() {
        if (disposed) {
            return
        }

        super.dispose()
        _disposed = true

        Toolkit.getDefaultToolkit().removeAWTEventListener(escListener)
        Disposer.dispose(this)
        println("Translation dialog disposed.")
    }

    /**
     * 翻译指定的[内容][text]
     */
    fun translate(text: String) {
        if (disposed || text.isBlank()) {
            return
        }

        val srcLang: Lang = Lang.AUTO
        val targetLang = presenter.getTargetLang(text)
        unequivocalTargetLang = false
        translateInternal(text, srcLang, targetLang)
    }

    /**
     * 以指定的[源语言][src]和[目标语言][target]翻译指定的[内容][text]
     *
     * @param text 需要翻译的内容
     * @param src 源语言
     * @param target 目标语言
     */
    fun translate(text: String, src: Lang, target: Lang) {
        if (disposed || text.isBlank()) {
            return
        }

        lateinit var srcLang: Lang
        lateinit var targetLang: Lang

        presenter.supportedLanguages.let { (sourceList, targetList) ->
            srcLang = src.takeIf { sourceList.contains(it) } ?: sourceLangComboBox.selected ?: sourceList.first()
            targetLang =
                target.takeIf { targetList.contains(it) } ?: targetLangComboBox.selected ?: presenter.primaryLanguage
        }

        unequivocalTargetLang = true
        translateInternal(text, srcLang, targetLang)
    }

    private fun translateInternal(text: String, srcLang: Lang, targetLang: Lang) {
        sourceLangComboBox.setSelectLangIgnoreEvent(srcLang)
        targetLangComboBox.setSelectLangIgnoreEvent(targetLang)
        inputTextArea.text = text
        detectedLanguageLabel.isVisible = false
    }

    private fun LangComboBoxLink.setSelectLangIgnoreEvent(lang: Lang) {
        ignoreLanguageEvent = true
        selected = lang
        ignoreLanguageEvent = false
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
                        translate(chosen ?: currentInput)
                    }
                }
            })
            .createPopup()
            .show(historyButton)

    }

    private fun fixWindowHeight(width: Int = window.width) {
        rootPane.preferredSize = null
        window.setSize(width, rootPane.preferredSize.height)
    }

    private fun storeWindowLocationAndSize() {
        AppStorage.newTranslationDialogX = window.location.x
        AppStorage.newTranslationDialogY = window.location.y
        AppStorage.newTranslationDialogWidth = translationPanel.width
        AppStorage.newTranslationDialogHeight = translationPanel.height

        translationPanel.preferredSize = translationPanel.size
    }

    private fun restoreWindowLocationAndSize() {
        val savedX = AppStorage.newTranslationDialogX
        val savedY = AppStorage.newTranslationDialogY
        val savedWidth = AppStorage.newTranslationDialogWidth
        val savedHeight = AppStorage.newTranslationDialogHeight
        if (savedX != null && savedY != null) {
            val intersectWithScreen = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .screenDevices
                .any { gd ->
                    gd.defaultConfiguration.bounds.intersects(
                        savedX.toDouble(),
                        savedY.toDouble(),
                        savedWidth.toDouble(),
                        savedHeight.toDouble()
                    )
                }
            if (intersectWithScreen) {
                window.location = Point(savedX, savedY)
            } else {
                AppStorage.newTranslationDialogX = null
                AppStorage.newTranslationDialogY = null
            }
        }
        val savedSize = Dimension(savedWidth, savedHeight)
        translationPanel.size = savedSize
        translationPanel.preferredSize = savedSize
        fixWindowHeight(savedWidth)
    }

    private class UIProvider : NewTranslationDialogUiProvider {
        override fun createPinButton(): JComponent = actionButton(MyPinAction())

        override fun createSettingsButton(): JComponent = actionButton(MySettingsAction())

        private fun actionButton(action: AnAction): ActionButton =
            ActionButton(
                action,
                action.templatePresentation,
                ActionPlaces.UNKNOWN,
                ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
            )
    }

    private class MyPinAction : ToggleAction(
        message("translation.dialog.pin.window"),
        message("translation.dialog.pin.window"),
        AllIcons.General.Pin_tab
    ), DumbAware {
        override fun isDumbAware(): Boolean {
            return true
        }

        override fun isSelected(e: AnActionEvent): Boolean {
            return AppStorage.pinNewTranslationDialog
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            AppStorage.pinNewTranslationDialog = state
        }
    }

    private class MySettingsAction : AnAction(
        message("settings.title.translate"),
        message("settings.title.translate"),
        AllIcons.General.GearPlain
    ), DumbAware {
        override fun actionPerformed(e: AnActionEvent) {
            OptionsConfigurable.showSettingsDialog(e.project)
        }
    }


    companion object {
        private const val FONT_SIZE_DEFAULT = 14
        private const val FONT_SIZE_PHONETIC = 12
    }
}