package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.trans.text.ExampleDocument
import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument
import cn.yiiguxing.plugin.translate.util.chunked
import cn.yiiguxing.plugin.translate.util.text.StyledString

object GoogleExampleDocumentFactory : TranslationDocument.Factory<GExamples?, ExampleDocument> {

    private val BOLD_REGEX = Regex("<b>(.+?)</b>")

    override fun getDocument(input: GExamples?): ExampleDocument? {
        if (input == null || input.examples.isEmpty()) {
            return null
        }

        val examples = input.examples.asSequence()
            .map { (example) ->
                example.chunked(BOLD_REGEX) { StyledString(it.groupValues[1], ExampleDocument.STYLE_EXAMPLE_BOLD) }
            }
            .toList()
        return ExampleDocument(examples)
    }
}