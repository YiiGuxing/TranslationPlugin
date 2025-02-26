package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.util.Application
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseStartupActivity(
    private val runOnlyOnce: Boolean = false,
    private val runInHeadless: Boolean = true
) : ProjectActivity {

    private var firstProjectOpening = AtomicBoolean(true)

    final override suspend fun execute(project: Project) {
        if (!runInHeadless && Application.isHeadlessEnvironment) {
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

    protected open suspend fun onBeforeRunActivity(project: Project): Boolean = true

    protected abstract suspend fun onRunActivity(project: Project)
}