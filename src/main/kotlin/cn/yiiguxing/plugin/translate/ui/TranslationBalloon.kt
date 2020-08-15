package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.balloon.BalloonPopupBuilder
import cn.yiiguxing.plugin.translate.ui.settings.OptionsConfigurable
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.copyToClipboard
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.util.isNullOrBlank
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.*
import icons.Icons
import java.awt.AWTEvent
import java.awt.Color
import java.awt.Component.RIGHT_ALIGNMENT
import java.awt.Component.TOP_ALIGNMENT
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.HyperlinkEvent

class TranslationBalloon(
    private val editor: Editor,
    private val text: String
) : View, SettingsChangeListener {

    private val project: Project? = editor.project
    private val presenter: Presenter = TranslationPresenter(this)

    private val layout = FixedSizeCardLayout()
    private val contentPanel = JBPanel<JBPanel<*>>(layout)
    private val errorPanel = NonOpaquePanel(FrameLayout())
    private val errorPane = JTextPane()
    private val processPane = ProcessComponent("Querying...", JBUI.insets(INSETS))
    private val translationContentPane = NonOpaquePanel(FrameLayout())
    private val translationPane = BalloonTranslationPane(project, Settings, getMaxWidth(project))
    private val pinButton = ActionLink(icon = Icons.Pin) {
        showOnTranslationDialog(text, translationPane.sourceLanguage, translationPane.targetLanguage)
    }
    private val copyErrorLink = ActionLink(icon = Icons.CopyToClipboard) {
        lastError?.copyToClipboard()
        hide()
    }

    private val balloon: Balloon

    private var isShowing = false
    private var _disposed = false
    override val disposed get() = _disposed || balloon.isDisposed

    private var lastError: Throwable? = null

    private var lastMoveWasInsideBalloon = false
    private val eventListener = AWTEventListener {
        if (it is MouseEvent && it.id == MouseEvent.MOUSE_MOVED) {
            val inside = isInsideBalloon(RelativePoint(it))
            if (inside != lastMoveWasInsideBalloon) {
                lastMoveWasInsideBalloon = inside
                pinButton.isVisible = inside
                copyErrorLink.isVisible = inside
            }
        }
    }

    init {
        initErrorPanel()
        initTranslationPanel()
        initContentPanel()

        balloon = createBalloon(contentPanel)
        initActions()

        Disposer.register(TranslationUIManager.disposable(project), balloon)
        // 如果使用`Disposer.register(balloon, this)`的话，
        // `TranslationBalloon`在外部以子`Disposable`再次注册时将会使之无效。
        Disposer.register(balloon, Disposable { Disposer.dispose(this) })
        Disposer.register(this, processPane)
        Disposer.register(this, translationPane)

        ApplicationManager
            .getApplication()
            .messageBus
            .connect(this)
            .subscribe(SettingsChangeListener.TOPIC, this)
    }

    private fun initContentPanel() = contentPanel
        .withFont(UI.defaultFont)
        .andTransparent()
        .apply {
            add(CARD_PROCESSING, processPane)
            add(CARD_TRANSLATION, translationContentPane)
            add(CARD_ERROR, errorPanel)
        }

    private fun initTranslationPanel() {
        presenter.supportedLanguages.let { (source, target) ->
            translationPane.setSupportedLanguages(source, target)
        }

        translationContentPane.apply {
            add(pinButton.apply {
                isVisible = false
                alignmentX = RIGHT_ALIGNMENT
                alignmentY = TOP_ALIGNMENT
            })
            add(translationPane)
        }
    }

    private fun initActions() = with(translationPane) {
        onRevalidate { if (!disposed) balloon.revalidate() }
        onLanguageChanged { src, target ->
            run {
                presenter.updateLastLanguages(src, target)
                translate(src, target)
            }
        }
        onNewTranslate { text, src, target ->
            invokeLater { showOnTranslationDialog(text, src, target) }
        }
        onSpellFixed { spell ->
            val targetLang = presenter.getTargetLang(spell)
            invokeLater { showOnTranslationDialog(spell, Lang.AUTO, targetLang) }
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_MOTION_EVENT_MASK)
    }

    private fun initErrorPanel() {
        errorPane.apply {
            contentType = "text/html"
            isEditable = false
            isOpaque = false
            editorKit = UI.errorHTMLKit
            foreground = JBColor(0xFF3333, 0xFF3333)
            font = UI.primaryFont(15)
            border = JBEmptyBorder(INSETS, INSETS + 10, INSETS, INSETS + 10)
            maximumSize = JBDimension(MAX_WIDTH, Int.MAX_VALUE)

            addHyperlinkListener(object : HyperlinkAdapter() {
                override fun hyperlinkActivated(hyperlinkEvent: HyperlinkEvent) {
                    if (HTML_DESCRIPTION_SETTINGS == hyperlinkEvent.description) {
                        this@TranslationBalloon.hide()
                        OptionsConfigurable.showSettingsDialog(project)
                    }
                }
            })
        }

        copyErrorLink.apply {
            isVisible = false
            border = JBEmptyBorder(0, 0, 0, 2)
            toolTipText = "Copy error info to clipboard."
            alignmentX = RIGHT_ALIGNMENT
            alignmentY = TOP_ALIGNMENT
        }
        errorPanel.apply {
            add(copyErrorLink)
            add(errorPane)
        }
    }

    private fun isInsideBalloon(target: RelativePoint): Boolean {
        val cmp = target.originalComponent
        val content = contentPanel

        return when {
            cmp === pinButton -> true
            !cmp.isShowing -> true
            cmp is MenuElement -> false
            UIUtil.isDescendingFrom(cmp, content) -> true
            !content.isShowing -> false
            else -> {
                val point = target.screenPoint
                SwingUtilities.convertPointFromScreen(point, content)
                content.contains(point)
            }
        }
    }

    override fun dispose() {
        if (_disposed) {
            return
        }

        _disposed = true
        isShowing = false

        balloon.hide()
        Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener)

        println("Balloon disposed.")
    }

    fun hide() {
        if (!disposed) {
            Disposer.dispose(this)
        }
    }

    fun show(tracker: PositionTracker<Balloon>, position: Balloon.Position) {
        check(!disposed) { "Balloon has been disposed." }

        if (!isShowing) {
            isShowing = true

            editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
            balloon.show(tracker, position)

            val targetLang = presenter.getTargetLang(text)
            translate(Lang.AUTO, targetLang)
        }
    }

    private fun translate(srcLang: Lang, targetLang: Lang) = presenter.translate(text, srcLang, targetLang)

    private fun showCard(card: String) {
        invokeLater {
            if (!disposed) {
                layout.show(contentPanel, card)
                balloon.revalidate()
            }
        }
    }

    private fun showOnTranslationDialog(text: String, srcLang: Lang? = null, targetLang: Lang? = null) {
        hide()
        val dialog = TranslationUIManager.showDialog(editor.project)
        if (!text.isNullOrBlank()) {
            dialog.translate(text, srcLang, targetLang)
        }
    }

    override fun onTranslatorChanged(settings: Settings, translatorId: String) {
        hide()
    }

    override fun showStartTranslate(request: Presenter.Request, text: String) {
        if (!disposed) {
            showCard(CARD_PROCESSING)
        }
    }

    override fun showTranslation(request: Presenter.Request, translation: Translation, fromCache: Boolean) {
        if (!disposed) {
            translationPane.translation = translation
            // 太快了会没有朋友，大小又会不对了，谁能告诉我到底发生了什么？
            invokeLater(5) { showCard(CARD_TRANSLATION) }
        }
    }

    override fun showError(request: Presenter.Request, errorMessage: String, throwable: Throwable) {
        if (!disposed) {
            lastError = throwable
            errorPane.text = errorMessage
            invokeLater(5) { showCard(CARD_ERROR) }
        }
    }

    companion object {

        private const val MAX_WIDTH = 500
        private const val INSETS = 20

        private const val CARD_PROCESSING = "processing"
        private const val CARD_ERROR = "error"
        private const val CARD_TRANSLATION = "translation"

        private fun createBalloon(content: JComponent): Balloon = BalloonPopupBuilder(content)
            .setDialogMode(true)
            .setFillColor(UIManager.getColor("Panel.background"))
            .setBorderColor(Color.darkGray.toAlpha(75))
            .setShadow(true)
            .setHideOnClickOutside(true)
            .setHideOnAction(true)
            .setHideOnFrameResize(true)
            .setHideOnCloseClick(true)
            .setHideOnKeyOutside(true)
            .setBlockClicksThroughBalloon(true)
            .setCloseButtonEnabled(false)
            .createBalloon()

        private fun getMaxWidth(project: Project?): Int {
            val maxWidth = (WindowManager.getInstance().getFrame(project)?.width ?: 0) * 0.45
            return maxOf(maxWidth.toInt(), MAX_WIDTH)
        }
    }
}
