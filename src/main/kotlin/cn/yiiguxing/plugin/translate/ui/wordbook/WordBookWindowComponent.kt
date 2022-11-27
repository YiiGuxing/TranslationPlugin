package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.LoadingPanel
import cn.yiiguxing.plugin.translate.util.Observable
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import cn.yiiguxing.plugin.translate.wordbook.WordBookState
import cn.yiiguxing.plugin.translate.wordbook.WordBookState.*
import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.MultiPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.Disposer
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent

class WordBookWindowComponent(private val parentDisposable: Disposable) :
    LoadingPanel(BorderLayout(), { parent -> WordBookWindowLoadingDecorator(parent, parentDisposable) }) {

    private val emptyPanel = JPanel()

    private val tableView = WordBookTableView()
    private val dataContentPanel = JBScrollPane(tableView)

    private val downloadingIcon = AsyncProcessIcon("")
    private val downloadLinkLabel =
        LinkLabel<Any>(message("wordbook.window.message.download"), AllIcons.General.Warning)
    private val downloadPanel = JPanel()

    private val retryButton = JButton(message("retry.action.name"))
    private val errorPanel = JPanel()

    private val multiPanel: MultiPanel = object : MultiPanel() {
        override fun create(key: Int): JComponent {
            return when (key) {
                DATA_CONTENT -> dataContentPanel
                EMPTY_PANEL -> emptyPanel
                DOWNLOAD_PANEL -> downloadPanel
                ERROR_PANEL -> errorPanel
                else -> super.create(key)
            }
        }
    }


    private var onViewWordDetailHandler: ((WordBookItem) -> Unit)? = null
    private var onDeleteWordsHandler: ((List<WordBookItem>) -> Unit)? = null


    init {
        initWordBookTableView()
        initDownloadPanel()
        initErrorPanel()

        multiPanel.select(EMPTY_PANEL, true)
        add(multiPanel)
    }

    private fun initWordBookTableView() {
        val table = tableView
        table.onWordDoubleClick { word -> onViewWordDetailHandler?.invoke(word) }
        table.popupMenu = JBPopupMenu().also { menu ->
            val detailItem = createMenuItem(message("wordbook.window.menu.detail"), AllIcons.Actions.Highlighting) {
                table.selectedWord?.let { word -> onViewWordDetailHandler?.invoke(word) }
            }
            val copyItem = createMenuItem(message("wordbook.window.menu.copy"), AllIcons.Actions.Copy) {
                table.selectedWord?.let { word ->
                    CopyPasteManager.getInstance().setContents(StringSelection(word.word))
                }
            }
            val deleteItem = createMenuItem(message("wordbook.window.menu.delete"), AllIcons.Actions.GC) {
                performWordsDelete()
            }
            menu.add(deleteItem)

            menu.addPopupMenuListener(object : PopupMenuListenerAdapter() {
                override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                    if (!table.isMultipleSelection) {
                        menu.add(detailItem, 0)
                        menu.add(copyItem, 1)
                    } else {
                        menu.remove(detailItem)
                        menu.remove(copyItem)
                    }
                }
            })
        }
        table.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                if (event.keyCode == KeyEvent.VK_DELETE) {
                    performWordsDelete()
                    event.consume()
                }
            }
        })
    }

    private inline fun createMenuItem(text: String, icon: Icon, crossinline action: () -> Unit): JBMenuItem {
        return JBMenuItem(text, icon).apply {
            addActionListener { action() }
        }
    }

    private fun performWordsDelete() {
        tableView.selectedWords.takeIf { it.isNotEmpty() }?.let {
            onDeleteWordsHandler?.invoke(it)
        }
    }

    private fun initDownloadPanel() {
        downloadingIcon.isVisible = false
        Disposer.register(parentDisposable, downloadingIcon)

        downloadLinkLabel.iconTextGap = JBUIScale.scale(5)

        downloadPanel.apply {
            layout = HorizontalLayout(JBUIScale.scale(5), SwingConstants.CENTER)
            add(downloadingIcon, HorizontalLayout.CENTER)
            add(downloadLinkLabel, HorizontalLayout.CENTER)
            add(JBLabel(message("wordbook.window.message.driver.files")), HorizontalLayout.CENTER)
        }
    }

    private fun initErrorPanel() {
        errorPanel.apply {
            layout = VerticalLayout(JBUIScale.scale(5), SwingConstants.CENTER)

            val label = JBLabel(message("wordbook.window.message.initialization.error")).apply {
                icon = AllIcons.General.Warning
                border = JBUI.Borders.emptyRight(4)
            }
            add(label, VerticalLayout.CENTER)
            add(retryButton, VerticalLayout.CENTER)
        }
    }

    fun onRetryInitialization(handler: () -> Unit) {
        retryButton.action = object : AbstractAction(message("retry.action.name")) {
            override fun actionPerformed(e: ActionEvent) = handler()
        }
    }

    fun onDownloadDriver(handler: (JComponent) -> Unit) {
        downloadLinkLabel.setListener({ label, _ -> handler(label) }, null)
    }

    fun onViewWordDetail(handler: (WordBookItem) -> Unit) {
        onViewWordDetailHandler = handler
    }

    fun onDeleteWords(handler: (List<WordBookItem>) -> Unit) {
        onDeleteWordsHandler = handler
    }

    fun bindState(state: Observable<WordBookState>, updateNow: Boolean = true) {
        state.observe(parentDisposable) { newValue, _ -> updateState(newValue) }
        if (updateNow) {
            updateState(state.value)
        }
    }

    private fun updateState(state: WordBookState) {
        val key = when (state) {
            UNINITIALIZED,
            INITIALIZING -> EMPTY_PANEL

            NO_DRIVER,
            DOWNLOADING_DRIVER -> DOWNLOAD_PANEL

            INITIALIZATION_ERROR -> ERROR_PANEL
            RUNNING -> DATA_CONTENT
        }

        multiPanel.select(key, true)
        if (state == INITIALIZING && !isLoading) {
            setLoadingText(message("label.initializing"))
            startLoading()
        } else if (isLoading) {
            stopLoading()
        }
        if (state == DOWNLOADING_DRIVER) {
            downloadLinkLabel.icon = null
            downloadingIcon.isVisible = true
            downloadingIcon.resume()
        } else {
            downloadingIcon.suspend()
            downloadingIcon.isVisible = false
            downloadLinkLabel.icon = AllIcons.General.Warning
        }
    }

    fun setWords(words: List<WordBookItem>) {
        tableView.setWords(words)
    }

    fun bindLoading(loading: Observable<Boolean>, updateNow: Boolean = false) {
        loading.observe(parentDisposable) { newValue, _ -> updateLoading(newValue) }
        if (updateNow) {
            updateLoading(loading.value)
        }
    }

    private fun updateLoading(loading: Boolean) {
        if (loading && !isLoading) {
            setLoadingText(message("label.loading"))
            startLoading()
        } else if (isLoading) {
            stopLoading()
        }
    }

    fun selectWord(word: WordBookItem) {
        tableView.selectWord(word)
    }


    companion object {
        private const val DATA_CONTENT = 0
        private const val EMPTY_PANEL = -1
        private const val DOWNLOAD_PANEL = -2
        private const val ERROR_PANEL = -3
    }

}