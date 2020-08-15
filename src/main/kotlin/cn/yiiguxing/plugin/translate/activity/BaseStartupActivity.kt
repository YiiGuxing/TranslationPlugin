package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.util.Application
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

abstract class BaseStartupActivity(private val runOnlyOnce: Boolean = false) : StartupActivity {

    private var veryFirstProjectOpening: Boolean = true

    final override fun runActivity(project: Project) {
        if (Application.isUnitTestMode || (runOnlyOnce && !veryFirstProjectOpening)) {
            return
        }

        veryFirstProjectOpening = false
        if (onBeforeRunActivity(project)) {
            onRunActivity(project)
        }
    }

    protected open fun onBeforeRunActivity(project: Project): Boolean = true

    protected abstract fun onRunActivity(project: Project)
}