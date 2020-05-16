package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.action.TranslateDocumentationAction
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.lang.documentation.DocumentationProviderEx
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.concurrency.runAsync
import java.util.concurrent.TimeoutException

/**
 * Translates documentation computed by another documentation provider. It should have
 *
 * order="first"
 *
 * in the extension declaration.
 */
class TranslatingDocumentationProvider : DocumentationProviderEx() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (!Settings.instance.translateDocumentation)
            return null

        return nullIfRecursive {
            translateOriginalDoc(element, originalElement)
        }
    }

    private fun translateOriginalDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val providerFromElement = DocumentationManager.getProviderFromElement(element, originalElement)
        val originalDoc = providerFromElement.generateDoc(element, originalElement)

        return translate(originalDoc)
    }

    companion object {
        private val recursion = ThreadLocal.withInitial{0}

        private fun <T> nullIfRecursive(computation: () -> T?): T? {
            if (recursion.get() > 0)
                return null

            recursion.set(recursion.get() + 1)

            try {
                return computation()
            } finally {
                recursion.set(recursion.get() - 1)
            }
        }

        //to reuse long running translation task
        private var lastTranslation: TranslationTask? = null

        private fun translate(text: String?): String? {
            text ?: return null

            val lastTask = lastTranslation

            val task =
                if (lastTask != null && lastTask.text == text) lastTask
                else TranslationTask(text)

            lastTranslation = task

            return task.nonBlockingGet()
        }

        private data class TranslationTask(val text: String) {
            private val totalTimeToWaitMs = 3_000
            private val timeToBlockMs = 100

            private var tries = totalTimeToWaitMs / timeToBlockMs

            //execute on a different thread outside read action
            private val promise = runAsync {
                TranslateDocumentationAction.getTranslatedDocumentation(text)
            }

            fun nonBlockingGet(): String? {
                //blocking for the whole time can lead to ui freezes, so we need to periodically do `checkCanceled`
                while (tries > 0) {
                    tries -= 1
                    ProgressManager.checkCanceled()
                    try {
                        return promise.blockingGet(timeToBlockMs)
                    }
                    catch (t: TimeoutException) {
                        //ignore
                    }
                    catch (e: Throwable) {
                        TranslateDocumentationAction.logAndShowWarning(e, null)
                        return null
                    }
                }

                //translation is not ready yet, show original documentation
                return null
            }
        }
    }
}
