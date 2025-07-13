package cn.yiiguxing.plugin.translate.documentation.service

import com.intellij.codeInsight.documentation.render.DocRenderItem
import com.intellij.codeInsight.documentation.render.InlineDocumentationFinder
import com.intellij.openapi.client.ClientKind
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.InlineDocumentation

@Suppress("UnstableApiUsage")
class TranslatableInlineDocumentationFinder : InlineDocumentationFinder {
    override fun getInlineDocumentation(item: DocRenderItem): InlineDocumentation? {
        val services = item.editor.project?.getServices(
            InlineDocumentationFinder::class.java,
            ClientKind.ALL
        )

        println("TranslatableInlineDocumentationFinder.getInlineDocumentation: $services")

        return null
    }

    override fun getInlineDocumentationTarget(item: DocRenderItem): DocumentationTarget? {
        return null
    }
}