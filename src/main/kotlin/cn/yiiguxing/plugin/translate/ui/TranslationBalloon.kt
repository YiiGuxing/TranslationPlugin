package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.balloon.BalloonImpl
import cn.yiiguxing.plugin.translate.ui.balloon.BalloonPopupBuilder
import cn.yiiguxing.plugin.translate.ui.icon.Spinner
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.TranslationStates
import cn.yiiguxing.plugin.translate.util.invokeLater
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.*
import java.awt.AWTEvent
import java.awt.Color
import java.awt.Component.RIGHT_ALIGNMENT
import java.awt.Component.TOP_ALIGNMENT
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.MenuElement
import javax.swing.SwingUtilities

class TranslationBalloon(
    private val editor: Editor,
    private val text: String
) : View, SettingsChangeListener {

    private val project: Project? = editor.project
    private val presenter: Presenter = TranslationPresenter(this)

    private val layout = FixedSizeCardLayout()
    private val contentPanel = JBPanel<JBPanel<*>>(layout)
    private val errorPanel = TranslationFailedComponent().apply {
        maximumSize = JBDimension(MAX_WIDTH, Int.MAX_VALUE)
    }
    private val processPane = ProcessComponent(Spinner(), JBUI.insets(INSETS, INSETS * 2))
    private val translationContentPane = NonOpaquePanel(FrameLayout())
    private val translationPane = BalloonTranslationPane(project, Settings, getMaxWidth(project))
    private val pinButton = ActionLink(icon = AllIcons.General.Pin_tab) { pin() }

    private val balloon: Balloon

    private var isShowing = false
    private var _disposed = false
    override val disposed get() = _disposed || balloon.isDisposed

    private var lastMoveWasInsideBalloon = false
    private val eventListener = AWTEventListener {
        if (it is MouseEvent && it.id == MouseEvent.MOUSE_MOVED) {
            val inside = isInsideBalloon(RelativePoint(it))
            if (inside != lastMoveWasInsideBalloon) {
                lastMoveWasInsideBalloon = inside
                pinButton.isVisible = inside
            }
        }
    }

    init {
        initTranslationPanel()
        initContentPanel()

        balloon = createBalloon(contentPanel)
        initActions()

        Disposer.register(TranslationUIManager.disposable(project), balloon)
        // 如果使用`Disposer.register(balloon, this)`的话，
        // `TranslationBalloon`在外部以子`Disposable`再次注册时将会使之无效。
        Disposer.register(balloon) { Disposer.dispose(this) }
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
                border = JBEmptyBorder(5, 0, 0, 0)
                isVisible = false
                alignmentX = RIGHT_ALIGNMENT
                alignmentY = TOP_ALIGNMENT
            })
            add(translationPane)
        }
    }

    private fun initActions() {
        with(translationPane) {
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
        }

        errorPanel.onRetry { onTranslate() }
        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_MOTION_EVENT_MASK)
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
    }

    fun hide() {
        if (!disposed) {
            Disposer.dispose(this)
        }
    }

    fun show(tracker: PositionTracker<Balloon>, position: Balloon.Position) {
        check(!disposed) { "Balloon has been disposed." }

        if (!presenter.translator.checkConfiguration()) {
            hide()
            return
        }

        if (!isShowing) {
            isShowing = true
            balloon.show(tracker, position)
            onTranslate()
        }
    }

    private fun onTranslate() {
        val targetLang = presenter.getTargetLang(text)
        translate(Lang.AUTO, targetLang)
    }

    private fun translate(srcLang: Lang, targetLang: Lang) = presenter.translate(text, srcLang, targetLang)

    private fun showCard(card: String) {
        // 使用`SwingUtilities.invokeLater`似乎要比使用`Application.invokeLater`更好，
        // `Application.invokeLater`有时候会得不到想要的效果，UI组件不会自动调整尺寸。
        SwingUtilities.invokeLater {
            if (!disposed) {
                layout.show(contentPanel, card)
                if (card == CARD_PROCESSING) {
                    processPane.resume()
                } else {
                    processPane.suspend()
                }
                balloon.revalidate()
            }
        }
    }

    fun pin() {
        val readyTranslation = translationPane.translation ?: return
        hide()

        TranslationStates.pinTranslationDialog = true
        TranslationUIManager.showDialog(editor.project)
            .applyTranslation(readyTranslation)
    }

    private fun showOnTranslationDialog(text: String, srcLang: Lang, targetLang: Lang) {
        hide()

        TranslationStates.pinTranslationDialog = true
        TranslationUIManager.showDialog(editor.project)
            .translate(text, srcLang, targetLang)
    }

    override fun onTranslatorChanged(settings: Settings, translationEngine: TranslationEngine) {
        hide()
    }

    override fun showStartTranslate(request: Presenter.Request, text: String) {
        if (!disposed) {
            showCard(CARD_PROCESSING)
            errorPanel.update(null as Throwable?)
        }
    }

    override fun showTranslation(request: Presenter.Request, translation: Translation, fromCache: Boolean) {
        if (!disposed) {
            translationPane.translation = translation
            showCard(CARD_TRANSLATION)
        }
    }

    override fun showError(request: Presenter.Request, throwable: Throwable) {
        if (!disposed) {
            errorPanel.update(throwable)
            showCard(CARD_ERROR)
        }
    }

    companion object {

        private const val MAX_WIDTH = 500
        private const val MIN_ERROR_PANEL_WIDTH = 300
        private const val INSETS = 20

        private const val CARD_PROCESSING = "processing"
        private const val CARD_ERROR = "error"
        private const val CARD_TRANSLATION = "translation"

        private fun createBalloon(content: JComponent): Balloon = BalloonPopupBuilder(content)
            .setShadow(true)
            .setDialogMode(true)
            .setRequestFocus(true)
            .setHideOnAction(true)
            .setHideOnCloseClick(true)
            .setHideOnKeyOutside(false)
            .setHideOnFrameResize(true)
            .setHideOnClickOutside(true)
            .setBlockClicksThroughBalloon(true)
            .setCloseButtonEnabled(false)
            .setAnimationCycle(200)
            .setBorderColor(Color.darkGray.toAlpha(35))
            .setFillColor(JBUI.CurrentTheme.CustomFrameDecorations.paneBackground())
            .createBalloon()
            .apply {
                this as BalloonImpl
                setHideListener { hide() }
            }

        private fun getMaxWidth(project: Project?): Int {
            val maxWidth = (WindowManager.getInstance().getFrame(project)?.width ?: 0) * 0.45
            return maxOf(maxWidth.toInt(), MAX_WIDTH)
        }
    }
}
