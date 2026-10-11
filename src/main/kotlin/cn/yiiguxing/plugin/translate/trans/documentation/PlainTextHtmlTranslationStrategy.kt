package cn.yiiguxing.plugin.translate.trans.documentation

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Translates translatable segments as plain text for translation services that
 * cannot recognize any HTML tags.
 *
 * Consecutive translatable nodes are merged into a single translation unit to
 * preserve the sentence context, but all tags (including protected tags) are
 * stripped when the segment is serialized: only the text content is passed to
 * the translation service. The translated text is written back as plain text
 * without restoring any tag structure.
 */
object PlainTextHtmlTranslationStrategy : HtmlTranslationStrategy {

    override val mergeAdjacentTranslatableNodes: Boolean = true

    override fun isTranslatableLeafElement(element: Element): Boolean {
        return isTranslatableInlineLeafElement(element)
    }

    override fun serialize(nodes: List<Node>): String {
        return nodes.joinToString("") { node ->
            when (node) {
                is TextNode -> node.text()
                is Element -> node.text()
                else -> ""
            }
        }
    }

    override fun deserialize(originalNodes: List<Node>, translatedText: String): List<Node>? {
        if (translatedText.isBlank()) {
            return null
        }

        return listOf(TextNode(translatedText))
    }
}
