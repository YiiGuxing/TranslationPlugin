package cn.yiiguxing.plugin.translate.documentation.provider

import cn.yiiguxing.plugin.translate.documentation.TranslatableDocumentationTarget
import com.intellij.lang.documentation.psi.psiDocumentationTargets
import com.intellij.openapi.diagnostic.logger
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement

private val LOG = logger<TranslatablePsiDocumentationTargetProvider>()
private val RECURSION = ThreadLocal.withInitial { false }

class TranslatablePsiDocumentationTargetProvider : PsiDocumentationTargetProvider {
    override fun documentationTarget(
        element: PsiElement,
        originalElement: PsiElement?
    ): DocumentationTarget? {
        return null
    }

    override fun documentationTargets(
        element: PsiElement,
        originalElement: PsiElement?
    ): List<DocumentationTarget> = nullIfRecursiveOrError {
        @Suppress("UnstableApiUsage")
        psiDocumentationTargets(element, originalElement).map {
            translatablePsiDocumentationTarget(it, element)
        }
    } ?: emptyList()
}


private fun translatablePsiDocumentationTarget(
    target: DocumentationTarget,
    psiElement: PsiElement
): TranslatableDocumentationTarget {
    return target as? TranslatableDocumentationTarget
        ?: TranslatableDocumentationTarget(
            project = psiElement.project,
            language = psiElement.language,
            wrapped = target,
            psiElement = psiElement
        )
}

private inline fun <T> nullIfRecursiveOrError(computation: () -> T?): T? {
    if (RECURSION.get()) {
        return null
    }

    RECURSION.set(true)

    return try {
        computation()
    } catch (e: Throwable) {
        LOG.warn("Error occurred while computing documentation target.", e)
        null
    } finally {
        RECURSION.set(false)
    }
}