package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.ui.icon.Icons
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

/**
 * Word book tool window factory
 *
 * Created by Yii.Guxing on 2019/08/26.
 */
class WordBookToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun init(window: ToolWindow) {
        super.init(window)
        window.icon = Icons.Wordbook
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        WordBookView.instance.setup(toolWindow)
    }

}