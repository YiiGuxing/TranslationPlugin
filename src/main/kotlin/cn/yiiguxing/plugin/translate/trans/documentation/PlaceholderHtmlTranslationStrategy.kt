package cn.yiiguxing.plugin.translate.trans.documentation

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Translates translatable segments with placeholder tags (`<b{level}{index}>`)
 * representing the element structure, for translation services that can
 * recognize basic tags but not tags with attributes.
 *
 * Translatable segments are serialized into text with placeholder tags instead
 * of the original elements, so that the translation service only translates the
 * text content and leaves the placeholders untouched. The translated text is
 * then written back to the original elements according to the placeholders.
 */
object PlaceholderHtmlTranslationStrategy : HtmlTranslationStrategy {

    /** Matches placeholder tags, e.g. `<b10>`, `</b10>` or `<b1 0>`. */
    private val PLACEHOLDER_TAG_REGEX = Regex(
        """<\s*(/?)\s*b\s*(\d+)\s*(\d*)\s*>""",
        RegexOption.IGNORE_CASE
    )

    /** Matches HTML tags that are not placeholder tags. */
    private val NON_PLACEHOLDER_TAG_REGEX = Regex("""<(?!\s*/?\s*b\s*\d+\s*\d*\s*>)[^>]*>""")

    private const val PLACEHOLDER_TAG_PREFIX = "b"

    override val mergeAdjacentTranslatableNodes: Boolean = true

    override fun isTranslatableLeafElement(element: Element): Boolean {
        return isTranslatableInlineLeafElement(element)
    }

    override fun serialize(nodes: List<Node>): String {
        val out = StringBuilder()
        serializeNodeList(nodes, 1, out)
        return out.toString()
    }

    override fun deserialize(originalNodes: List<Node>, translatedText: String): List<Node>? {
        val sanitized = escapeNonPlaceholderTags(translatedText)
        val normalized = sanitized.replace(PLACEHOLDER_TAG_REGEX) { match ->
            val groups = match.groupValues
            "<${groups[1]}$PLACEHOLDER_TAG_PREFIX${groups[2]}${groups[3]}>"
        }

        val translatedNodes = Jsoup.parseBodyFragment(normalized).body().childNodes().toList()
        if (translatedNodes.isEmpty()) {
            return null
        }

        return rebuildNodeList(originalNodes, translatedNodes)
    }

    /**
     * Serializes the given [nodes] into text for translation: text nodes are
     * escaped as-is, while element nodes are replaced with placeholder tags
     * (`<b{level}{index}>`) representing their structure, so that the translation
     * service only translates the text content and keeps the placeholders intact.
     *
     * @param level the level of the elements directly contained in [nodes].
     */
    private fun serializeNodeList(nodes: List<Node>, level: Int, out: StringBuilder) {
        var elementIndex = 0
        for (node in nodes) {
            when (node) {
                is TextNode -> out.append(escapeText(node.text()))
                is Element -> {
                    serializeElement(node.childNodes(), level, elementIndex, out)
                    elementIndex++
                }
            }
        }
    }

    private fun serializeElement(nodes: List<Node>, level: Int, index: Int, out: StringBuilder) {
        out.append('<').append(PLACEHOLDER_TAG_PREFIX).append(level).append(index).append('>')
        serializeNodeList(nodes, level + 1, out)
        out.append("</").append(PLACEHOLDER_TAG_PREFIX).append(level).append(index).append('>')
    }

    /**
     * Rebuilds the translated content of [translatedNodes] according to the
     * original structure of [nodes].
     *
     * Placeholder elements are mapped back to their original elements by index,
     * while text nodes are copied verbatim. The whole segment is rebuilt from
     * scratch instead of patching text nodes in place, so that a reordered or
     * otherwise reshaped translation is applied correctly.
     */
    private fun rebuildNodeList(nodes: List<Node>, translatedNodes: List<Node>): List<Node> {
        val originalElements = nodes.filterIsInstance<Element>()
        val result = mutableListOf<Node>()

        for (child in translatedNodes) {
            when (child) {
                is TextNode -> result.add(TextNode(child.text()))
                is Element -> {
                    val placeholder = parsePlaceholderTag(child.tagName()) ?: continue
                    val original = originalElements.getOrNull(placeholder.second) ?: continue
                    result.add(rebuildElement(original, child))
                }
            }
        }
        return result
    }

    private fun rebuildElement(original: Element, placeholder: Element): Element {
        val rebuilt = original.clone()
        if (rebuilt.tagName().lowercase() !in PROTECTED_INLINE_TAGS) {
            rebuilt.empty()
            rebuildNodeList(original.childNodes(), placeholder.childNodes()).forEach {
                rebuilt.appendChild(it)
            }
        }
        return rebuilt
    }

    private fun parsePlaceholderTag(tagName: String): Pair<Int, Int>? {
        if (tagName.length < 3 || !tagName.startsWith(PLACEHOLDER_TAG_PREFIX, ignoreCase = true)) {
            return null
        }

        val level = tagName.substring(1, 2).toIntOrNull() ?: return null
        val index = tagName.substring(2).toIntOrNull() ?: return null
        return level to index
    }

    private fun escapeNonPlaceholderTags(html: String): String {
        return html.replace(NON_PLACEHOLDER_TAG_REGEX) { match ->
            match.value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
        }
    }
}
