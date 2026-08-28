package cn.yiiguxing.plugin.translate.trans.documentation

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Translates the text content of an HTML document while keeping its structure,
 * styles and attributes (such as `href` or `src`) intact.
 *
 * This is a generic helper that can be used by any translator: it extracts
 * translatable leaf elements from the document, serializes them according to
 * the given [strategy] and writes the translations back to the original
 * document.
 *
 * @param strategy determines how translatable leaf elements are detected, how
 *   translatable segments are serialized for the translation service and how
 *   translations are applied back. See [HtmlTranslationStrategy] and its
 *   implementations: [PlaceholderHtmlTranslationStrategy],
 *   [RawHtmlTranslationStrategy], [PlainTextHtmlTranslationStrategy] and
 *   [LeafPlainTextHtmlTranslationStrategy].
 * @param translate the function used to translate the collected texts; the
 *   returned list must have the same size and order as the input list.
 *   Blank entries at any position leave the corresponding original text
 *   untouched.
 */
class HtmlDocumentationTranslator(
    private val strategy: HtmlTranslationStrategy,
    private val translate: (List<String>) -> List<String>
) {

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
                val end = if (strategy.mergeAdjacentTranslatableNodes) {
                    collectTranslatableSegmentEnd(nodes, index)
                } else {
                    index + 1
                }
                if (hasTranslatableText(nodes, index, end)) {
                    val segment = nodes.subList(index, end).toList()
                    units.add(TranslatableUnit(segment, strategy.serialize(segment)))
                }
                index = end
            } else if (node is Element && strategy.shouldTraverseElement(node)) {
                collectFromNodeList(node, units)
                index++
            } else {
                index++
            }
        }
    }

    private fun isTranslatableNode(node: Node): Boolean = when (node) {
        is TextNode -> true
        is Element -> !node.shouldSkipTranslation() && strategy.isTranslatableLeafElement(node)
        else -> false
    }

    private fun collectTranslatableSegmentEnd(nodes: List<Node>, start: Int): Int {
        var end = start
        while (end < nodes.size && isTranslatableNode(nodes[end])) {
            end++
        }
        return end
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

            val rebuiltNodes = strategy.deserialize(nodes, translatedText) ?: return

            if (keepOriginal) {
                appendTranslation(rebuiltNodes)
            } else {
                replaceNodes(nodes, rebuiltNodes)
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

        private fun replaceNodes(originalNodes: List<Node>, newNodes: List<Node>) {
            if (originalNodes.isEmpty() || newNodes.isEmpty()) {
                return
            }

            val anchor = originalNodes.first()
            for (node in newNodes) {
                anchor.before(node)
            }
            originalNodes.forEach { it.remove() }
        }
    }
}
