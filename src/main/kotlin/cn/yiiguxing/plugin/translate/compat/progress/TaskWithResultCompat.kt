package cn.yiiguxing.plugin.translate.compat.progress

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.ProgressManagerImpl
import com.intellij.openapi.project.Project
import java.lang.reflect.Method
import javax.swing.JComponent

abstract class TaskWithResultCompat<T, E : Exception>(
    project: Project?,
    private val component: JComponent?,
    title: String,
    canBeCancelled: Boolean = true
) : Task.WithResult<T, E>(
    project, title, canBeCancelled
) {

    private var isSetParentComponent = false

    init {
        setParentComponent()
    }

    private fun setParentComponent() {
        try {
            Task::class.java.getDeclaredField("myParentComponent").apply {
                isAccessible = true
                set(this@TaskWithResultCompat, component)
                isAccessible = false
            }
            isSetParentComponent = true
        } catch (e: NoSuchFieldException) {
            // no-op
        }
    }

    fun queueAndGet(): T? {
        return try {
            if (!isSetParentComponent && runProcessWithProgressSynchronouslyMethod != null) {
                val progressManager = ProgressManager.getInstance() as ProgressManagerImpl
                runProcessWithProgressSynchronouslyMethod!!.invoke(progressManager, this, component)
                result
            } else {
                ProgressManager.getInstance().run(this)
            }
        } catch (e: ProcessCanceledException) {
            null
        }
    }


    companion object {
        private val runProcessWithProgressSynchronouslyMethod: Method? by lazy {
            try {
                ProgressManagerImpl::class.java.getMethod(
                    "runProcessWithProgressSynchronously",
                    Task::class.java,
                    JComponent::class.java
                )
            } catch (e: NoSuchMethodException) {
                null
            }
        }
    }

}