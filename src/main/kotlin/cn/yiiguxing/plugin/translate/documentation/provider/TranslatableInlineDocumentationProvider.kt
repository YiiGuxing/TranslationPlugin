package cn.yiiguxing.plugin.translate.documentation.provider

import cn.yiiguxing.plugin.translate.documentation.TranslatableInlineDocumentation
import com.intellij.openapi.util.TextRange
import com.intellij.platform.backend.documentation.InlineDocumentation
import com.intellij.platform.backend.documentation.InlineDocumentationProvider
import com.intellij.psi.PsiFile
import com.intellij.util.SmartList

@Suppress("UnstableApiUsage")
class TranslatableInlineDocumentationProvider : InlineDocumentationProvider {

    override fun inlineDocumentationItems(file: PsiFile): Collection<InlineDocumentation> {
        val result = SmartList<InlineDocumentation>()
        for (provider in InlineDocumentationProvider.EP_NAME.extensionList) {
            if (provider is TranslatableInlineDocumentationProvider) {
                continue
            }
            result.addAll(provider.inlineDocumentationItems(file))
        }

        return result.map { TranslatableInlineDocumentation(file, it) }
    }

    override fun findInlineDocumentation(file: PsiFile, textRange: TextRange): InlineDocumentation? {
        for (provider in InlineDocumentationProvider.EP_NAME.extensionList) {
            if (provider is TranslatableInlineDocumentationProvider) {
                continue
            }

            return provider.findInlineDocumentation(file, textRange)
                ?.let { TranslatableInlineDocumentation(file, it) }
                ?: continue
        }

        return null
    }
}
