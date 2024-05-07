package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.microsoft.models.DictionaryExample
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

        val examples = input.asSequence()
            .mapNotNull { it.examples.firstOrNull() }
            .map {
                listOf(
                    it.sourcePrefix,
                    StyledString(it.sourceTerm, ExampleDocument.STYLE_EXAMPLE_BOLD),
                    it.sourceSuffix,
                    " - ",
                    it.targetPrefix,
                    StyledString(it.targetTerm, ExampleDocument.STYLE_EXAMPLE_BOLD),
                    it.targetSuffix
                )
            }
            .toList()

        return ExampleDocument(examples)
    }
}