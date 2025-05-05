package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.checkDispatchThread
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener

/** The id of the wordbook tool window. */
const val WORDBOOK_TOOL_WINDOW_ID = "Translation.Wordbook"

internal interface WordBookToolWindowFactory : ToolWindowFactory, DumbAware {
    companion object {
        /**
         * Show the word book tool window stripe button.
         */
        fun requireWordBook() {
            checkDispatchThread { "Must only be invoked from the Event Dispatch Thread." }
            Application.messageBus.syncPublisher(RequireWordBookListener.TOPIC).onRequire()
        }
    }
}

/**
 * Word book tool window factory
 */
internal class WordBookToolWindowFactoryImpl : WordBookToolWindowFactory, ToolWindowManagerListener {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Try to fix: https://github.com/YiiGuxing/TranslationPlugin/issues/1186
        if (project.isDisposed) {
            return
        }
        WordBookView.getInstance().setup(project, toolWindow)
        project.messageBus.connect(toolWindow.contentManager).subscribe(ToolWindowManagerListener.TOPIC, this)
    }

    override fun init(toolWindow: ToolWindow) {
        adaptedMessage("wordbook.window.title").let { title ->
            toolWindow.title = title
            toolWindow.stripeTitle = title
        }

        val project = toolWindow.project
        val toolWindowRef = DisposableRef.create(toolWindow.disposable, toolWindow)
        val messageBusConnection = project.messageBus.connect(toolWindow.disposable)
        messageBusConnection.subscribe(RequireWordBookListener.TOPIC, object : RequireWordBookListener {
            override fun onRequire() {
                toolWindowRef.get()?.runIfSurvive {
                    isAvailable = true
                }
            }
        })
    }

    private inline fun ToolWindow.runIfSurvive(crossinline action: ToolWindow.() -> Unit) {
        if (isDisposed || project.isDisposed) {
            return
        }
        ToolWindowManager.getInstance(project).invokeLater {
            if (!isDisposed) {
                action()
            }
        }
    }
}