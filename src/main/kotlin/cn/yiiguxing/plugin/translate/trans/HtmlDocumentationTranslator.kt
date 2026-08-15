package cn.yiiguxing.plugin.translate.trans

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Translates the text content of an HTML document while keeping its structure,
 * styles and attributes (such as `href` or `src`) intact.
 *
 * This is a generic helper that can be used by any translator: it only requires
 * a plain-text translation function, so translators without HTML support can
 * translate HTML documents safely. Translatable text segments are serialized
 * into plain text with placeholder tags (`<b{level}{index}>`) representing the
 * element structure, so that the translation service only translates the text
 * content and leaves the placeholders untouched. The translated text is then
 * written back to the original text nodes according to the placeholders.
 *
 * @param translate the function used to translate the collected texts; the
 *   returned list must have the same size and order as the input list.
 *   Blank entries at any position leave the corresponding original text
 *   untouched.
 */
class HtmlDocumentationTranslator(
    private val translate: (List<String>) -> List<String>
) {

    companion object {

        /** Tags whose contents should not be translated. */
        private val SKIP_TAGS: Set<String> = setOf(
            "head", "script", "style", "pre",
            "object", "address", "noscript", "embed", "map", "area",
            "hr", "input", "title", "br", "frame", "iframe", "textarea",
            "select", "option", "optgroup", "button",
            "img", "picture", "audio", "video", "source", "track", "canvas",
            "svg", "math", "meta", "link", "base", "param", "wbr"
        )

        /** Inline tags. An element is a translatable leaf if all its element descendants are inline. */
        private val INLINE_TAGS: Set<String> = setOf(
            "a", "abbr", "acronym", "b", "bdi", "bdo", "big", "cite", "code", "data",
            "del", "dfn", "em", "font", "i", "ins", "kbd", "label", "mark", "q", "rp",
            "rt", "ruby", "s", "samp", "small", "span", "sub", "sup", "strong", "time",
            "tt", "u", "var"
        )

        /**
         * Inline tags whose content must not be translated.
         *
         * Unlike [SKIP_TAGS], these tags do not break a text segment: their content
         * is sent to the translation service to preserve the sentence context, but
         * the translated content is discarded when writing back, keeping the
         * original text intact.
         */
        private val PROTECTED_INLINE_TAGS: Set<String> = setOf("code", "samp", "kbd", "var")

        /** Matches placeholder tags, e.g. `<b10>`, `</b10>` or `<b1 0>`. */
        private val PLACEHOLDER_TAG_REGEX = Regex(
            """<\s*(/?)\s*b\s*(\d+)\s*(\d*)\s*>""",
            RegexOption.IGNORE_CASE
        )

        /** Matches HTML tags that are not placeholder tags. */
        private val NON_PLACEHOLDER_TAG_REGEX = Regex("""<(?!\s*/?\s*b\s*\d+\s*\d*\s*>)[^>]*>""")

        /** Matches characters that can be translated. */
        private val TRANSLATABLE_TEXT_REGEX = Regex("""[a-zA-Z0-9?!\u00BF\u00A1.,:|\u00C0-\uFFFF]""")

        private const val PLACEHOLDER_TAG_PREFIX = "b"

        private fun escapeText(text: String): String = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    /**
     * Translates the text content of the body of the given [document] in place.
     *
     * @param keepOriginal Whether to keep the original text after translation.
     *   When enabled, the translated text is appended after the original text,
     *   separated by a line break, within the same element structure.
     */
    fun translateDocument(document: Document, keepOriginal: Boolean = false) {
        translateElement(document.body(), keepOriginal)
    }

    /**
     * Translates the text content of the given [element] in place.
     *
     * The element is traversed to collect translatable text segments, which are
     * then passed to the [translate] function to be translated. The returned
     * translations are written back to the original text nodes in order.
     *
     * @param element the root element whose text content is to be translated.
     * @param keepOriginal Whether to keep the original text after translation.
     *   When enabled, the translated text is appended after the original text,
     *   separated by a line break, within the same element structure.
     */
    fun translateElement(element: Element, keepOriginal: Boolean = false) {
        val units = collectUnits(element)
        if (units.isEmpty()) {
            return
        }

        val texts = units.map { it.serializedText }
        val translations = translate(texts)
        units.forEachIndexed { index, unit ->
            unit.applyTranslation(translations.getOrNull(index).orEmpty(), keepOriginal)
        }
    }

    private fun collectUnits(root: Element): List<TranslatableUnit> {
        val units = mutableListOf<TranslatableUnit>()
        collectFromNodeList(root, units)
        return units
    }

    private fun collectFromNodeList(parent: Element, units: MutableList<TranslatableUnit>) {
        val nodes = parent.childNodes()
        var index = 0
        while (index < nodes.size) {
            val node = nodes[index]
            if (isTranslatableNode(node)) {
                val end = collectTranslatableSegmentEnd(nodes, index)
                if (hasTranslatableText(nodes, index, end)) {
                    val out = StringBuilder()
                    serializeNodeList(nodes.subList(index, end), 1, out)
                    units.add(TranslatableUnit(nodes.subList(index, end).toList(), out.toString()))
                }
                index = end
            } else if (node is Element && !node.shouldSkipTranslation()) {
                collectFromNodeList(node, units)
                index++
            } else {
                index++
            }
        }
    }

    private fun isTranslatableNode(node: Node): Boolean = when (node) {
        is TextNode -> true
        is Element -> !node.shouldSkipTranslation() && isTranslatableLeafElement(node)
        else -> false
    }

    private fun isTranslatableLeafElement(element: Element): Boolean {
        return element.tagName().lowercase() in INLINE_TAGS && element.children().all { child ->
            !child.shouldSkipTranslation() && isTranslatableLeafElement(child)
        }
    }

    private fun collectTranslatableSegmentEnd(nodes: List<Node>, start: Int): Int {
        var end = start
        while (end < nodes.size && isTranslatableNode(nodes[end])) {
            end++
        }
        return end
    }

    private fun hasTranslatableText(nodes: List<Node>, start: Int, end: Int): Boolean {
        return (start until end).any { index ->
            when (val node = nodes[index]) {
                is TextNode -> node.text().contains(TRANSLATABLE_TEXT_REGEX)
                is Element -> node.tagName().lowercase() !in PROTECTED_INLINE_TAGS &&
                        hasTranslatableText(node.childNodes(), 0, node.childNodes().size)

                else -> false
            }
        }
    }

    private fun Element.shouldSkipTranslation(): Boolean {
        if (tagName().lowercase() in SKIP_TAGS) {
            return true
        }
        if (attr("translate").equals("no", ignoreCase = true)) {
            return true
        }

        val classes = className().split(Regex("\\s+")).map { it.lowercase() }
        return classes.any { it == "notranslate" || it == "skiptranslate" }
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
     * Writes the translated text of [translatedHtml] back to the given [nodes].
     *
     * @return `true` if the translation was applied; `false` if [translatedHtml]
     *   could not be parsed and the original nodes were left untouched.
     */
    private fun denormalize(nodes: List<Node>, translatedHtml: String): Boolean {
        val sanitized = escapeNonPlaceholderTags(translatedHtml)
        val normalized = sanitized.replace(PLACEHOLDER_TAG_REGEX) { match ->
            val groups = match.groupValues
            "<${groups[1]}$PLACEHOLDER_TAG_PREFIX${groups[2]}${groups[3]}>"
        }

        val translatedNodes = Jsoup.parseBodyFragment(normalized).body().childNodes().toList()
        if (translatedNodes.isEmpty()) {
            return false
        }

        denormalizeNodeList(nodes, translatedNodes)
        return true
    }

    private fun denormalizeNodeList(nodes: List<Node>, translatedNodes: List<Node>) {
        val originalElements = nodes.filterIsInstance<Element>()
        val originalTextNodes = nodes.filterIsInstance<TextNode>().toMutableList()
        var textIndex = 0

        for (child in translatedNodes) {
            when (child) {
                is TextNode -> {
                    val target = originalTextNodes.getOrNull(textIndex)
                    if (target != null) {
                        target.text(child.text())
                        textIndex++
                    }
                }

                is Element -> {
                    val placeholder = parsePlaceholderTag(child.tagName()) ?: continue
                    val original = originalElements.getOrNull(placeholder.second) ?: continue
                    if (original.tagName().lowercase() !in PROTECTED_INLINE_TAGS) {
                        denormalizeNodeList(original.childNodes(), child.childNodes())
                    }
                }
            }
        }
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

    private inner class TranslatableUnit(
        /**
         * A snapshot of the translatable segment nodes taken when the unit is
         * collected. Node references stay valid even after other units insert
         * nodes into the same parent, unlike index-based ranges.
         */
        private val nodes: List<Node>,
        val serializedText: String
    ) {

        fun applyTranslation(translatedText: String, keepOriginal: Boolean) {
            if (translatedText.isBlank() || translatedText == serializedText) {
                return
            }

            if (keepOriginal) {
                val translatedNodes = nodes.map { it.clone() }
                if (denormalize(translatedNodes, translatedText)) {
                    appendTranslation(translatedNodes)
                }
            } else {
                denormalize(nodes, translatedText)
            }
        }

        private fun appendTranslation(translatedNodes: List<Node>) {
            val newNodes = ArrayList<Node>(translatedNodes.size + 1)
            newNodes.add(Element("br"))
            newNodes.addAll(translatedNodes)

            var anchor: Node = nodes.last()
            for (node in newNodes) {
                anchor.after(node)
                anchor = node
            }
        }
    }
}
