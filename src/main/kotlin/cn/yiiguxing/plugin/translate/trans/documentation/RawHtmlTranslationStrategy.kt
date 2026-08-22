package cn.yiiguxing.plugin.translate.trans.documentation

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Translates translatable segments as raw HTML for translation services that can
 * recognize HTML tags in any form and translate them directly.
 *
 * Translatable segments are serialized as-is: text nodes are HTML-escaped and
 * translatable leaf elements keep their full HTML, including attributes. The
 * translated HTML is parsed and applied back directly, without any tag
 * substitution or restoration.
 *
 * The content of [PROTECTED_INLINE_TAGS] is sent to the translation service to
 * preserve the sentence context, but it is restored to the original text when
 * the translation is applied.
 */
object RawHtmlTranslationStrategy : HtmlTranslationStrategy {

    override val mergeAdjacentTranslatableNodes: Boolean = true

    override fun isTranslatableLeafElement(element: Element): Boolean {
        return isTranslatableInlineLeafElement(element)
    }

    override fun serialize(nodes: List<Node>): String {
        val out = StringBuilder()
        for (node in nodes) {
            when (node) {
                is TextNode -> out.append(escapeText(node.text()))
                is Element -> out.append(node.outerHtml())
            }
        }
        return out.toString()
    }

    override fun deserialize(originalNodes: List<Node>, translatedText: String): List<Node>? {
        val translatedNodes = Jsoup.parseBodyFragment(translatedText).body().childNodes().toList()
        if (translatedNodes.isEmpty()) {
            return null
        }

        return restoreProtectedElements(originalNodes, translatedNodes)
    }

    private fun restoreProtectedElements(originalNodes: List<Node>, translatedNodes: List<Node>): List<Node> {
        val protectedElements = originalNodes
            .filterIsInstance<Element>()
            .filter { it.tagName().lowercase() in PROTECTED_INLINE_TAGS }
        if (protectedElements.isEmpty()) {
            return translatedNodes
        }

        var index = 0
        return translatedNodes.map { node ->
            if (node is Element && node.tagName().lowercase() in PROTECTED_INLINE_TAGS) {
                protectedElements.getOrNull(index++)?.clone() ?: node
            } else {
                node
            }
        }
    }
}
