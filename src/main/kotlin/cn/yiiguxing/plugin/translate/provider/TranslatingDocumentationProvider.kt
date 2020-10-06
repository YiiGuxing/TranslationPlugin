package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.documentation.TranslateDocumentationTask
import cn.yiiguxing.plugin.translate.util.TranslateService
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.lang.Language
import com.intellij.lang.documentation.DocumentationProviderEx
import com.intellij.psi.PsiElement

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

        return translate(originalDoc, element?.language)
    }

    companion object {
        private val recursion = ThreadLocal.withInitial { 0 }

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
        private var lastTranslation: TranslateDocumentationTask? = null

        private fun translate(text: String?, language: Language?): String? {
            if (text.isNullOrEmpty()) return null

            val lastTask = lastTranslation
            val translator = TranslateService.translator

            val task =
                if (lastTask != null && lastTask.translator.id == translator.id && lastTask.text == text) lastTask
                else TranslateDocumentationTask(text, language, translator)

            lastTranslation = task

            return task.nonBlockingGet()
        }
    }
}
