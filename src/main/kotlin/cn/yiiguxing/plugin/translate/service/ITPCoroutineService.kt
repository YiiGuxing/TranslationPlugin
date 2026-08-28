package cn.yiiguxing.plugin.translate.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


/**
 * A service that provides a [CoroutineScope] for launching coroutines.
 *
 * This service can be used at both the application and project levels.
 *
 * Usage:
 * - To get the application-level scope: `ITPCoroutineService.appScope()`
 * - To get the project-level scope: `ITPCoroutineService.projectScope(project)`
 *
 * Example:
 * ```
 * ITPCoroutineService.appScope().launch {
 *     // Your coroutine code here
 * }
 * ```
 *
 * or
 *
 * ```
 * ITPCoroutineService.projectScope(project).launch {
 *     // Your coroutine code here
 * }
 * ```
 *
 * @param coroutineScope The [CoroutineScope] to be used for launching coroutines.
 */
@Service(Service.Level.APP, Service.Level.PROJECT)
internal class ITPCoroutineService private constructor(private val coroutineScope: CoroutineScope) {

    /**
     * Launches a new coroutine without blocking the current thread.
     *
     * @see CoroutineScope.launch
     */
    fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return coroutineScope.launch(context, start, block)
    }

    /**
     * Creates a new coroutine and returns its future result as a [Deferred].
     *
     * @see CoroutineScope.async
     */
    fun <T> async(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        return coroutineScope.async(context, start, block)
    }

    companion object {
        @JvmStatic
        fun appScope(): ITPCoroutineService {
            return service<ITPCoroutineService>()
        }

        @JvmStatic
        fun projectScope(project: Project): ITPCoroutineService {
            return project.service<ITPCoroutineService>()
        }
    }
}