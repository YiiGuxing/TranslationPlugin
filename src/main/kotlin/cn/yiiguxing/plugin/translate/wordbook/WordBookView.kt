package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.ui.Popups
import cn.yiiguxing.plugin.translate.ui.wordbook.WordBookWindowComponent
import cn.yiiguxing.plugin.translate.ui.wordbook.WordDetailsDialog
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.util.WordBookService
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.tools.SimpleActionGroup
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.JBUI
import org.jetbrains.concurrency.runAsync
import javax.swing.Icon

/**
 * Word book view.
 */
class WordBookView {

    private var isInitialized: Boolean = false

    private val observableLoading = ObservableValue(false)
    private var isLoading: Boolean by observableLoading

    private val words: MutableList<WordBookItem> = ArrayList()
    private var groupedWords: Map<String, List<WordBookItem>> = HashMap()

    val wordTags: Set<String> get() = groupedWords.keys

    private val windows: MutableMap<Project, ToolWindow> = ContainerUtil.createWeakMap()
    private val components: MutableSet<WordBookWindowComponent> = ContainerUtil.createWeakSet()

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
                setTitleActions(listOf(RefreshAction(), ShowWordOfTheDayAction()))
            }
        }

        val content = createContent(project, contentManager, TAB_NAME_ALL)
        contentManager.addContent(content)
        contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) = onWindowContentSelected(event.content)
        })
        contentManager.setSelectedContent(content)

        if (WordBookService.isInitialized) {
            refresh()
        }

        Disposer.register(TranslationUIManager.disposable(project)) {
            windows.remove(project)
        }

        subscribeWordBookTopic()
        isInitialized = true
    }

    private fun createContent(
        project: Project,
        contentManager: ContentManager,
        tabName: String? = null,
        displayName: String? = null
    ): Content {
        val disposable = Disposer.newDisposable(contentManager, "Wordbook Window Component")
        val windowComponent = createWordBookWindowComponent(project, disposable)
        val content = contentManager.factory.createContent(windowComponent, displayName, false).apply {
            this.tabName = tabName
        }
        Disposer.register(content, disposable)
        return content
    }

    private fun createWordBookWindowComponent(project: Project, disposable: Disposable): WordBookWindowComponent {
        val component = WordBookWindowComponent(disposable).apply {
            bindLoading(observableLoading.asReadOnly(), false)
            bindState(WordBookService.stateBinding)
            onDownloadDriver {
                if (!WordBookService.downloadDriverAndInitService()) {
                    val message = message("wordbook.window.message.in.download")
                    Popups.showBalloonForComponent(it, message, MessageType.INFO, project, JBUI.scale(10))
                }
            }
            onViewWordDetail { word -> openWordDetails(project, word) }
            onDeleteWords { words -> deleteWord(project, words) }
        }

        components += component
        Disposer.register(disposable) { components -= component }
        return component
    }


    private fun onWindowContentSelected(content: Content) {
        val words = if (content.tabName == TAB_NAME_ALL) {
            words
        } else {
            groupedWords[content.displayName]
        }
        content.wordBookWindowComponent.setWords(words ?: emptyList())
    }

    private fun deleteWord(project: Project, words: List<WordBookItem>) {
        if (words.isEmpty()) {
            return
        }

        val title = message("wordbook.window.confirmation.delete.title")
        val message = if (words.size == 1) {
            message("wordbook.window.confirmation.delete.message", words.joinToString { it.word })
        } else {
            message("wordbook.window.confirmation.delete.message.multiple", words.joinToString { it.word })
        }
        val result = MessageDialogBuilder.yesNo(title, message).ask(project)
        if (result) {
            executeOnPooledThread { WordBookService.removeWords(words.mapNotNull { it.id }) }
        }
    }

    private fun subscribeWordBookTopic() {
        if (!isInitialized) {
            if (!WordBookService.isInitialized) {
                WordBookService.stateBinding.observe(TranslationUIManager.disposable()) { state, _ ->
                    if (state == WordBookState.RUNNING) {
                        refresh()
                    }
                }
            }
            Application.messageBus
                .connect(TranslationUIManager.disposable())
                .subscribe(WordBookListener.TOPIC, object : WordBookListener {
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
        Application.assertIsDispatchThread()
        if (isLoading) {
            return
        }

        isLoading = true
        val modalityState = ModalityState.current()
        runAsync { WordBookService.getWords() }
            .onSuccess { newWords ->
                invokeLater(modalityState) {
                    words.clear()
                    words.addAll(newWords)
                    notifyWordsChanged()
                }
            }
            .onProcessed {
                invokeLater(modalityState) { isLoading = false }
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
            allContent.wordBookWindowComponent.selectWord(wordBookItem)
        }
    }

    private fun updateContent(project: Project, toolWindow: ToolWindow) {
        if (project.isDisposed) {
            return
        }

        val groupedWords = groupedWords
        val contentManager = toolWindow.contentManager
        if (contentManager.isDisposed) {
            return
        }

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
            selectedContent = allContent
        } else {
            allContent.displayName = message("wordbook.window.ui.tab.title.all")
            allContent.tabName = TAB_NAME_ALL

            val keys = groupedWords.keys
            val livingContents = ArrayList<String>(keys.size)
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

            // Using binary search should require sorting first,
            // but `livingContents` don't need to be sorted anymore,
            // because it was initially empty, after inserting a new
            // element by binary search, it will be an ordered list.
            for (name in groupedWords.keys) {
                val index = livingContents.binarySearch(name)
                if (index < 0) {
                    val insertIndex = -index - 1
                    livingContents.add(insertIndex, name)
                    val content = createContent(project, contentManager, name, name)
                    contentManager.addContent(content, insertIndex + 1)
                }
            }
        }

        (selectedContent ?: allContent).let { selection ->
            val wordsToDisplay = if (selection === allContent) {
                words
            } else {
                groupedWords[selection.displayName] ?: words
            }

            selection.wordBookWindowComponent.setWords(wordsToDisplay)
            contentManager.setSelectedContent(selection)
        }
    }

    private fun openWordDetails(project: Project?, word: WordBookItem) {
        WordDetailsDialog(project, word).show()
    }

    private abstract inner class WordBookAction(text: String, description: String? = text, icon: Icon? = null) :
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
                for ((p, w) in windows) if (p != project) {
                    w.hide()
                }

                val shuffledWords = words.shuffled()
                val toolWindow = windows[project]
                if (toolWindow != null) {
                    toolWindow.hide { TranslationUIManager.showWordOfTheDayDialog(project, shuffledWords) }
                } else {
                    TranslationUIManager.showWordOfTheDayDialog(project, shuffledWords)
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
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = WordBookService.isInitialized
        }

        override fun doAction(e: AnActionEvent) = importWordBook(e.project)
    }

    private inner class ExportAction(private val exporter: WordBookExporter) :
        WordBookAction("${exporter.name}${if (exporter.availableForImport) message("wordbook.window.export.tip") else ""}") {
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = WordBookService.isInitialized
        }

        override fun doAction(e: AnActionEvent) = exporter.export(e.project, words)
    }

    private inner class ExportActionGroup : ActionGroup(message("wordbook.window.action.export"), true), DumbAware {
        private val actions: Array<AnAction> = WORD_BOOK_EXPORTERS.map { ExportAction(it) }.toTypedArray()

        override fun getChildren(e: AnActionEvent?): Array<AnAction> = actions
    }

    companion object {
        private const val TAB_NAME_ALL = "ALL"

        val instance: WordBookView
            get() = ApplicationManager.getApplication().getService(WordBookView::class.java)

        private val Content.wordBookWindowComponent: WordBookWindowComponent
            get() = component as WordBookWindowComponent
    }
}