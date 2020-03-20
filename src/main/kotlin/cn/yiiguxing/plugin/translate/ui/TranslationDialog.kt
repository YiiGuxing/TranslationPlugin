package cn.yiiguxing.plugin.translate.ui


import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.YoudaoTranslator
import cn.yiiguxing.plugin.translate.ui.form.TranslationDialogForm
import cn.yiiguxing.plugin.translate.ui.settings.OptionsConfigurable
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.*
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.Icons
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.LineBorder
import javax.swing.event.HyperlinkEvent
import javax.swing.event.PopupMenuEvent
import javax.swing.text.JTextComponent

class TranslationDialog(private val project: Project?) : TranslationDialogForm(project), View, HistoriesChangedListener,
    SettingsChangeListener {

    private val processPane = ProcessComponent("Querying...")
    private val translationPane = DialogTranslationPane(project, Settings)
    private val translationPanel = ScrollPane(translationPane)
    private val closeButton = ActionLink(icon = Icons.Close, hoveringIcon = Icons.ClosePressed) { close() }

    private val presenter: Presenter = TranslationPresenter(this)
    private val inputModel: MyModel = MyModel(presenter.histories)

    private var ignoreLanguageEvent: Boolean = false
    private var ignoreInputEvent: Boolean = false
    private var ignoreHistoryEvent: Boolean = false

    private var _disposed: Boolean = false
    override val disposed: Boolean get() = _disposed

    private val focusManager: IdeFocusManager = IdeFocusManager.getInstance(project)
    private lateinit var windowListener: WindowListener
    private lateinit var activityListener: AWTEventListener
    private var lastMoveWasInsideDialog: Boolean = false
    private var lastScrollValue: Int = 0
    private var lastError: Throwable? = null

    init {
        isModal = false
        setUndecorated(true)
        peer.setContentPane(createCenterPanel())

        setResizable()
        initUIComponents()
        setListeners()

        Disposer.register(this, processPane)
        Disposer.register(this, translationPane)
    }

    override fun createCenterPanel(): JComponent = component.apply {
        preferredSize = JBDimension(WIDTH, HEIGHT)
        border = BORDER
    }

    override fun getPreferredFocusedComponent(): JComponent? = inputComboBox

    private fun setResizable() {
        val resizableListener = ResizableListener()
        component.apply {
            addMouseMotionListener(resizableListener)
            addMouseListener(resizableListener)
        }
    }

    private fun initUIComponents() {
        rootPane.andTransparent()

        initTitle()
        initInputComboBox()
        initLanguagePanel()
        initTranslationPanel()
        initMessagePane()
        initFont()

        translateButton.apply {
            icon = Icons.Translate
            addActionListener { translateInternal(inputComboBox.editor.item.toString()) }
            minimumSize = JBDimension(45, 0)
            maximumSize = JBDimension(45, Int.MAX_VALUE)
            preferredSize = JBDimension(45, (preferredSize.height / JBUI.scale(1f)).toInt())
        }
        mainContentPanel.apply {
            border = BORDER
            background = CONTENT_BACKGROUND
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
        val scale = JBUI.scale(1f)
        val width = (preferredSize.width / scale).toInt()
        minimumSize = JBDimension(width, 0)
        maximumSize = JBDimension(width, Int.MAX_VALUE)
        preferredSize = JBDimension(width, (preferredSize.height / scale).toInt())
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
            if (!ignoreInputEvent && it.stateChange == ItemEvent.SELECTED) {
                onTranslate()
            }
        }
    }

    private fun initLanguagePanel() {
        languagePanel.apply {
            background = UI.getColor("ToolWindow.Header.background", JBColor(0xEEF1F3, 0x353739))
                ?.alphaBlend(CONTENT_BACKGROUND, 0.6f)
            val borderColor = UI.getBordersColor(JBColor(0xB1B1B1, 0x282828))
            border = SideBorder(borderColor, SideBorder.BOTTOM)
        }

        presenter.supportedLanguages.let { (source, target) ->
            sourceLangComboBox.init(source)
            targetLangComboBox.init(target)
        }

        swapButton.apply {
            icon = Icons.Swap
            disabledIcon = Icons.SwapDisabled
            setHoveringIcon(Icons.SwapHovering)

            fun ComboBox<Lang>.swap() {
                selected = Lang.ENGLISH.takeUnless { it == selected } ?: presenter.primaryLanguage
            }

            setListener({ _, _ ->
                val nonAutoSrc = Lang.AUTO != sourceLangComboBox.selected
                val nonAutoTarget = Lang.AUTO != targetLangComboBox.selected

                if (nonAutoSrc && nonAutoTarget) {
                    sourceLangComboBox.selected = targetLangComboBox.selected
                } else if (nonAutoSrc) {
                    sourceLangComboBox.swap()
                } else if (nonAutoTarget) {
                    targetLangComboBox.swap()
                }
            }, null)
        }
    }

    private fun ComboBox<Lang>.init(languages: List<Lang>) {
        andTransparent()
        foreground = JBColor(0x555555, 0xACACAC)
        ui = LangComboBoxUI(this, SwingConstants.CENTER)
        model = LanguageListModel(languages)

        fun ComboBox<Lang>.swap(old: Any?, new: Any?) {
            if (new == selectedItem && old != Lang.AUTO && new != Lang.AUTO) {
                ignoreLanguageEvent = true
                selectedItem = old
                ignoreLanguageEvent = false
            }
        }

        var old: Any? = selected
        addItemListener {
            when (it.stateChange) {
                ItemEvent.DESELECTED -> old = it.item
                ItemEvent.SELECTED -> {
                    if (!ignoreLanguageEvent) {
                        when (it.source) {
                            sourceLangComboBox -> targetLangComboBox.swap(old, it.item)
                            targetLangComboBox -> sourceLangComboBox.swap(old, it.item)
                        }

                        presenter.updateLastLanguages(sourceLangComboBox.selected!!, targetLangComboBox.selected!!)
                        updateSwitchButtonEnable()
                        onTranslate()
                    }
                }
            }
        }
    }

    private fun initTranslationPanel() {
        with(translationPane) {
            border = JBUI.Borders.empty(8)

            onNewTranslate { text, src, target ->
                val srcLang: Lang = if (sourceLangComboBox.selected == Lang.AUTO) Lang.AUTO else src
                val targetLang: Lang = if (targetLangComboBox.selected == Lang.AUTO) Lang.AUTO else target
                translate(text, srcLang, targetLang)
            }
            onFixLanguage { sourceLangComboBox.selected = it }
            onSpellFixed { spell -> translate(spell, null, null) }
            onBeforeFoldingExpand {
                lastScrollValue = translationPanel.verticalScrollBar.value
            }
            onRevalidate {
                invokeLater { translationPanel.verticalScrollBar.value = lastScrollValue }
            }
        }

        translationPanel.apply {
            val view = viewport.view
            viewport = ScrollPane.Viewport(CONTENT_BACKGROUND, 10)
            viewport.view = view
        }
    }

    private fun initMessagePane() = messagePane.run {
        editorKit = UI.errorHTMLKit
        addHyperlinkListener(object : HyperlinkAdapter() {
            override fun hyperlinkActivated(hyperlinkEvent: HyperlinkEvent) {
                if (HTML_DESCRIPTION_SETTINGS == hyperlinkEvent.description) {
                    close()
                    OptionsConfigurable.showSettingsDialog(project)
                }
            }
        })

        componentPopupMenu = cratePopupMenu()
        (parent as JComponent).componentPopupMenu = cratePopupMenu()
    }

    private fun cratePopupMenu() = JBPopupMenu().apply {
        val copyToClipboard = JBMenuItem("Copy to Clipboard", Icons.CopyToClipboard).apply {
            addActionListener { lastError?.copyToClipboard() }
        }
        add(copyToClipboard)
        addPopupMenuListener(object : PopupMenuListenerAdapter() {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                copyToClipboard.isEnabled = lastError != null
            }
        })
    }

    private fun initFont() {
        UI.primaryFont(12).let {
            inputComboBox.font = it
            sourceLangComboBox.font = it
            targetLangComboBox.font = it
            messagePane.font = it.biggerOn(3f)
        }
    }

    private fun setListeners() {
        windowListener = object : WindowAdapter() {
            override fun windowActivated(e: WindowEvent) {
                titlePanel.setActive(true)
                focusManager.requestFocus(inputComboBox, true)
            }

            override fun windowDeactivated(e: WindowEvent) {
                titlePanel.setActive(false)
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
            .let {
                it.subscribe(HistoriesChangedListener.TOPIC, this)
                it.subscribe(SettingsChangeListener.TOPIC, this)
            }
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
            ignoreInputEvent = true
            inputComboBox.selectedIndex = 0
            ignoreInputEvent = false
            translate(inputModel.getElementAt(0))
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

    private fun onTranslate() {
        if (disposed) {
            return
        }

        (inputComboBox.selectedItem as String?)?.let { translateInternal(it) }
    }

    private fun translateInternal(text: String) {
        presenter.translate(text, sourceLangComboBox.selected!!, targetLangComboBox.selected!!)
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
        translateInternal(text, srcLang, targetLang)
    }

    /**
     * 以指定的[源语言][src]和[目标语言][target]翻译指定的[内容][text]
     *
     * @param text 需要翻译的内容
     * @param src 源语言, `null`则使用当前选中的语言
     * @param target 目标语言, `null`则使用当前选中的语言
     */
    fun translate(text: String, src: Lang?, target: Lang?) {
        if (disposed || text.isBlank()) {
            return
        }

        lateinit var srcLang: Lang
        lateinit var targetLang: Lang

        presenter.supportedLanguages.let { (sourceList, targetList) ->
            srcLang = src?.takeIf { sourceList.contains(it) }
                ?: sourceLangComboBox.selected
                        ?: sourceList.first()
            targetLang = target?.takeIf { targetList.contains(it) }
                ?: targetLangComboBox.selected
                        ?: presenter.primaryLanguage
        }

        translateInternal(text, srcLang, targetLang)
    }

    private fun translateInternal(text: String, srcLang: Lang, targetLang: Lang) {
        sourceLangComboBox.setSelectLangIgnoreEvent(srcLang)
        targetLangComboBox.setSelectLangIgnoreEvent(targetLang)
        ignoreHistoryEvent = true
        presenter.translate(text, srcLang, targetLang)
        ignoreHistoryEvent = false
    }

    private fun ComboBox<Lang>.setSelectLangIgnoreEvent(lang: Lang) {
        ignoreLanguageEvent = true
        selected = lang
        ignoreLanguageEvent = false
    }

    override fun onTranslatorChanged(settings: Settings, translatorId: String) {
        presenter.supportedLanguages.let { (src, target) ->
            sourceLangComboBox.apply {
                val srcSelected = selected.takeIf { src.contains(it) } ?: src.first()
                model = CollectionComboBoxModel<Lang>(src, srcSelected)
            }
            targetLangComboBox.apply {
                val targetSelected = selected.takeIf { target.contains(it) } ?: presenter.primaryLanguage
                model = CollectionComboBoxModel<Lang>(target, targetSelected)
            }
        }
        onTranslate()
    }

    override fun onHistoriesChanged() {
        if (!disposed) {
            inputModel.fireContentsChanged()
        }
    }

    override fun onHistoryItemChanged(newHistory: String) {
        if (!disposed) {
            ignoreInputEvent = true
            inputComboBox.selectedItem = newHistory
            ignoreInputEvent = false

            if (!ignoreHistoryEvent) {
                translate(newHistory)
            }
        }
    }

    private fun updateSwitchButtonEnable(enabled: Boolean = true) {
        swapButton.isEnabled = enabled
                && (Lang.AUTO != sourceLangComboBox.selected || Lang.AUTO != targetLangComboBox.selected)
    }

    private fun setLanguageComponentsEnable(enabled: Boolean) {
        sourceLangComboBox.isEnabled = enabled
        updateSwitchButtonEnable(enabled)
        targetLangComboBox.isEnabled = enabled
    }

    private fun showCard(card: String) {
        (contentContainer.layout as CardLayout).show(contentContainer, card)
    }

    override fun showStartTranslate(request: Presenter.Request, text: String) {
        if (disposed) {
            return
        }

        inputModel.setSelectedItem(text)
        showCard(CARD_PROCESSING)
        setLanguageComponentsEnable(false)
    }

    override fun showTranslation(request: Presenter.Request, translation: Translation, fromCache: Boolean) {
        if (disposed) {
            return
        }

        translationPane.translation = translation
        showCard(CARD_TRANSLATION)
        invokeLater { translationPanel.verticalScrollBar.apply { value = 0 } }

        if (request.translatorId == YoudaoTranslator.id &&
            request.targetLang != Lang.AUTO &&
            request.targetLang != translation.targetLang
        ) {
        }

        setLanguageComponentsEnable(true)
    }

    override fun showError(request: Presenter.Request, errorMessage: String, throwable: Throwable) {
        if (disposed) {
            return
        }

        lastError = throwable
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

                append("<html><body><b>")
                append(value)
                append("</b>")

                val src = sourceLangComboBox.selected
                val target = targetLangComboBox.selected
                if (src != null && target != null) {
                    presenter.getCache(value, src, target)?.let {
                        append("  -  <i><small>")
                        append(it.translation)
                        append("</small></i>")
                    }
                }

                builder.append("</body></html>")
                toString()
            }
            setText(text)
        }
    }

    private inner class ResizableListener : MouseAdapter() {

        private var resizeFlag = 0
        private var startX = 0
        private var startY = 0
        private val startLocation = Point()
        private val startSize = Dimension()

        private val MouseEvent.resizeFlag: Int
            get() {
                var flag = 0
                val component = source as JComponent
                if (x in 0..RESIZE_TOUCH_SIZE) {
                    flag = flag or RESIZE_FLAG_LEFT
                }
                if (x >= component.width - RESIZE_TOUCH_SIZE && x <= component.width) {
                    flag = flag or RESIZE_FLAG_RIGHT
                }
                if (y >= component.height - RESIZE_TOUCH_SIZE && y <= component.height) {
                    flag = flag or RESIZE_FLAG_BOTTOM
                }

                return flag
            }

        private fun JComponent.updateCursor(flag: Int) {
            val cursor = when {
                flag == RESIZE_FLAG_LEFT -> Cursor.W_RESIZE_CURSOR
                flag == RESIZE_FLAG_RIGHT -> Cursor.E_RESIZE_CURSOR
                flag == RESIZE_FLAG_BOTTOM -> Cursor.S_RESIZE_CURSOR
                flag and RESIZE_FLAG_LEFT != 0 && flag and RESIZE_FLAG_BOTTOM != 0 -> Cursor.SW_RESIZE_CURSOR
                flag and RESIZE_FLAG_RIGHT != 0 && flag and RESIZE_FLAG_BOTTOM != 0 -> Cursor.SE_RESIZE_CURSOR
                else -> Cursor.DEFAULT_CURSOR
            }

            this.cursor = Cursor.getPredefinedCursor(cursor)
        }

        override fun mouseMoved(e: MouseEvent) {
            if (resizeFlag == 0) {
                (e.source as JComponent).updateCursor(e.resizeFlag)
            }
        }

        override fun mousePressed(e: MouseEvent) {
            resizeFlag = if (e.button == MouseEvent.BUTTON1) e.resizeFlag else 0
            if (resizeFlag != 0) {
                startX = e.xOnScreen
                startY = e.yOnScreen
                startLocation.location = window.location
                startSize.size = window.size
            }

            (e.source as JComponent).updateCursor(resizeFlag)
        }

        override fun mouseReleased(e: MouseEvent) {
            if (e.button == MouseEvent.BUTTON1) {
                resizeFlag = 0
                (e.source as JComponent).cursor = Cursor.getDefaultCursor()
            }
        }

        override fun mouseDragged(e: MouseEvent) {
            if (resizeFlag == 0) {
                return
            }

            var x = startLocation.x
            var w = startSize.width
            var h = startSize.height

            val dx = e.xOnScreen - startX
            val dy = e.yOnScreen - startY

            if (resizeFlag and RESIZE_FLAG_LEFT != 0) {
                w = maxOf(WIDTH, w - dx)
                x = x - w + startSize.width
            } else if (resizeFlag and RESIZE_FLAG_RIGHT != 0) {
                w = maxOf(WIDTH, w + dx)
            }
            if (resizeFlag and RESIZE_FLAG_BOTTOM != 0) {
                h = maxOf(HEIGHT, h + dy)
            }

            window.setBounds(x, startLocation.y, w, h)
            window.revalidate()
        }
    }

    companion object {
        private const val WIDTH = 400
        private const val HEIGHT = 500

        private const val RESIZE_TOUCH_SIZE = 3
        private const val RESIZE_FLAG_LEFT = 0b001
        private const val RESIZE_FLAG_RIGHT = 0b010
        private const val RESIZE_FLAG_BOTTOM = 0b100

        private val CONTENT_BACKGROUND
            get() = JBColor(Color.WHITE, UI.getColor("Editor.background", Color(0x2B2B2B))!!)
        private val DEFAULT_BORDER_COLOR = JBColor(0x808080, 0x232323)
        private val BORDER get() = LineBorder(UI.getBordersColor(DEFAULT_BORDER_COLOR))

        private const val CARD_MASSAGE = "message"
        private const val CARD_PROCESSING = "processing"
        private const val CARD_TRANSLATION = "translation"
    }
}
