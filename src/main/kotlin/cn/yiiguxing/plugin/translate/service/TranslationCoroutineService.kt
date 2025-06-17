package cn.yiiguxing.plugin.translate.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


@Service(Service.Level.APP, Service.Level.PROJECT)
class TranslationCoroutineService private constructor(private val coroutineScope: CoroutineScope) {

    fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return coroutineScope.launch(context, start, block)
    }

    companion object {
        @JvmStatic
        fun appScope(): TranslationCoroutineService {
            return service<TranslationCoroutineService>()
        }

        @JvmStatic
        fun projectScope(project: Project): TranslationCoroutineService {
            return project.service<TranslationCoroutineService>()
        }
    }
}