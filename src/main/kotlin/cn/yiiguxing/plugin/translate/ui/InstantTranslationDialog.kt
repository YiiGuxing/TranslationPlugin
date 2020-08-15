package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.trans.BaiduTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.LanguagePair
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.form.InstantTranslationDialogForm
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.SideBorder
import com.intellij.util.Alarm
import icons.Icons
import java.awt.datatransfer.StringSelection
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent
import kotlin.text.isNullOrBlank

/**
 * InstantTranslationDialog
 */
class InstantTranslationDialog(private val project: Project?) :
    InstantTranslationDialogForm(project),
    View,
    Disposable,
    SettingsChangeListener {

    private val presenter: Presenter = TranslationPresenter(this, false)
    private val focusManager: IdeFocusManager = IdeFocusManager.getInstance(project)

    private val alarm: Alarm = Alarm(this)
    private val translateAction = Runnable { onTranslate() }

    private var currentRequest: Presenter.Request? = null
    private var lastTranslation: Translation? = null

    private var _disposed = false
    override val disposed get() = _disposed

    private inline val sourceLang: Lang get() = sourceLangComboBox.selected!!
    private inline val targetLang: Lang get() = targetLangComboBox.selected!!

    init {
        title = "Translation"
        isModal = false
        initComponents()
        peer.setContentPane(createCenterPanel())

        Application.messageBus
            .connect(this)
            .subscribe(SettingsChangeListener.TOPIC, this)
    }

    private fun initComponents() {
        initBorders()
        initBackground()
        initLangComboBoxes()
        initTextAreas()
        initToolBar()
        initSwapButton()
        initTranslateButton()
    }

    private fun initBorders() {
        inputScrollPane.border = null
        translationScrollPane.border = null

        val borderColor = UI.getBordersColor(BORDER_COLOR)
        val border = LineBorder(borderColor)
        val borderTop = SideBorder(borderColor, SideBorder.TOP)

        inputContentPanel.border = border
        translationContentPanel.border = border
        inputToolBar.border = borderTop
        translationToolBar.border = borderTop
    }

    private fun initBackground() {
        val background = UI.getColor("ToolWindow.Header.background", TOOLBAR_BACKGROUND)
            ?.alphaBlend(inputTextArea.background, 0.6f)

        inputToolBar.background = background
        translationToolBar.background = background
    }

    private fun initLangComboBoxes() {
        sourceLangComboBox.renderer = LanguageRenderer()
        targetLangComboBox.renderer = LanguageRenderer()

        val itemListener = ItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                AppStorage.lastInstantLanguages.let { pair ->
                    pair.source = sourceLang
                    pair.target = targetLang
                }
                requestTranslate()
            }
        }
        sourceLangComboBox.addItemListener(itemListener)
        targetLangComboBox.addItemListener(itemListener)

        updateLanguages(AppStorage.lastInstantLanguages)
    }

    private fun initTextAreas() {
        UI.primaryFont(14).let {
            inputTextArea.font = it
            translationTextArea.font = it
        }
        inputTextArea.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                clearButton.isEnabled = e.document.length > 0
                requestTranslate()
            }
        })
        translationTextArea.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                copyButton.isEnabled = e.document.length > 0
            }
        })
    }

    private fun initToolBar() {
        inputTTSButton.isEnabled = false
        translationTTSButton.isEnabled = false

        inputTTSButton.dataSource { lastTranslation?.run { original to srcLang } }
        translationTTSButton.dataSource { lastTranslation?.run { translation?.let { it to targetLang } } }

        clearButton.apply {
            isEnabled = false
            icon = Icons.ClearText
            disabledIcon = Icons.ClearTextDisabled
            setHoveringIcon(Icons.ClearTextHovering)
            setListener({ _, _ -> inputTextArea.text = "" }, null)
        }
        copyButton.apply {
            isEnabled = false
            icon = Icons.CopyAll
            disabledIcon = Icons.CopyAllDisabled
            setHoveringIcon(Icons.CopyAllHovering)
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
    }

    private fun initSwapButton() = with(swapButton) {
        icon = Icons.Swap2
        addActionListener {
            val srcLang = lastTranslation?.srcLang ?: sourceLang
            val targetLang = lastTranslation?.targetLang ?: targetLang

            if (srcLang != targetLang) {
                presenter.supportedLanguages.let { (src, target) ->
                    sourceLangComboBox.selected = targetLang.takeIf { src.contains(it) } ?: presenter.primaryLanguage
                    targetLangComboBox.selected = srcLang.takeIf { target.contains(it) } ?: presenter.primaryLanguage
                }

                lastTranslation?.translation?.let { inputTextArea.text = it }
            }
        }
    }

    private fun initTranslateButton() = with(translateButton) {
        foreground = JBColor(0x0077C2, 0x389FD6)
        addActionListener { onTranslate() }
    }

    private fun requestTranslate(delay: Int = if (presenter.translatorId == BaiduTranslator.id) 1000 else 500) {
        alarm.apply {
            cancelAllRequests()
            addRequest(translateAction, delay)
        }
    }

    private fun onTranslate() {
        inputTextArea.text.takeUnless { it.isNullOrBlank() }?.let {
            presenter.translate(it, sourceLang, targetLang)
        } ?: clearTranslation()
    }

    private fun clearTranslation() {
        swapButton.isEnabled = true
        inputTTSButton.isEnabled = false
        translationTTSButton.isEnabled = false
        currentRequest = null
        lastTranslation = null
        translationTextArea.text = null
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
                    ?.takeIf { src.contains(it) }
                    ?: src.first()
                model = LanguageListModel(src, srcSelected)
            }
            targetLangComboBox.apply {
                val targetSelected = (languagePair?.target ?: selected)
                    ?.takeIf { target.contains(it) }
                    ?: Lang.ENGLISH
                model = LanguageListModel(target, targetSelected)
            }
        }
    }

    override fun show() {
        if (!isShowing) {
            super.show()
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

    companion object {
        private const val NOTIFICATION_DISPLAY_ID = "Instant Translate Error"
        private val BORDER_COLOR = JBColor(0x808080, 0x303030)
        private val TOOLBAR_BACKGROUND = JBColor(0xEEF1F3, 0x4E5556)
    }
}