package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.tts.TextToSpeech
import cn.yiiguxing.plugin.translate.util.copyToClipboard
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.util.isNullOrBlank
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.util.ui.*
import java.awt.AWTEvent
import java.awt.Color
import java.awt.Component
import java.awt.Component.RIGHT_ALIGNMENT
import java.awt.Component.TOP_ALIGNMENT
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.HyperlinkEvent

class TranslationBalloon(
        private val editor: Editor,
        private val caretRangeMarker: RangeMarker,
        private val text: String
) : View {

    private val project: Project? = editor.project
    private val presenter: Presenter = TranslationPresenter(this)
    private val settings: Settings = Settings.instance

    private val layout = FixedSizeCardLayout()
    private val contentPanel = JBPanel<JBPanel<*>>(layout)
    private val errorPanel = NonOpaquePanel(FrameLayout())
    private val errorPane = JEditorPane()
    private val processPane = ProcessComponent("Querying...", JBUI.insets(INSETS))
    private val translationContentPane = NonOpaquePanel(FrameLayout())
    private val translationPane = BalloonTranslationPanel(settings)
    private val pinButton = ActionLink(icon = Icons.Pin) { showOnTranslationDialog(text) }
    private val copyErrorLink = ActionLink(icon = Icons.CopyToClipboard) {
        lastError?.copyToClipboard()
        hide()
    }

    private val balloon: Balloon
    private var targetLocation: RelativePoint? = null

    private var isShowing = false
    private var _disposed = false
    override val disposed get() = _disposed
    private var ttsDisposable: Disposable? = null

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
        initTranslationContent()
        initContentPanel()

        balloon = createBalloon(contentPanel)
        initActions()

        updateCaretPosition()

        project?.let { Disposer.register(it, balloon) }
        // 如果使用`Disposer.register(balloon, this)`的话，
        // `TranslationBalloon`在外部以子`Disposable`再次注册时将会使之无效。
        Disposer.register(balloon, Disposable { Disposer.dispose(this) })
        Disposer.register(this, processPane)
    }

    private fun initContentPanel() = contentPanel
            .withFont(UI.defaultFont)
            .andTransparent()
            .apply {
                add(CARD_PROCESSING, processPane)
                add(CARD_TRANSLATION, translationContentPane)
                add(CARD_ERROR, errorPanel)
            }


    private fun initTranslationContent() = translationContentPane.apply {
        add(pinButton.apply {
            isVisible = false
            alignmentX = RIGHT_ALIGNMENT
            alignmentY = TOP_ALIGNMENT
        })
        add(translationPane.component.apply {
            border = JBEmptyBorder(16, 16, 10, 16)
        })
    }

    private fun initActions() = with(translationPane) {
        onRevalidate { balloon.revalidate() }
        onLanguageChanged { src, target -> presenter.translate(src, target, text) }
        onNewTranslate { showOnTranslationDialog(it) }
        onTextToSpeech { text, lang ->
            ttsDisposable = TextToSpeech.INSTANCE.speak(project, text, lang)
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_MOTION_EVENT_MASK)
    }

    private fun initErrorPanel() {
        errorPane.apply {
            contentType = "text/html"
            isEditable = false
            isOpaque = false
            editorKit = UI.errorHTMLKit
            border = JBEmptyBorder(20, 30, 20, 30)
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
            alignmentX = Component.RIGHT_ALIGNMENT
            alignmentY = Component.TOP_ALIGNMENT
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

    private fun updateCaretPosition() {
        with(caretRangeMarker) {
            if (isValid) {
                val offset = Math.round((startOffset + endOffset) / 2f)
                editor.apply {
                    val position = offsetToVisualPosition(offset)
                    putUserData<VisualPosition>(PopupFactoryImpl.ANCHOR_POPUP_POSITION, position)
                }
            }
        }
    }

    override fun dispose() {
        if (disposed) {
            return
        }

        _disposed = true
        isShowing = false

        balloon.hide()
        caretRangeMarker.dispose()
        ttsDisposable?.let { Disposer.dispose(it) }
        Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener)

        println("Balloon disposed.")
    }

    fun hide() {
        if (!disposed) {
            Disposer.dispose(this)
        }
    }

    fun show() {
        check(!disposed) { "Balloon was disposed." }

        if (!isShowing) {
            isShowing = true

            editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
            showBalloon()
            presenter.translate(Lang.AUTO, presenter.primaryLanguage, text)
        }
    }

    private fun showCard(card: String) {
        // 相信我，我发誓，没有什么能比使用swing开发更糟糕的了
        invokeLater {
            layout.show(contentPanel, card)
            with(balloon) {
                revalidate()
                // 大小还是没有调整好，再刷一次
                invokeLater { revalidate() }
            }
        }
    }

    private fun showOnTranslationDialog(text: String?) {
        hide()
        val dialog = TranslationManager.instance.showDialog(editor.project)
        if (!text.isNullOrBlank()) {
            dialog.translate(text)
        }
    }

    override fun showStartTranslate(text: String) {
        if (!disposed) {
            showCard(CARD_PROCESSING)
        }
    }

    private fun showBalloon() {
        val popupFactory = JBPopupFactory.getInstance()
        balloon.show(object : PositionTracker<Balloon>(editor.contentComponent) {
            override fun recalculateLocation(balloon: Balloon): RelativePoint? {
                if (targetLocation != null && !popupFactory.isBestPopupLocationVisible(editor)) {
                    return targetLocation
                }

                updateCaretPosition()

                targetLocation = popupFactory.guessBestPopupLocation(editor)
                return targetLocation
            }
        }, Balloon.Position.below)
    }

    override fun showTranslation(translation: Translation) {
        if (disposed) {
            return
        }

        val (source, target) = presenter.supportedLanguages
        translationPane.apply {
            srcLang = Lang.AUTO
            setSupportedLanguages(source, target)
            this.translation = translation
        }
        showCard(CARD_TRANSLATION)
    }

    override fun showError(errorMessage: String, throwable: Throwable) {
        if (!disposed) {
            lastError = throwable
            errorPane.text = errorMessage
            showCard(CARD_ERROR)
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
    }
}
