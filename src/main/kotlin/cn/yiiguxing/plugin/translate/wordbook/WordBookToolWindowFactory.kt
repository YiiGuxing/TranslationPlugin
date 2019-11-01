package cn.yiiguxing.plugin.translate.wordbook

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

/**
 * Word book tool window factory
 */
class WordBookToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        WordBookView.instance.setup(project, toolWindow)
    }

    companion object{
        const val TOOL_WINDOW_ID = "Word Book"
    }

}