package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.util.Application
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseStartupActivity(private val runOnlyOnce: Boolean = false) : StartupActivity {

    private var firstProjectOpening = AtomicBoolean(true)

    final override fun runActivity(project: Project) {
        if (Application.isUnitTestMode || project.isDisposed) {
            return
        }
        if (runOnlyOnce && !firstProjectOpening.compareAndSet(true, false)) {
            return
        }

        firstProjectOpening.set(false)
        if (onBeforeRunActivity(project)) {
            onRunActivity(project)
        }
    }

    protected open fun onBeforeRunActivity(project: Project): Boolean = true

    protected abstract fun onRunActivity(project: Project)
}