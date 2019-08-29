package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.assertIsDispatchThread
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowEx
import javax.swing.Icon
import javax.swing.JPanel

/**
 * Word book view.
 *
 * Created by Yii.Guxing on 2019/08/28.
 */
class WordBookView {

    private var isInitialized: Boolean = false

    fun setup(toolWindow: ToolWindow) {
        assertIsDispatchThread()

        val contentManager = toolWindow.contentManager
        if (!Application.isUnitTestMode) {
            (toolWindow as ToolWindowEx).setTitleActions(RefreshAction(), ShowWordOfTheDayAction())
        }

        val panel = SimpleToolWindowPanel(true, true)
        panel.setContent(JPanel())

        val content = contentManager.factory.createContent(panel, null, false)
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)

        subscribeWordBookTopic()
        isInitialized = true
    }

    private fun subscribeWordBookTopic() {
        if (!isInitialized) {
            Application.messageBus
                .connect()
                .subscribe(WordBookChangeListener.TOPIC, object : WordBookChangeListener {
                    override fun onWordAdded(service: WordBookService, wordBookItem: WordBookItem) {
                    }

                    override fun onWordRemoved(service: WordBookService, id: Long) {
                    }
                })
        }
    }

    private abstract class WordBookAction(text: String?, description: String?, icon: Icon?) :
        DumbAwareAction(text, description, icon) {

        init {
        }

        override fun update(e: AnActionEvent) {
        }
    }

    private inner class RefreshAction : WordBookAction(
        message("wordbook.window.action.refresh"),
        message("wordbook.window.action.refresh.desc"),
        AllIcons.Actions.Refresh
    ) {
        override fun actionPerformed(e: AnActionEvent) {

        }
    }

    private inner class ShowWordOfTheDayAction : WordBookAction(
        message("word.of.the.day.title"), null, AllIcons.Actions.IntentionBulb
    ) {
        override fun actionPerformed(e: AnActionEvent) {

        }
    }

    companion object {
        val instance: WordBookView
            get() = ServiceManager.getService(WordBookView::class.java)
    }
}