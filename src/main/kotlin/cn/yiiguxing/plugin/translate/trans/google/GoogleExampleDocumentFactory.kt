package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.trans.text.ExampleDocument
import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument
import cn.yiiguxing.plugin.translate.util.chunked
import cn.yiiguxing.plugin.translate.util.text.StyledString

object GoogleExampleDocumentFactory : TranslationDocument.Factory<List<GSourceExample>?, ExampleDocument> {

    private val BOLD_REGEX = Regex("<b>(.+?)</b>")

    override fun getDocument(input: List<GSourceExample>?): ExampleDocument? {
        if (input.isNullOrEmpty()) {
            return null
        }

        val examples = input.asSequence()
            .map { (example) ->
                example.chunked(BOLD_REGEX) { StyledString(it.groupValues[1], ExampleDocument.STYLE_EXAMPLE_BOLD) }
            }
            .toList()
        return ExampleDocument(examples)
    }
}