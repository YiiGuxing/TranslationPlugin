package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.WordBookPanel
import cn.yiiguxing.plugin.translate.ui.WordDetailsDialog
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.util.WordBookService
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.util.ui.JBUI
import icons.Icons
import java.awt.datatransfer.StringSelection
import javax.swing.Icon

/**
 * Word book view.
 *
 * Created by Yii.Guxing on 2019/08/28.
 */
class WordBookView {

    private var isInitialized: Boolean = false

    private val words: MutableList<WordBookItem> = ArrayList()

    private val windows: MutableMap<Project, ToolWindow> = HashMap()
    private val wordBookPanels: MutableMap<Project, WordBookPanel> = HashMap()

    fun setup(project: Project, toolWindow: ToolWindow) {
        assertIsDispatchThread()

        windows[project] = toolWindow

        val contentManager = toolWindow.contentManager
        if (!Application.isUnitTestMode) {
            (toolWindow as ToolWindowEx).setTitleActions(RefreshAction(), ShowWordOfTheDayAction())
        }

        val panel = wordBookPanels.getOrPut(project) {
            WordBookPanel().apply {
                setupMenu()
                setWords(this@WordBookView.words)
                onWordDoubleClicked { word -> openWordDetails(word) }
                onDownloadDriver {
                    if (!WordBookService.downloadDriver()) {
                        val message = message("wordbook.window.message.in.download")
                        Popups.showBalloonForComponent(it, message, MessageType.INFO, project, JBUI.scale(10))
                    }
                }
            }
        }

        val content = contentManager.factory.createContent(panel, null, false)
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)

        if (WordBookService.isInitialized) {
            refresh()
            panel.showTable()
        } else {
            panel.showMessagePane()
        }

        Disposer.register(project, Disposable {
            windows.remove(project)
            wordBookPanels.remove(project)
        })

        subscribeWordBookTopic()
        isInitialized = true
    }

    private fun WordBookPanel.setupMenu() {
        val panel = this@setupMenu
        popupMenu = JBPopupMenu()
            .addMenuItem(panel, message("wordbook.window.menu.detail"), Icons.Detail) { openWordDetails(it) }
            .addMenuItem(panel, message("wordbook.window.menu.copy"), AllIcons.Actions.Copy) { word ->
                CopyPasteManager.getInstance().setContents(StringSelection(word.word))
            }
            .addMenuItem(panel, message("wordbook.window.menu.delete"), AllIcons.Actions.Delete) { word ->
                val id = word.id
                if (id != null) {
                    val confirmed = Messages.showOkCancelDialog(
                        message("wordbook.window.confirmation.delete.message", word.word),
                        message("wordbook.window.confirmation.delete.title"),
                        null
                    ) == Messages.OK
                    if (confirmed) {
                        executeOnPooledThread { WordBookService.removeWord(id) }
                    }
                }
            }
    }

    private inline fun JBPopupMenu.addMenuItem(
        panel: WordBookPanel,
        text: String,
        icon: Icon,
        crossinline action: (WordBookItem) -> Unit
    ): JBPopupMenu {
        val menuItem = JBMenuItem(text, icon)
        menuItem.addActionListener { panel.selectedWord?.let { action(it) } }
        add(menuItem)

        return this
    }

    private fun subscribeWordBookTopic() {
        if (!isInitialized) {
            Application.messageBus
                .connect()
                .subscribe(WordBookListener.TOPIC, object : WordBookListener {
                    override fun onInitialized(service: WordBookService) {
                        assertIsDispatchThread()
                        showWordBookTable()
                    }

                    override fun onWordAdded(service: WordBookService, wordBookItem: WordBookItem) {
                        assertIsDispatchThread()
                        words.add(wordBookItem)
                        notifyWordsChanged(words.lastIndex, WordBookPanel.ChangeType.INSERT)
                    }

                    override fun onWordUpdated(service: WordBookService, wordBookItem: WordBookItem) {
                        assertIsDispatchThread()
                        val index = words.indexOfFirst { it.id == wordBookItem.id }
                        if (index >= 0) {
                            words[index] = wordBookItem
                            notifyWordsChanged(index, WordBookPanel.ChangeType.UPDATE)
                        }
                    }

                    override fun onWordRemoved(service: WordBookService, id: Long) {
                        assertIsDispatchThread()
                        val index = words.indexOfFirst { it.id == id }
                        if (index >= 0) {
                            words.removeAt(index)
                            notifyWordsChanged(index, WordBookPanel.ChangeType.DELETE)
                        }
                    }
                })
        }
    }

    private fun refresh() {
        val newWords = WordBookService.getWords()
        words.clear()
        words.addAll(newWords)
        notifyWordsChanged()
    }

    private fun showWordBookTable() {
        refresh()
        for ((_, panel) in wordBookPanels) {
            panel.showTable()
        }
    }

    private fun notifyWordsChanged(
        row: Int = WordBookPanel.ALL_ROWS,
        type: WordBookPanel.ChangeType = WordBookPanel.ChangeType.UPDATE
    ) {
        for ((_, panel) in wordBookPanels) {
            panel.fireWordsChanged(row, type)
        }
    }

    private fun openWordDetails(word: WordBookItem) {
        WordDetailsDialog(word).show()
    }

    private abstract inner class WordBookAction(text: String, description: String?, icon: Icon) :
        DumbAwareAction(text, description, icon) {

        final override fun actionPerformed(e: AnActionEvent) {
            if (WordBookService.isInitialized) {
                doAction(e)
            } else {
                Popups.showBalloonForComponent(
                    e.inputEvent.component,
                    message("wordbook.window.message.missing.driver"),
                    MessageType.INFO,
                    e.project,
                    offsetY = 1
                )
            }
        }

        protected abstract fun doAction(e: AnActionEvent)

    }

    private inner class RefreshAction : WordBookAction(
        message("wordbook.window.action.refresh"),
        message("wordbook.window.action.refresh.desc"),
        AllIcons.Actions.Refresh
    ) {
        override fun doAction(e: AnActionEvent) = refresh()
    }

    private inner class ShowWordOfTheDayAction : WordBookAction(
        message("word.of.the.day.title"), null, AllIcons.Actions.IntentionBulb
    ) {
        override fun doAction(e: AnActionEvent) {
            val project = e.project
            if (words.isNotEmpty()) {
                windows[project]?.hide {
                    TranslationUIManager.showWordOfTheDayDialog(project, words.toList())
                }
            } else {
                Popups.showBalloonForComponent(
                    e.inputEvent.component,
                    message("wordbook.window.message.empty"),
                    MessageType.INFO,
                    project,
                    offsetY = 1
                )
            }
        }
    }

    companion object {
        val instance: WordBookView
            get() = ServiceManager.getService(WordBookView::class.java)
    }
}