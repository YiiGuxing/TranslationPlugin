package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.checkDispatchThread
import cn.yiiguxing.plugin.translate.util.invokeLater
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Ref
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import org.jetbrains.concurrency.runAsync

/**
 * Word book tool window factory
 */
class WordBookToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Try to fix: https://github.com/YiiGuxing/TranslationPlugin/issues/1186
        if (project.isDisposed) {
            return
        }
        WordBookView.instance.setup(project, toolWindow)
    }

    override fun init(toolWindow: ToolWindow) {
        val toolWindowRef: Ref<ToolWindow?> = Ref.create(toolWindow)
        Disposer.register(toolWindow.disposable) { toolWindowRef.set(null) }

        val project = (toolWindow as ToolWindowEx).project
        val messageBusConnection = project.messageBus.connect(toolWindow.disposable)
        messageBusConnection.subscribe(RequireWordBookListener.TOPIC, object : RequireWordBookListener {
            override fun onRequire() {
                toolWindowRef.get()?.runIfSurvive {
                    if (isAvailable) {
                        return@runIfSurvive
                    }
                    setAvailable(true) {
                        isShowStripeButton = true
                        show()
                    }
                }
            }
        })
        messageBusConnection.subscribe(WordBookListener.TOPIC, object : WordBookListener {
            override fun onWordsAdded(service: WordBookService, words: List<WordBookItem>) {
                toolWindowRef.get()?.runIfSurvive { isAvailable = true }
            }

            override fun onWordsRemoved(service: WordBookService, wordIds: List<Long>) {
                Application.executeOnPooledThread {
                    val available = WordBookService.instance.hasAnyWords()
                    toolWindowRef.get()?.runIfSurvive { isAvailable = available }
                }
            }

            override fun onStoragePathChanged(service: WordBookService) {
                updateAvailable(toolWindowRef)
            }
        })

        val disposable = Disposer.newDisposable(toolWindow.disposable, "Wordbook tool window availability state")
        WordBookService.instance.stateBinding.observe(disposable) { state, _ ->
            if (state == WordBookState.RUNNING) {
                Disposer.dispose(disposable)
                updateAvailable(toolWindowRef)
            }
        }
        if (WordBookService.instance.isInitialized) {
            Disposer.dispose(disposable)
            updateAvailable(toolWindowRef)
        }
    }

    private fun updateAvailable(toolWindowRef: Ref<ToolWindow?>) {
        runAsync { with(WordBookService.instance) { isInitialized && hasAnyWords() } }
            .onSuccess { available ->
                toolWindowRef.get()?.runIfSurvive { isAvailable = available }
            }
    }

    override fun shouldBeAvailable(project: Project): Boolean = false

    companion object {
        const val TOOL_WINDOW_ID = "Word Book"

        private val requirePublisher: RequireWordBookListener by lazy {
            Application.messageBus.syncPublisher(RequireWordBookListener.TOPIC)
        }

        fun requireWordBook() {
            checkDispatchThread { "Must only be invoked from the Event Dispatch Thread." }
            requirePublisher.onRequire()
        }

        private inline fun ToolWindow.runIfSurvive(crossinline action: ToolWindow.() -> Unit) {
            if (isDisposed) {
                return
            }
            invokeLater(ModalityState.NON_MODAL, (this as ToolWindowEx).project.disposed) {
                if (!isDisposed) {
                    action()
                }
            }
        }
    }
}