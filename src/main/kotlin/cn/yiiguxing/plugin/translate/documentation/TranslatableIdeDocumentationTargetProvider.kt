package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.Settings
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.documentation.ide.impl.IdeDocumentationTargetProviderImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.psi.PsiFile

@Suppress("UnstableApiUsage")
class TranslatableIdeDocumentationTargetProvider(
    private val project: Project
) : IdeDocumentationTargetProviderImpl(project) {

    private val settings: Settings by lazy { service<Settings>() }

    override fun documentationTarget(
        editor: Editor,
        file: PsiFile,
        lookupElement: LookupElement
    ): DocumentationTarget? {
        return super.documentationTarget(editor, file, lookupElement)
            ?.let { TranslatableDocumentationTarget(project, it, settings.translateDocumentation) }
    }

    override fun documentationTargets(editor: Editor, file: PsiFile, offset: Int): List<DocumentationTarget> {
        val shouldTranslate = settings.translateDocumentation
        return super.documentationTargets(editor, file, offset)
            .map { TranslatableDocumentationTarget(project, it, shouldTranslate) }
    }
}