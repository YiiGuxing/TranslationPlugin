package cn.yiiguxing.plugin.translate.documentation.provider

import cn.yiiguxing.plugin.translate.documentation.TranslatableDocumentationTarget
import com.intellij.openapi.diagnostic.logger
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

private val LOG = logger<TranslatablePsiDocumentationTargetProvider>()
private val PSI_DOCUMENTATION_TARGET_METHOD_HANDLE: MethodHandle? by lazy {
    getPsiTargetMethodHandle("psiDocumentationTarget", DocumentationTarget::class.java)
}
private val PSI_DOCUMENTATION_TARGETS_METHOD_HANDLE: MethodHandle? by lazy {
    getPsiTargetMethodHandle("psiDocumentationTargets", List::class.java)
}
private val RECURSION = ThreadLocal.withInitial { false }

class TranslatablePsiDocumentationTargetProvider : PsiDocumentationTargetProvider {
    override fun documentationTarget(
        element: PsiElement,
        originalElement: PsiElement?
    ): DocumentationTarget? {
        return nullIfRecursiveOrError {
            PSI_DOCUMENTATION_TARGET_METHOD_HANDLE?.invoke(element, originalElement) as? DocumentationTarget
        }?.let { translatablePsiDocumentationTarget(it, element) }
    }

    // Support for 233+
    @Suppress("unused")
    fun documentationTargets(
        element: PsiElement,
        originalElement: PsiElement?
    ): List<DocumentationTarget> {
        @Suppress("UNCHECKED_CAST")
        return nullIfRecursiveOrError {
            PSI_DOCUMENTATION_TARGETS_METHOD_HANDLE?.invoke(element, originalElement) as? List<DocumentationTarget>
        }?.map { translatablePsiDocumentationTarget(it, element) } ?: emptyList()
    }
}


private fun translatablePsiDocumentationTarget(
    target: DocumentationTarget,
    psiElement: PsiElement
): TranslatableDocumentationTarget {
    return target as? TranslatableDocumentationTarget
        ?: TranslatableDocumentationTarget(
            project = psiElement.project,
            language = psiElement.language,
            delegate = target,
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

private fun getPsiTargetMethodHandle(name: String, returnType: Class<*>): MethodHandle? {
    return try {
        val methodType = MethodType.methodType(
            returnType,
            PsiElement::class.java,
            PsiElement::class.java
        )
        MethodHandles.privateLookupIn(
            TranslatablePsiDocumentationTargetProvider::class.java,
            MethodHandles.lookup()
        ).findStatic(
            Class.forName("com.intellij.lang.documentation.psi.UtilKt"),
            name,
            methodType
        )
    } catch (e: Throwable) {
        LOG.warn(
            "Failed to find method handle for `$name(PsiElement, PsiElement): ${returnType.simpleName}`.",
            e
        )
        null
    }
}