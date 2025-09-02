package cn.yiiguxing.plugin.translate.documentation.provider

import cn.yiiguxing.plugin.translate.documentation.TranslatableDocumentationTarget
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.Language
import com.intellij.lang.documentation.ide.impl.IdeDocumentationTargetProviderImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.psi.PsiFile
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType


private val LOG = logger<TranslatableIdeDocumentationTargetProvider>()
private val DOCUMENTATION_TARGETS_METHOD_HANDLE: MethodHandle? by lazy {
    try {
        val methodType = MethodType.methodType(
            List::class.java,
            Editor::class.java,
            PsiFile::class.java,
            LookupElement::class.java
        )
        MethodHandles.privateLookupIn(
            TranslatableIdeDocumentationTargetProvider::class.java,
            MethodHandles.lookup()
        ).findSpecial(
            @Suppress("UnstableApiUsage")
            IdeDocumentationTargetProviderImpl::class.java,
            "documentationTargets",
            methodType,
            TranslatableIdeDocumentationTargetProvider::class.java
        )
    } catch (e: Throwable) {
        LOG.warn(
            "Failed to find method handle for `IdeDocumentationTargetProviderImpl." +
                    "documentationTargets(Editor, PsiFile, LookupElement): List<DocumentationTarget>`.",
            e
        )
        null
    }
}


@Suppress("UnstableApiUsage")
class TranslatableIdeDocumentationTargetProvider(
    private val project: Project
) : IdeDocumentationTargetProviderImpl(project) {

    // Support for 241+
    @Suppress("unused")
    fun documentationTargets(editor: Editor, file: PsiFile, lookupElement: LookupElement): List<DocumentationTarget> {
        return try {
            @Suppress("UNCHECKED_CAST")
            DOCUMENTATION_TARGETS_METHOD_HANDLE?.invoke(
                this, editor, file, lookupElement
            ) as? List<DocumentationTarget>
        } catch (e: Throwable) {
            LOG.w(
                "Failed to invoke `IdeDocumentationTargetProviderImpl." +
                        "documentationTargets(Editor, PsiFile, LookupElement): List<DocumentationTarget>`.",
                e
            )
            null
        }
            ?.map { translatableDocumentationTarget(it, file.language) }
            ?: emptyList()
    }

    override fun documentationTarget(
        editor: Editor,
        file: PsiFile,
        lookupElement: LookupElement
    ): DocumentationTarget? {
        return super.documentationTarget(editor, file, lookupElement)
            ?.let { translatableDocumentationTarget(it, file.language) }
    }

    override fun documentationTargets(editor: Editor, file: PsiFile, offset: Int): List<DocumentationTarget> {
        return super.documentationTargets(editor, file, offset)
            .map { translatableDocumentationTarget(it, file.language) }
    }

    private fun translatableDocumentationTarget(
        target: DocumentationTarget,
        language: Language
    ): TranslatableDocumentationTarget {
        if (target is TranslatableDocumentationTarget) {
            return target
        }

        return TranslatableDocumentationTarget(project, language, target)
    }
}