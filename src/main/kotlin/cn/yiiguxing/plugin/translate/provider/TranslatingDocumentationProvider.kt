package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.documentation.DocTranslations
import cn.yiiguxing.plugin.translate.documentation.InlayDocTranslations
import cn.yiiguxing.plugin.translate.documentation.TranslateDocumentationTask
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.TranslateService
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.lang.Language
import com.intellij.lang.documentation.DocumentationProviderEx
import com.intellij.lang.documentation.ExternalDocumentationProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.util.ui.JBUI

/**
 * Translates documentation computed by another documentation provider. It should have
 *
 * `order = "first"`
 *
 * in the extension declaration.
 */
class TranslatingDocumentationProvider : DocumentationProviderEx(), ExternalDocumentationProvider {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (!isTranslateDocumentation(element)) {
            return null
        }

        return nullIfRecursive {
            val providerFromElement = DocumentationManager.getProviderFromElement(element, originalElement)
            val originalDoc = nullIfError { providerFromElement.generateDoc(element, originalElement) }
            val translatedDoc = translate(originalDoc, element?.language)
            translatedDoc ?: addTranslationFailureMessage(originalDoc)
        }
    }

    override fun fetchExternalDocumentation(
        project: Project?,
        element: PsiElement?,
        docUrls: MutableList<String>?,
        onHover: Boolean
    ): String? {
        if (!isTranslateDocumentation(element)) {
            return null
        }

        return nullIfRecursive {
            val (language, providerFromElement) = Application.runReadAction(Computable {
                element?.takeIf { it.isValid }?.language to DocumentationManager.getProviderFromElement(element, null)
            })
            val originalDoc = when (providerFromElement) {
                is ExternalDocumentationProvider -> nullIfError {
                    providerFromElement.fetchExternalDocumentation(project, element, docUrls, onHover)
                }

                else -> null
            }

            translate(originalDoc, language) ?: addTranslationFailureMessage(originalDoc)
        }
    }

    @Suppress("UnstableApiUsage")
    override fun generateRenderedDoc(docComment: PsiDocCommentBase): String? {
        val translationResult = InlayDocTranslations.getTranslationResult(docComment) ?: return null

        return nullIfRecursive {
            val providerFromElement = DocumentationManager.getProviderFromElement(docComment)
            val originalDoc = nullIfError { providerFromElement.generateRenderedDoc(docComment) }

            if (translationResult.original == originalDoc) {
                return@nullIfRecursive translationResult.translation
            }

            translate(originalDoc, docComment.language).also { translation ->
                InlayDocTranslations.updateTranslationResult(
                    docComment,
                    translation?.let { InlayDocTranslations.TranslationResult(originalDoc, it) }
                )
            }
        }
    }

    override fun canPromptToConfigureDocumentation(element: PsiElement?): Boolean {
        return false
    }

    override fun promptToConfigureDocumentation(element: PsiElement?) {}

    // This method is deprecated and not used by the platform
    @Suppress("OVERRIDE_DEPRECATION")
    override fun hasDocumentationFor(element: PsiElement?, originalElement: PsiElement?): Boolean {
        return false
    }

    companion object {
        private val recursion = ThreadLocal.withInitial { 0 }

        // To reuse long-running translation task
        private var lastTranslationTask: TranslateDocumentationTask? = null

        private fun isTranslateDocumentation(element: PsiElement?): Boolean {
            return DocTranslations.getTranslationState(element) ?: Settings.instance.translateDocumentation
        }

        /**
         * 用于[DocumentationProviderEx]和[ExternalDocumentationProvider]生成文档方法的包装调用，
         * 此方法应该在[nullIfRecursive]方法的`computation`参数中调用：
         *
         * ```
         * nullIfRecursive {
         *     val originalDoc = nullIfError { provider.generateDoc(element, originalElement) }
         *     // ...
         * }
         * ```
         * 它的作用是：
         * 如果在调用真正的文档提供者生成文档时发生错误的话，屏蔽错误并跳过文档翻译。错误将会在轮到真正的文档提供者
         * 自己提供文档时的重新发生，此时的错误就和[TranslatingDocumentationProvider]无关了，
         * 否则原本是属于真正文档提供者的错误将会被IDE误认为是[TranslatingDocumentationProvider]的错误，
         * 避免插件自己背黑锅，例如
         * [#1203](https://github.com/YiiGuxing/TranslationPlugin/issues/1203)
         *
         * 2022/5/9 去除`inline`关键字，使之变为普通方法，以便`Github Action`识别自动处理。
         */
        private fun <T> nullIfError(block: () -> T?): T? {
            return try {
                block()
            } catch (e: Throwable) {
                null
            }
        }

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

        private fun translate(text: String?, language: Language?): String? {
            if (text.isNullOrEmpty()) return null

            val lastTask = lastTranslationTask
            val translator = TranslateService.translator

            val task = if (
                lastTask != null &&
                lastTask.translator.id == translator.id &&
                lastTask.text == text &&
                (lastTask.isSucceeded || !lastTask.isProcessed)
            ) lastTask
            else TranslateDocumentationTask(text, language, translator)

            lastTranslationTask = task

            return task.nonBlockingGet()
        }

        private fun addTranslationFailureMessage(doc: String?): String? {
            doc ?: return null

            val message = message("doc.message.translation.failure.please.try.again")
            val color = JBUI.CurrentTheme.Label.disabledForeground()
            return DocTranslations.addMessage(doc, message, color)
        }
    }
}
