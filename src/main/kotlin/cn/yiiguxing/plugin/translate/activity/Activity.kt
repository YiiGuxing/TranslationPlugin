package cn.yiiguxing.plugin.translate.activity

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

object Activity {
    fun runLater(project: Project, delayCount: Int, action: () -> Unit) {
        if (project.isDisposed) {
            return
        }

        if (delayCount > 0) {
            ToolWindowManager.getInstance(project).invokeLater {
                runLater(project, delayCount - 1, action)
            }
        } else {
            action()
        }
    }
}