package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.ObservableValue
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import cn.yiiguxing.plugin.translate.wordbook.WordBookState
import cn.yiiguxing.plugin.translate.wordbook.WordBookState.*
import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.MultiPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class WordBookWindowComponent(/*project: Project*/) : SimpleToolWindowPanel(true, true), Disposable {

    private val loadingPanel = JBLoadingPanel(BorderLayout(), this)
    private val emptyPanel = JPanel()

    private val tableView = WordBookTableView()
    private val dataContentPanel = JBScrollPane(tableView)

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


    init {
        initDownloadPanel()
        initErrorPanel()

        multiPanel.select(EMPTY_PANEL, true)
        loadingPanel.add(multiPanel)
        setContent(loadingPanel)
        loadingPanel.startLoading()

        //Disposer.register(TranslationUIManager.disposable(project), this)
    }

    private fun initDownloadPanel() {
        downloadPanel.apply {
            layout = HorizontalLayout(JBUIScale.scale(5))
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

    fun setDownloadDriverHandler(handler: (JComponent) -> Unit) {
        downloadLinkLabel.setListener({ label, _ -> handler(label) }, null)
    }

    fun bindState(state: ObservableValue<WordBookState>) {
        state.observe(this) { newValue, _ -> updateState(newValue) }
        updateState(state.value)
    }

    private fun updateState(state: WordBookState) {
        val key = when (state) {
            UNINITIALIZED,
            INITIALIZING,
            DOWNLOADING_DRIVER -> EMPTY_PANEL
            NO_DRIVER -> DOWNLOAD_PANEL
            INITIALIZATION_ERROR -> ERROR_PANEL
            RUNNING -> DATA_CONTENT
        }

        multiPanel.select(key, true)
        when (state) {
            INITIALIZING, DOWNLOADING_DRIVER -> if (!loadingPanel.isLoading) {
                loadingPanel.startLoading()
            }
            else -> if (loadingPanel.isLoading) {
                loadingPanel.stopLoading()
            }
        }
    }

    fun setWords(words: List<WordBookItem>) {
        tableView.setWords(words)
    }

    override fun dispose() {
    }

    companion object {
        private const val DATA_CONTENT = 0
        private const val EMPTY_PANEL = -1
        private const val DOWNLOAD_PANEL = -2
        private const val ERROR_PANEL = -3
    }

}