package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.WordBookPanel
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.Popups
import cn.yiiguxing.plugin.translate.util.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.WordBookService
import cn.yiiguxing.plugin.translate.util.assertIsDispatchThread
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowEx

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
            WordBookPanel().apply { setWords(this@WordBookView.words) }
        }

        val content = contentManager.factory.createContent(panel, null, false)
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)

        refresh()
        panel.showTable()

        Disposer.register(project, Disposable {
            windows.remove(project)
            wordBookPanels.remove(project)
        })

        subscribeWordBookTopic()
        isInitialized = true
    }

    private fun subscribeWordBookTopic() {
        if (!isInitialized) {
            Application.messageBus
                .connect()
                .subscribe(WordBookChangeListener.TOPIC, object : WordBookChangeListener {
                    override fun onWordAdded(service: WordBookService, wordBookItem: WordBookItem) {
                        words.add(wordBookItem)
                        notifyWordsChanged()
                    }

                    override fun onWordRemoved(service: WordBookService, id: Long) {
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

    private fun notifyWordsChanged() {
        for ((_, panel) in wordBookPanels) {
            panel.update()
        }
    }

    private inner class RefreshAction : DumbAwareAction(
        message("wordbook.window.action.refresh"),
        message("wordbook.window.action.refresh.desc"),
        AllIcons.Actions.Refresh
    ) {
        override fun actionPerformed(e: AnActionEvent) = refresh()
    }

    private inner class ShowWordOfTheDayAction : DumbAwareAction(
        message("word.of.the.day.title"), null, AllIcons.Actions.IntentionBulb
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project
            if (words.isNotEmpty()) {
                windows[project]?.hide {
                    TranslationUIManager.showWordDialog(project, words.toList())
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