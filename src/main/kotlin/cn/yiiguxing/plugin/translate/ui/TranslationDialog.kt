package cn.yiiguxing.plugin.translate.ui


import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.UI.errorHTMLKit
import cn.yiiguxing.plugin.translate.util.isNullOrBlank
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.*
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import java.awt.AWTEvent
import java.awt.CardLayout
import java.awt.Point
import java.awt.Toolkit
import java.awt.event.*
import javax.swing.*
import javax.swing.border.LineBorder
import javax.swing.event.HyperlinkEvent
import javax.swing.text.JTextComponent

class TranslationDialog(private val project: Project?)
    : TranslationDialogForm(project), View, HistoriesChangedListener {

    private val settings = Settings.instance

    private val processPane = ProcessComponent("Querying...")
    private val translationPane = DialogTranslationPanel(settings)
    private val translationPanel = ScrollPane(translationPane.component)
    private val closeButton = ActionLink(icon = Icons.Close, hoveringIcon = Icons.ClosePressed) { close() }

    private val translationPresenter: Presenter = TranslationPresenter(this)
    private val inputModel: MyModel = MyModel(translationPresenter.histories)

    private var _disposed: Boolean = false
    override val disposed: Boolean get() = _disposed

    private val focusManager = IdeFocusManager.getInstance(project)
    private lateinit var windowListener: WindowListener
    private lateinit var activityListener: AWTEventListener
    private var lastMoveWasInsideDialog: Boolean = false

    init {
        isModal = false
        setUndecorated(true)
        peer.setContentPane(createCenterPanel())

        initUIComponents()
        setListeners()
        installEnterHook()

        Disposer.register(this, processPane)
    }

    override fun createCenterPanel(): JComponent = component.apply {
        preferredSize = JBDimension(WIDTH, HEIGHT)
        border = BORDER_ACTIVE
    }

    override fun getPreferredFocusedComponent(): JComponent? = inputComboBox

    private fun initUIComponents() {
        rootPane.andTransparent()

        initTitle()
        initInputComboBox()
        initLanguagePanel()

        translateButton.apply {
            icon = Icons.Translate
            addActionListener { translate(inputComboBox.editor.item.toString()) }
        }

        val contentBackground = JBColor(0xFFFFFF, 0x2B2B2B)
        mainContentPanel.apply {
            border = BORDER_ACTIVE
            background = contentBackground
        }
        translationPane.component.border = JBEmptyBorder(10, 10, 5, 10)
        translationPanel.apply {
            val view = viewport.view
            viewport = ScrollPane.Viewport(contentBackground, 10)
            viewport.view = view
        }
        messagePane.apply {
            editorKit = errorHTMLKit
            addHyperlinkListener(object : HyperlinkAdapter() {
                override fun hyperlinkActivated(hyperlinkEvent: HyperlinkEvent) {
                    if (HTML_DESCRIPTION_SETTINGS == hyperlinkEvent.description) {
                        close()
                        OptionsConfigurable.showSettingsDialog(project)
                    }
                }
            })
        }

        contentContainer.apply {
            add(CARD_PROCESSING, processPane)
            add(CARD_TRANSLATION, translationPanel)
        }
    }

    private fun initTitle() {
        closeButton.apply {
            isVisible = false
            disabledIcon = Icons.ClosePressed
        }
        titlePanel.apply {
            setText("Translation")
            setActive(true)

            setButtonComponent(object : ActiveComponent {
                override fun getComponent(): JComponent = NonOpaquePanel().apply {
                    preferredSize = closeButton.preferredSize
                    add(closeButton)
                }

                override fun setActive(active: Boolean) {
                    closeButton.isEnabled = active
                }
            }, JBEmptyBorder(0, 0, 0, 2))

            WindowMoveListener(this).let {
                addMouseListener(it)
                addMouseMotionListener(it)
            }
        }
    }

    private fun initInputComboBox() = with(inputComboBox) {
        model = inputModel
        renderer = ComboRenderer()

        (editor.editorComponent as JTextComponent).let {
            it.addFocusListener(object : FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    it.selectAll()
                }

                override fun focusLost(e: FocusEvent?) {
                    it.select(0, 0)
                }
            })
        }

        addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                onTranslate()
            }
        }
    }

    private fun initLanguagePanel() {
        languagePanel.apply {
            background = JBColor(0xEEF1F3, 0x353739)
            border = SideBorder(JBColor(0xB1B1B1, 0x282828), SideBorder.BOTTOM)
        }

        sourceLangComboBox.apply {
            andTransparent()
            foreground = JBColor(0x555555, 0xACACAC)
            ui = LangComboBoxUI(this, SwingConstants.CENTER)
            model = CollectionComboBoxModel<Lang>(Lang.values().asList())
        }
        targetLangComboBox.apply {
            andTransparent()
            foreground = JBColor(0x555555, 0xACACAC)
            ui = LangComboBoxUI(this, SwingConstants.CENTER)
            model = CollectionComboBoxModel<Lang>(Lang.values().asList())
        }
    }

    private fun installEnterHook() {
        object : DumbAwareAction() {
            override fun actionPerformed(e: AnActionEvent) {
                translateButton.doClick()
            }
        }.registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), component, disposable)
    }

    private fun setListeners() {
        windowListener = object : WindowAdapter() {
            override fun windowActivated(e: WindowEvent) {
                titlePanel.setActive(true)
                component.border = BORDER_ACTIVE
                focusManager.requestFocus(inputComboBox, true)
            }

            override fun windowDeactivated(e: WindowEvent) {
                titlePanel.setActive(false)
                component.border = BORDER_PASSIVE
            }

            override fun windowClosed(e: WindowEvent) {
                // 在对话框上打开此对话框时，关闭主对话框时导致此对话框也跟着关闭，
                // 但资源没有释放干净，回调也没回完整，再次打开的话就会崩溃
                close()
            }
        }
        window.addWindowListener(windowListener)

        activityListener = AWTEventListener {
            if (it is MouseEvent && it.id == MouseEvent.MOUSE_MOVED) {
                val inside = isInside(RelativePoint(it))
                if (inside != lastMoveWasInsideDialog) {
                    closeButton.isVisible = inside
                    lastMoveWasInsideDialog = inside
                }
            }
        }
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener, AWTEvent.MOUSE_MOTION_EVENT_MASK)

        ApplicationManager
                .getApplication()
                .messageBus
                .connect(this)
                .subscribe(HistoriesChangedListener.TOPIC, this)
    }

    private fun isInside(target: RelativePoint): Boolean {
        val cmp = target.originalComponent
        return when {
            !cmp.isShowing -> true
            cmp is MenuElement -> false
            UIUtil.isDescendingFrom(cmp, window) -> true
            !isShowing -> true
            else -> {
                val point = target.screenPoint.also {
                    SwingUtilities.convertPointFromScreen(it, window)
                }
                window.contains(point)
            }
        }
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

        window.removeWindowListener(windowListener)
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener)

        Disposer.dispose(this)
        println("Dialog disposed.")
    }

    override fun show() {
        check(!disposed) { "Dialog was disposed." }
        if (!isShowing) {
            super.show()
        }

        update()
        registerKeyboardShortcuts()
        focusManager.requestFocus(window, true)
    }

    private fun update() {
        if (isShowing && inputModel.size > 0) {
            inputComboBox.selectedIndex = 0
        }
    }

    private fun registerKeyboardShortcuts() {
        rootPane?.apply {
            val closeAction = ActionListener { close() }
            val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
            registerKeyboardAction(closeAction, keyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW)

            val shortcuts = CommonShortcuts.getCloseActiveWindow()
            ActionUtil.registerForEveryKeyboardShortcut(this, closeAction, shortcuts)
        }
    }

    fun translate(query: String?) {
        if (!disposed && query != null && !query.isBlank()) {
            translationPresenter.translate(query)
        }
    }

    private fun onTranslate() {
        if (disposed) {
            return
        }

        (inputComboBox.selectedItem as String?)?.let {
            if (it.isNotBlank()) {
                translationPresenter.translate(it)
            }
        }
    }

    override fun onHistoriesChanged() {
        if (!disposed) {
            inputModel.fireContentsChanged()
        }
    }

    override fun onHistoryItemChanged(newHistory: String) {
        if (!disposed) {
            inputComboBox.selectedItem = newHistory
        }
    }

    private fun setLanguageComponentsEnable(enabled: Boolean) {
        sourceLangComboBox.isEnabled = enabled
        switchButton.isEnabled = enabled
        targetLangComboBox.isEnabled = enabled
    }

    private fun showCard(card: String) {
        (contentContainer.layout as CardLayout).show(contentContainer, card)
    }

    override fun showStartTranslate(text: String) {
        if (disposed) {
            return
        }

        inputModel.setSelectedItem(text)
        showCard(CARD_PROCESSING)
        setLanguageComponentsEnable(false)
    }

    override fun showTranslation(text: String, translation: Translation) {
        if (disposed) {
            return
        }

        translationPane.apply {
            srcLang = Lang.AUTO
            this.translation = translation
        }
        translationPanel.viewport.viewPosition = Point(0, 0)
        showCard(CARD_TRANSLATION)
        setLanguageComponentsEnable(true)
    }

    override fun showError(text: String, errorMessage: String) {
        if (disposed) {
            return
        }

        messagePane.text = errorMessage
        showCard(CARD_MASSAGE)
        setLanguageComponentsEnable(true)
    }

    private class MyModel(private val fullList: List<String>) : AbstractListModel<String>(), ComboBoxModel<String> {
        private var selectedItem: Any? = null

        override fun getElementAt(index: Int): String = fullList[index]

        override fun getSize(): Int = fullList.size

        override fun getSelectedItem(): Any? = selectedItem

        override fun setSelectedItem(anItem: Any) {
            selectedItem = anItem
            fireContentsChanged()
        }

        internal fun fireContentsChanged() {
            fireContentsChanged(this, -1, -1)
        }
    }

    private inner class ComboRenderer : ListCellRendererWrapper<String>() {
        private val builder = StringBuilder()

        override fun customize(list: JList<*>, value: String?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
            if (list.width == 0 || value.isNullOrBlank()) { // 在没有确定大小之前不设置真正的文本,否则控件会被过长的文本撑大.
                setText(null)
            } else {
                setRenderText(value!!)
            }
        }

        private fun setRenderText(value: String) {
            val text = with(builder) {
                setLength(0)

                append("<html><b>")
                append(value)
                append("</b>")
                translationPresenter.getCache(value)?.let {
                    append("  -  <i><small>")
                    append(it.trans)
                    append("</small></i>")
                }
                builder.append("</html>")

                toString()
            }
            setText(text)
        }
    }

    companion object {
        private val WIDTH = 400
        private val HEIGHT = 500

        private val BORDER_ACTIVE = LineBorder(JBColor(JBColor.GRAY, Gray._35))
        private val BORDER_PASSIVE = LineBorder(JBColor(JBColor.LIGHT_GRAY, Gray._75))

        private const val CARD_MASSAGE = "message"
        private const val CARD_PROCESSING = "processing"
        private const val CARD_TRANSLATION = "translation"
    }
}
