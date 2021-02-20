package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.Application
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

    override fun init(toolWindow: ToolWindow) {
        Application.messageBus
            .connect(TranslationUIManager.disposable())
            .subscribe(WordBookListener.TOPIC, object : WordBookListener {

                override fun onWordAdded(service: WordBookService, wordBookItem: WordBookItem) {
                    toolWindow.isAvailable = true
                }

                override fun onWordRemoved(service: WordBookService, id: Long) {
                    if (service.getWords().isEmpty()) {
                        toolWindow.isAvailable = false
                    }
                }
            })

    }

    override fun shouldBeAvailable(project: Project): Boolean = WordBookService.instance.getWords().isNotEmpty()

    companion object {
        const val TOOL_WINDOW_ID = "Word Book"
    }

}