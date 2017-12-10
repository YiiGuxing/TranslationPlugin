package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.model.QueryResult
import cn.yiiguxing.plugin.translate.util.isNullOrBlank
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.BalloonBuilder
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import com.intellij.ui.BalloonImpl
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.util.Consumer
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.PositionTracker
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.event.PopupMenuEvent

class TranslationBalloon(private val editor: Editor, private val caretRangeMarker: RangeMarker) : View {

    private val project: Project? = editor.project
    private val presenter: Presenter = TranslationPresenter(this)

    private val layout = FixedSizeCardLayout()
    private val contentPanel = JBPanel<JBPanel<*>>(layout)
    private val processPane = ProcessComponent("Querying...")
    private val errorPane = JEditorPane()
    private val translationPane = JEditorPane()

    private var balloon: Balloon? = null
    private var targetLocation: RelativePoint? = null

    private var _disposed = false
    override val disposed get() = _disposed


    init {
        initErrorPane()
        initContentPanel()

        updateCaretPosition()


        Disposer.register(balloon!!, processPane)
        project?.let {
            Disposer.register(it, this)
        }

    }

    private fun initContentPanel() {
        contentPanel
                .withFont(Styles.defaultFont)
                .andTransparent()
                .apply {
                    add(CARD_PROCESSING, processPane)
                    add(CARD_ERROR, errorPane)
                }
    }

    private fun initErrorPane() {
        errorPane.apply {
            contentType = "text/html"
            editorKit = Styles.errorHTMLKit
            isEditable = false
            isOpaque = false

            setMaxSize()
            addHyperlinkListener(object : HyperlinkAdapter() {
                override fun hyperlinkActivated(hyperlinkEvent: HyperlinkEvent) {
                    if (HTML_DESCRIPTION_SETTINGS == hyperlinkEvent.description) {
                        this@TranslationBalloon.hide()
                        OptionsConfigurable.showSettingsDialog(project)
                    }
                }
            })
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
        _disposed = true

        balloon?.hide()
        balloon = null

        caretRangeMarker.dispose()
    }

    fun hide() {
        if (!disposed) {
            Disposer.dispose(this)
        }
    }

    private fun buildBalloon(): BalloonBuilder = JBPopupFactory
            .getInstance()
            .createDialogBalloonBuilder(contentPanel, null)
            .setHideOnClickOutside(true)
            .setShadow(true)
            .setHideOnKeyOutside(true)
            .setBlockClicksThroughBalloon(true)
            .setBorderInsets(JBUI.insets(20, 20, 16, 20))

    fun show(text: String) {
        check(!disposed) { "Balloon was disposed." }

        balloon = buildBalloon().setCloseButtonEnabled(false).createBalloon()

        editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
        showBalloon(balloon!!)

        presenter.translate(text)
    }

    private fun showBalloon(balloon: Balloon) {
        val popupFactory = JBPopupFactory.getInstance()
        balloon.show(object : PositionTracker<Balloon>(editor.contentComponent) {
            override fun recalculateLocation(`object`: Balloon): RelativePoint? {
                if (targetLocation != null && !popupFactory.isBestPopupLocationVisible(editor)) {
                    return targetLocation
                }

                updateCaretPosition()

                val target = popupFactory.guessBestPopupLocation(editor)
                val visibleArea = editor.scrollingModel.visibleArea
                val point = Point(visibleArea.x, visibleArea.y)
                SwingUtilities.convertPointToScreen(point, component)

                val screenPoint = target.screenPoint
                val y = screenPoint.y - point.y
                if (targetLocation != null && y + balloon.preferredSize.getHeight() > visibleArea.height) {
                    //FIXME 只是判断垂直方向，没有判断水平方向，但水平方向问题不是很大。
                    //FIXME 垂直方向上也只是判断Balloon显示在下方的情况，还是有些小问题。
                    return targetLocation
                }

                targetLocation = RelativePoint(Point(screenPoint.x, screenPoint.y))
                return targetLocation
            }
        }, Balloon.Position.below)
    }

    override fun showStartTranslate(query: String) {}

    override fun showResult(query: String, result: QueryResult) {
        if (balloon != null) {
            if (balloon!!.isDisposed) {
                return
            }

            balloon!!.hide(true)
        } else {
            return
        }

        if (disposed) {
            return
        }

        val translationDialog = TranslationManager.instance.translationDialog
        translationDialog?.query(query)


        val resultText = object : JTextPane() {
            override fun paint(g: Graphics?) {
                // 还原设置图像背景后的图形上下文，使图像背景在JTextPane上失效。
                super.paint(IdeBackgroundUtil.getOriginalGraphics(g!!))
            }
        }
        resultText.isEditable = false
        resultText.background = UIManager.getColor("Panel.background")
        setFont(resultText)

        resultText.caretPosition = 0

        val scrollPane = JBScrollPane(resultText)
        scrollPane.border = JBEmptyBorder(0)
        scrollPane.verticalScrollBar = scrollPane.createVerticalScrollBar()
        scrollPane.horizontalScrollBar = scrollPane.createHorizontalScrollBar()

        updateCaretPosition()
        val balloon = buildBalloon().createBalloon() as BalloonImpl
        val showPoint = JBPopupFactory.getInstance().guessBestPopupLocation(editor)
        createPinButton(balloon, showPoint, query)
        showBalloon(balloon)
        setPopupMenu(resultText)

        this.balloon = balloon
    }

    private fun setFont(component: JComponent) {
        val settings = Settings.instance
        if (settings.isOverrideFont) {
            val fontFamily = settings.primaryFontFamily
            if (!fontFamily.isNullOrBlank()) {
                component.font = JBUI.Fonts.create(fontFamily, 14)
                return
            }
        }
        component.font = JBUI.Fonts.label(14f)
    }

    private fun showOnTranslationDialog(text: String?) {
        hide()
        val dialog = TranslationManager.instance.showDialog(editor.project)
        if (!text.isNullOrBlank()) {
            dialog.query(text)
        }
    }

    private fun setPopupMenu(textPane: JTextPane) {
        val menu = JBPopupMenu()

        val copy = JBMenuItem("Copy", Icons.Copy)
        copy.addActionListener { e -> textPane.copy() }

        val query = JBMenuItem("Query", Icons.Translate)
        query.addActionListener { e ->
            val selectedText = textPane.selectedText
            showOnTranslationDialog(selectedText)
        }

        menu.add(copy)
        menu.add(query)
        menu.addPopupMenuListener(object : PopupMenuListenerAdapter() {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                val hasSelectedText = !textPane.selectedText.isNullOrBlank()
                copy.isEnabled = hasSelectedText
                query.isEnabled = hasSelectedText
            }
        })

        textPane.componentPopupMenu = menu
    }

