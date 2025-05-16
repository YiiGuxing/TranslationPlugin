package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.util.checkDispatchThread
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager

/** The id of the wordbook tool window. */
const val WORDBOOK_TOOL_WINDOW_ID = "Translation.Wordbook"

internal interface WordBookToolWindowFactory : ToolWindowFactory, DumbAware {
    companion object {
        /**
         * Show the word book tool window.
         */
        fun requireWordBook(project: Project) {
            checkDispatchThread { "Must only be invoked from the Event Dispatch Thread." }
            ToolWindowManager.getInstance(project).getToolWindow(WORDBOOK_TOOL_WINDOW_ID)?.show()
        }
    }
}

/**
 * Word book tool window factory
 */
internal class WordBookToolWindowFactoryImpl : WordBookToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        WordBookView.getInstance().setup(project, toolWindow)
    }

    override fun init(toolWindow: ToolWindow) {
        adaptedMessage("wordbook.window.title").let { title ->
            toolWindow.title = title
            toolWindow.stripeTitle = title
        }
    }
}