package cn.yiiguxing.plugin.translate.documentation.service

import cn.yiiguxing.plugin.translate.documentation.PsiTranslatableInlineDocumentation
import com.intellij.codeInsight.documentation.render.DocRenderItem
import com.intellij.codeInsight.documentation.render.InlineDocumentationFinder
import com.intellij.codeInsight.documentation.render.InlineDocumentationFinderImpl
import com.intellij.platform.backend.documentation.InlineDocumentation
import com.intellij.psi.PsiDocumentManager

@Suppress("UnstableApiUsage")
class TranslatableInlineDocumentationFinder(
    private val delegate: InlineDocumentationFinder = InlineDocumentationFinderImpl()
) : InlineDocumentationFinder by delegate {

    override fun getInlineDocumentation(item: DocRenderItem): InlineDocumentation? {
        val originDocumentation = delegate.getInlineDocumentation(item) ?: return null
        val psiDocumentManager = PsiDocumentManager.getInstance(item.editor.project ?: return originDocumentation)
        val file = psiDocumentManager.getPsiFile(item.editor.document) ?: return originDocumentation
        return PsiTranslatableInlineDocumentation(file, originDocumentation)
    }
}