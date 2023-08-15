package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.documentation.DocNotifications
import cn.yiiguxing.plugin.translate.documentation.DocTranslationService
import cn.yiiguxing.plugin.translate.documentation.Documentations
import cn.yiiguxing.plugin.translate.documentation.TranslateDocumentationTask
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.TranslateService
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.util.runReadAction
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.lang.Language
import com.intellij.lang.documentation.DocumentationProviderEx
import com.intellij.lang.documentation.ExternalDocumentationProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.util.ui.JBUI
import java.util.concurrent.TimeoutException

/**
 * Translates documentation computed by another documentation provider. It should have
 *
 * `order = "first"`
 *
 * in the extension declaration.
 */
class TranslatedDocumentationProvider : DocumentationProviderEx(), ExternalDocumentationProvider {

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        if (!isTranslateDocumentation(element)) {
            return null
        }

        return nullIfRecursive {
            val providerFromElement = DocumentationManager.getProviderFromElement(element, originalElement)
            val originalDoc = nullIfError { providerFromElement.generateDoc(element, originalElement) }
            translate(originalDoc, element.language)
        }
    }

    override fun fetchExternalDocumentation(
        project: Project,
        element: PsiElement,
        docUrls: MutableList<String>?,
        onHover: Boolean
    ): String? {
        val isTranslated = runReadAction { isTranslateDocumentation(element) }
        if (!isTranslated) {
            return null
        }

        return nullIfRecursive {
            val (language, providerFromElement) = runReadAction {
                element.takeIf { it.isValid }?.language to DocumentationManager.getProviderFromElement(element, null)
            }
            val originalDoc = when (providerFromElement) {
                is ExternalDocumentationProvider -> nullIfError {
                    providerFromElement.fetchExternalDocumentation(project, element, docUrls, onHover)
                }

                else -> null
            }

            translate(originalDoc, language)
        }
    }

    @Suppress("UnstableApiUsage")
    override fun generateRenderedDoc(docComment: PsiDocCommentBase): String? {
        val translationResult = DocTranslationService.getInlayDocTranslation(docComment) ?: return null

        return nullIfRecursive {
            val providerFromElement = DocumentationManager.getProviderFromElement(docComment)
            val originalDoc = nullIfError { providerFromElement.generateRenderedDoc(docComment) }

            if (translationResult.original == originalDoc) {
                return@nullIfRecursive translationResult.translation
            }

            translateTask(originalDoc, docComment.language)
                ?.nonBlockingGetOrDefault {
                    if (it is TimeoutException) {
                        val project = docComment.project
                        invokeLater(expired = project.disposed) {
                            DocNotifications.showTranslationTimeoutWarning(project)
                        }
                    }
                    null
                }
                .also { translation ->
                    DocTranslationService.updateInlayDocTranslation(
                        docComment,
                        translation?.let { DocTranslationService.TranslationResult(originalDoc, it) }
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

    @Suppress("CompanionObjectInExtension")
    companion object {
        private val recursion = ThreadLocal.withInitial { 0 }

        // To reuse long-running translation task
        private var lastTranslationTask: TranslateDocumentationTask? = null

        private fun isTranslateDocumentation(element: PsiElement): Boolean {
            return DocTranslationService.getTranslationState(element) ?: Settings.instance.translateDocumentation
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
         * 自己提供文档时的重新发生，此时的错误就和[TranslatedDocumentationProvider]无关了，
         * 否则原本是属于真正文档提供者的错误将会被IDE误认为是[TranslatedDocumentationProvider]的错误，
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

        private fun translateTask(text: String?, language: Language?): TranslateDocumentationTask? {
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

            return task
        }

        private fun translate(text: String?, language: Language?): String? {
            return translateTask(text, language)?.nonBlockingGetOrDefault {
                val message = if (it is TimeoutException) {
                    message("doc.message.translation.timeout.please.try.again")
                } else {
                    message("doc.message.translation.failure.please.try.again")
                }
                addTranslationFailureMessage(text, message)
            }
        }

        private fun addTranslationFailureMessage(doc: String?, message: String): String? {
            doc ?: return null

            val color = JBUI.CurrentTheme.Label.disabledForeground()
            return Documentations.addMessage(doc, message, color)
        }
    }
}
