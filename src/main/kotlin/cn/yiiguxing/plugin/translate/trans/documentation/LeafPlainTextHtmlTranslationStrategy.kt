package cn.yiiguxing.plugin.translate.trans.documentation

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Translates the text content of each leaf element as plain text for translation
 * services that cannot recognize any HTML tags.
 *
 * A translatable leaf element is a true leaf element: an element without any
 * element children. Each text node and leaf element is translated separately,
 * which may reduce the translation quality due to the loss of context, but it
 * is the only viable approach for translation services without HTML support.
 */
object LeafPlainTextHtmlTranslationStrategy : HtmlTranslationStrategy {

    override val mergeAdjacentTranslatableNodes: Boolean = false

    override fun isTranslatableLeafElement(element: Element): Boolean {
        return element.children().isEmpty()
    }

    override fun shouldTraverseElement(element: Element): Boolean {
        return !element.shouldSkipTranslation() &&
                element.tagName().lowercase() !in PROTECTED_INLINE_TAGS
    }

    override fun serialize(nodes: List<Node>): String {
        return when (val node = nodes.firstOrNull()) {
            is TextNode -> node.text()
            is Element -> node.text()
            else -> ""
        }
    }

    override fun deserialize(originalNodes: List<Node>, translatedText: String): List<Node>? {
        if (translatedText.isBlank()) {
            return null
        }

        val original = originalNodes.firstOrNull() ?: return null
        return listOf(
            when (original) {
                is TextNode -> TextNode(translatedText)
                is Element -> original.clone().apply {
                    empty()
                    appendText(translatedText)
                }

                else -> original
            }
        )
    }
}
