package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.DefaultHyperlinkListener
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.checkDispatchThread
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowBalloonShowOptions
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType

/** The id of the wordbook tool window. */
const val WORDBOOK_TOOL_WINDOW_ID = "Translation.Wordbook"

private val TOOLTIP_KEY = TranslationPlugin.generateId("tooltip.wordbook.storage.path")

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
                    setAvailable(true) {
                        show { showTooltipIfNeed(this.project) }
                    }
                }
            }
        })

        toolWindow.runIfSurvive { showTooltipIfNeed(this.project) }
    }

    override fun stateChanged(toolWindowManager: ToolWindowManager, changeType: ToolWindowManagerEventType) {
        if (changeType == ToolWindowManagerEventType.ActivateToolWindow) {
            val toolWindow = toolWindowManager.getToolWindow(WORDBOOK_TOOL_WINDOW_ID) ?: return
            toolWindow.runIfSurvive { showTooltipIfNeed(toolWindow.project) }
        }
    }

    private fun showTooltipIfNeed(project: Project) {
        val properties = PropertiesComponent.getInstance()
        if (project.isDisposed || properties.getBoolean(TOOLTIP_KEY, false)) {
            return
        }
        if (!Settings.getInstance().wordbookStoragePath.isNullOrEmpty()) {
            properties.setValue(TOOLTIP_KEY, true)
            return
        }

        val toolWindowManager = ToolWindowManager.getInstance(project)
        if (!toolWindowManager.canShowNotification(WORDBOOK_TOOL_WINDOW_ID)) {
            return
        }

        properties.setValue(TOOLTIP_KEY, true)
        toolWindowManager.notifyByBalloon(
            ToolWindowBalloonShowOptions(
                toolWindowId = WORDBOOK_TOOL_WINDOW_ID,
                type = MessageType.INFO,
                htmlBody = message("tooltip.wordbook.storage.path"),
                listener = DefaultHyperlinkListener()
            )
        )
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