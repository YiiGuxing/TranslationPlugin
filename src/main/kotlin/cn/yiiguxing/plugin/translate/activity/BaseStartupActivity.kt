package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.util.Application
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.ToolWindowManager

abstract class BaseStartupActivity(private val runOnlyOnce: Boolean = false) : StartupActivity {

    private var veryFirstProjectOpening: Boolean = true

    final override fun runActivity(project: Project) {
        if (Application.isUnitTestMode || (runOnlyOnce && !veryFirstProjectOpening)) {
            return
        }

        veryFirstProjectOpening = false
        if (onBeforeRunActivity(project)) {
            runLater(project, 3) { onRunActivity(project) }
        }
    }

    protected open fun onBeforeRunActivity(project: Project): Boolean = true

    protected abstract fun onRunActivity(project: Project)

    private companion object {
        fun runLater(project: Project, delayCount: Int, action: () -> Unit) {
            if (project.isDisposed) {
                return
            }

            if (delayCount > 0) {
                ToolWindowManager.getInstance(project).invokeLater(
                    Runnable { runLater(project, delayCount - 1, action) }
                )
            } else {
                action()
            }
        }
    }

}