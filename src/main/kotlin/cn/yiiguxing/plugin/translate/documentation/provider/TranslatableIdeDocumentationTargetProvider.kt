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


@Suppress("UnstableApiUsage")
class TranslatableIdeDocumentationTargetProvider(
    private val project: Project
) : IdeDocumentationTargetProviderImpl(project) {

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