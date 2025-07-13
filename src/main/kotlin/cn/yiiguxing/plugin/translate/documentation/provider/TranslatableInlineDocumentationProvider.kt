package cn.yiiguxing.plugin.translate.documentation.provider

import cn.yiiguxing.plugin.translate.documentation.TranslatableInlineDocumentation
import com.intellij.openapi.util.TextRange
import com.intellij.platform.backend.documentation.InlineDocumentation
import com.intellij.platform.backend.documentation.InlineDocumentationProvider
import com.intellij.psi.PsiFile

@Suppress("UnstableApiUsage")
class TranslatableInlineDocumentationProvider : InlineDocumentationProvider {

    override fun inlineDocumentationItems(file: PsiFile): Collection<InlineDocumentation> {
        return resolve {
            inlineDocumentationItems(file).map { TranslatableInlineDocumentation(file, it) }
        } ?: emptyList()
    }

    override fun findInlineDocumentation(file: PsiFile, textRange: TextRange): InlineDocumentation? = resolve {
        findInlineDocumentation(file, textRange)?.let { TranslatableInlineDocumentation(file, it) }
    }
}

private inline fun <T> resolve(block: InlineDocumentationProvider.() -> T?): T? {
    @Suppress("UnstableApiUsage")
    for (provider in InlineDocumentationProvider.EP_NAME.extensionList) {
        if (provider is TranslatableInlineDocumentationProvider) {
            continue
        }

        return provider.block() ?: continue
    }

    return null
}