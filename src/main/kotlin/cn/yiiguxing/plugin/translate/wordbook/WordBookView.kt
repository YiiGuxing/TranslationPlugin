package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.ui.wordbook.WordBookPanel
import cn.yiiguxing.plugin.translate.ui.wordbook.WordDetailsDialog
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.Popups
import cn.yiiguxing.plugin.translate.util.WordBookService
import cn.yiiguxing.plugin.translate.util.assertIsDispatchThread
import cn.yiiguxing.plugin.translate.util.executeOnPooledThread
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.tools.SimpleActionGroup
import com.intellij.ui.content.ContentManagerAdapter
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.util.ui.JBUI
import icons.Icons
import java.awt.datatransfer.StringSelection
import javax.swing.Icon

/**
 * Word book view.
 */
class WordBookView {

    private var isInitialized: Boolean = false

    private val words: MutableList<WordBookItem> = ArrayList()
    private var groupedWords: Map<String, MutableList<WordBookItem>> = HashMap()

    private val windows: MutableMap<Project, ToolWindow> = HashMap()
    private val wordBookPanels: MutableMap<Project, WordBookPanel> = HashMap()

    fun setup(project: Project, toolWindow: ToolWindow) {
        assertIsDispatchThread()

        windows[project] = toolWindow

        val contentManager = toolWindow.contentManager
        if (!Application.isUnitTestMode) {
            (toolWindow as ToolWindowEx).apply {
                val gearActions = SimpleActionGroup().apply {
                    add(ImportAction())
                    add(ExportActionGroup())
                }
                setAdditionalGearActions(gearActions)
                setTitleActions(RefreshAction(), ShowWordOfTheDayAction())
            }
        }

        val panel = getWordBookPanel(project)
        val content = contentManager.factory.createContent(panel, null, false)
        content.tabName = TAB_NAME_ALL
        contentManager.addContent(content)
        contentManager.addContentManagerListener(object : ContentManagerAdapter() {
            override fun selectionChanged(event: ContentManagerEvent) {
                val words = if (event.content.tabName == TAB_NAME_ALL) {
                    words
                } else {
                    groupedWords[event.content.displayName]
                }
                wordBookPanels[project]?.apply {
                    setWords(words ?: emptyList())
                    fireWordsChanged()
                }
            }
        })
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

    private fun getWordBookPanel(project: Project): WordBookPanel {
        return wordBookPanels.getOrPut(project) {
            WordBookPanel().apply {
                setupMenu(project)
                onWordDoubleClicked { word -> openWordDetails(project, word) }
                onDownloadDriver {
                    if (!WordBookService.downloadDriver()) {
                        val message = message("wordbook.window.message.in.download")
                        Popups.showBalloonForComponent(it, message, MessageType.INFO, project, JBUI.scale(10))
                    }
                }
            }
        }
    }

    private fun WordBookPanel.setupMenu(project: Project) {
        val panel = this@setupMenu
        popupMenu = JBPopupMenu()
            .addMenuItem(panel, message("wordbook.window.menu.detail"), Icons.Detail) { openWordDetails(project, it) }
            .addMenuItem(panel, message("wordbook.window.menu.copy"), AllIcons.Actions.Copy) { word ->
                CopyPasteManager.getInstance().setContents(StringSelection(word.word))
            }
            .addMenuItem(panel, message("wordbook.window.menu.delete"), AllIcons.Actions.Cancel) { word ->
                val id = word.id
                if (id != null) {
                    val confirmed = Messages.showOkCancelDialog(
                        message("wordbook.window.confirmation.delete.message", word.word),
                        message("wordbook.window.confirmation.delete.title"),
                        Messages.OK_BUTTON,
                        Messages.CANCEL_BUTTON,
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
                        notifyWordsChanged()
                        selectWord(wordBookItem)
                    }

                    override fun onWordUpdated(service: WordBookService, wordBookItem: WordBookItem) {
                        assertIsDispatchThread()
                        val index = words.indexOfFirst { it.id == wordBookItem.id }
                        if (index >= 0) {
                            words[index] = wordBookItem
                            notifyWordsChanged()
                        }
                    }

                    override fun onWordRemoved(service: WordBookService, id: Long) {
                        assertIsDispatchThread()
                        val index = words.indexOfFirst { it.id == id }
                        if (index >= 0) {
                            words.removeAt(index)
                            notifyWordsChanged()
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

    private fun notifyWordsChanged() {
        updateGroupedWords()
        for ((project, toolWindow) in windows) {
            updateContent(project, toolWindow)
        }
    }

    private fun updateGroupedWords() {
        val newGroupedWords = HashMap<String, MutableList<WordBookItem>>()
        for (word in words) {
            for (tag in word.tags) {
                if (tag.isNotEmpty()) {
                    newGroupedWords.getOrPut(tag) { ArrayList() } += word
                }
            }
        }

        groupedWords = newGroupedWords.toSortedMap()
    }

    private fun selectWord(wordBookItem: WordBookItem) {
        for ((_, toolWindow) in windows) {
            val contentManager = toolWindow.contentManager
            val allContent = contentManager.contents.find { it.tabName == TAB_NAME_ALL } ?: continue
            if (contentManager.selectedContent != allContent) {
                contentManager.setSelectedContent(allContent)
            }
            (allContent.component as? WordBookPanel)?.selectWord(wordBookItem)
        }
    }

    private fun updateContent(project: Project, toolWindow: ToolWindow) {
        val groupedWords = groupedWords
        val contentManager = toolWindow.contentManager
        val allContent = contentManager.getContent(0)!!
        val groupedContents = contentManager.contents.let { contents ->
            if (contents.size > 1) contents.copyOfRange(1, contents.size) else emptyArray()
        }
        var selectedContent = contentManager.selectedContent

        if (groupedWords.isEmpty()) {
            allContent.displayName = null
            allContent.tabName = TAB_NAME_ALL
            for (content in groupedContents) {
                contentManager.removeContent(content, true)
            }
            contentManager.setSelectedContent(allContent)
            selectedContent = allContent
        } else {
            allContent.displayName = message("wordbook.window.ui.tab.title.all")
            allContent.tabName = TAB_NAME_ALL

            val keys = groupedWords.keys
            val livingContents = ArrayList<String>()
            for (content in groupedContents) {
                val isDead = content.displayName !in keys
                if (isDead) {
                    contentManager.removeContent(content, true)
                    if (selectedContent === content) {
                        selectedContent = null
                    }
                } else {
                    livingContents += content.displayName
                }
            }

            val factory = contentManager.factory
            for (name in groupedWords.keys) {
                val index = livingContents.binarySearch(name)
                if (index < 0) {
                    val insertIndex = -index - 1
                    livingContents.add(insertIndex, name)
                    val content = factory.createContent(getWordBookPanel(project), name, false)
                    contentManager.addContent(content, insertIndex + 1)
                }
            }

            selectedContent = selectedContent ?: allContent
            contentManager.setSelectedContent(selectedContent)
        }

        val wordsToDisplay = if (selectedContent === allContent) {
            words
        } else {
            groupedWords[selectedContent.displayName] ?: words
        }
        getWordBookPanel(project).apply {
            setWords(wordsToDisplay)
            fireWordsChanged()
        }
    }

    private fun openWordDetails(project: Project?, word: WordBookItem) {
        WordDetailsDialog(project, word, groupedWords.keys).show()
    }

    private abstract inner class WordBookAction(text: String, description: String? = null, icon: Icon? = null) :
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
                    TranslationUIManager.showWordOfTheDayDialog(project, words.sortedBy { Math.random() })
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

    private inner class ImportAction : WordBookAction(message("wordbook.window.action.import")) {
        override fun doAction(e: AnActionEvent) = importWordBook(e.project)
    }

    private inner class ExportAction(private val exporter: WordBookExporter) : WordBookAction(exporter.name) {
        override fun doAction(e: AnActionEvent) = exporter.export(e.project, words)
    }

    private inner class ExportActionGroup : ActionGroup(message("wordbook.window.action.export"), true), DumbAware {
        private val actions: Array<AnAction> = WORD_BOOK_EXPORTERS.map { ExportAction(it) }.toTypedArray()

        override fun getChildren(e: AnActionEvent?): Array<AnAction> = actions
    }

    companion object {
        private const val TAB_NAME_ALL = "ALL"

        val instance: WordBookView
            get() = ServiceManager.getService(WordBookView::class.java)
    }
}