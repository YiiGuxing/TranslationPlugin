package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.microsoft.models.DictionaryExample
import cn.yiiguxing.plugin.translate.trans.microsoft.models.DictionaryExampleItem
import cn.yiiguxing.plugin.translate.trans.text.ExampleDocument
import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument
import cn.yiiguxing.plugin.translate.util.text.StyledString

/**
 * Microsoft example document factory.
 */
internal object MicrosoftExampleDocumentFactory :
    TranslationDocument.Factory<List<DictionaryExample>, ExampleDocument> {
    override fun getDocument(input: List<DictionaryExample>): ExampleDocument? {
        if (input.isEmpty()) {
            return null
        }

        return input.asSequence()
            .mapNotNull { it.examples.firstOrNull() }
            .map(::toExampleStrings)
            .toList()
            .takeIf { it.isNotEmpty() }
            ?.let { ExampleDocument(it) }
    }

    private fun toExampleStrings(example: DictionaryExampleItem): List<CharSequence> {
        return listOf(
            example.sourcePrefix,
            StyledString(example.sourceTerm, ExampleDocument.STYLE_EXAMPLE_BOLD),
            example.sourceSuffix,
            StyledString("\t", ExampleDocument.STYLE_EXAMPLE_SPACE),
            example.targetPrefix,
            StyledString(example.targetTerm, ExampleDocument.STYLE_EXAMPLE_BOLD),
            example.targetSuffix
        )
    }
}