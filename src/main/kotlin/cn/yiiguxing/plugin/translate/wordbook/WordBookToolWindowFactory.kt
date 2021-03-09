package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.invokeOnDispatchThread
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Ref
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

/**
 * Word book tool window factory
 */
class WordBookToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        WordBookView.instance.setup(project, toolWindow)
    }

    override fun init(toolWindow: ToolWindow) {
        val toolWindowRef: Ref<ToolWindow?> = Ref.create(toolWindow)
        val uiDisposable = TranslationUIManager.disposable()
        Disposer.register(uiDisposable, {
            toolWindowRef.set(null)
        })

        val messageBusConnection = Application.messageBus.connect(uiDisposable)
        messageBusConnection.subscribe(RequireWordBookListener.TOPIC, object : RequireWordBookListener {
            override fun onRequire() {
                toolWindow.isAvailable = true
                toolWindow.isShowStripeButton = true
                toolWindow.show()
            }
        })
        messageBusConnection.subscribe(WordBookListener.TOPIC, object : WordBookListener {
            override fun onWordAdded(service: WordBookService, wordBookItem: WordBookItem) {
                toolWindow.isAvailable = true
            }

            override fun onWordRemoved(service: WordBookService, id: Long) {
                Application.executeOnPooledThread {
                    val isAvailable = WordBookService.instance.hasAnyWords()
                    invokeOnDispatchThread {
                        toolWindowRef.get()?.isAvailable = isAvailable
                    }
                }
            }
        })

        Application.executeOnPooledThread {
            val isAvailable = WordBookService.instance.let { service ->
                service.isInitialized && service.hasAnyWords()
            }
            invokeOnDispatchThread {
                toolWindowRef.get()?.isAvailable = isAvailable
            }
        }
    }

    override fun shouldBeAvailable(project: Project): Boolean = false

    companion object {
        const val TOOL_WINDOW_ID = "Word Book"

        private val requirePublisher: RequireWordBookListener by lazy {
            Application.messageBus.syncPublisher(RequireWordBookListener.TOPIC)
        }

        fun requireWordBook() {
            requirePublisher.onRequire()
        }

    }

}