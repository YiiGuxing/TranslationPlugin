package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.message
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import javax.swing.Icon
import javax.swing.JPanel

/**
 * Word book tool window factory
 *
 * Created by Yii.Guxing on 2019/08/26.
 */
class WordBookToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        val panel = SimpleToolWindowPanel(true, true)
        val toolbar = createToolbar()
        panel.setToolbar(toolbar.component)
        panel.setContent(JPanel())

        val content = contentManager.factory.createContent(panel, null, false)
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)
    }

    private fun createToolbar(): ActionToolbar {
        val group = DefaultActionGroup().apply {
            add(RefreshAction())
            add(ShowWordOfTheDayAction())
            addSeparator()
            add(DeleteAction())
        }
        return ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true)
    }

    private abstract class WordBookAction(text: String?, description: String?, icon: Icon?) :
        DumbAwareAction(text, description, icon) {

        override fun update(e: AnActionEvent) {
        }

        open fun isEnabled(): Boolean = true

    }

    private class RefreshAction : WordBookAction(
        message("wordbook.window.action.refresh"),
        message("wordbook.window.action.refresh.desc"),
        AllIcons.Actions.Refresh
    ) {
        override fun actionPerformed(e: AnActionEvent) {

        }
    }

    private class ShowWordOfTheDayAction : WordBookAction(
        message("word.of.the.day.title"),
        null,
        AllIcons.Actions.IntentionBulb
    ) {
        override fun actionPerformed(e: AnActionEvent) {

        }
    }

    private class DeleteAction : WordBookAction(
        message("wordbook.window.action.del"),
        message("wordbook.window.action.del.desc"),
        AllIcons.Actions.GC
    ) {
        override fun isEnabled(): Boolean = false

        override fun actionPerformed(e: AnActionEvent) {

        }
    }

}