    private fun createPinButton(balloon: BalloonImpl, showPoint: RelativePoint, query: String) {
        balloon.setActionProvider(object : BalloonImpl.ActionProvider {
            private val icon = Icons.Pin
            private val pinButton = balloon.ActionButton(icon, icon, null, Consumer {
                if (it.clickCount == 1) {
                    showOnTranslationDialog(query)
                }
            })

            override fun createActions(): List<BalloonImpl.ActionButton> {
                return listOf(pinButton)
            }

            override fun layout(lpBounds: Rectangle) {
                if (pinButton.isVisible) {
                    val iconWidth = icon.iconWidth
                    val iconHeight = icon.iconHeight
                    val margin = JBUI.scale(3)
                    val x = lpBounds.x + lpBounds.width - iconWidth - margin
                    val y = lpBounds.y + margin

                    val rectangle = Rectangle(x, y, iconWidth, iconHeight)
                    val border = balloon.shadowBorderInsets
                    rectangle.x -= border.left

                    // FIXME 由于现在的Balloon是可以移动的，所以showPoint不再那么准确了，可以会使得PinButton显示位置不对。
                    val location = targetLocation ?: showPoint
                    val showX = location.point.x
                    val showY = location.point.y
                    // 误差
                    val offset = JBUI.scale(1)
                    val atRight = showX <= lpBounds.x + offset
                    val atLeft = showX >= lpBounds.x + lpBounds.width - offset
                    val below = lpBounds.y >= showY
                    val above = lpBounds.y + lpBounds.height <= showY
                    if (atRight || atLeft || below || above) {
                        rectangle.y += border.top
                    }

                    pinButton.bounds = rectangle
                }
            }
        })
    }

    override fun showError(query: String, error: String) {
        if (balloon == null || balloon!!.isDisposed)
            return

        val text = JEditorPane()

        text.text = error

        balloon!!.revalidate()
    }

    companion object {

        private const val MIN_WIDTH = 200
        private const val MAX_WIDTH = 500
        private const val MAX_HEIGHT = 600

        private const val CARD_PROCESSING = "processing"
        private const val CARD_ERROR = "error"
        private const val CARD_TRANSLATION = "translation"

        private fun JComponent.setMaxSize() {
            maximumSize = Dimension(JBUI.scale(MAX_WIDTH), Int.MAX_VALUE)
        }
    }
}
