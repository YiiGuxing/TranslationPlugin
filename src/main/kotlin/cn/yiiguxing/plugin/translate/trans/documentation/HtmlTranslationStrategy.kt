package cn.yiiguxing.plugin.translate.trans.documentation

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Strategy for translating the text content of an HTML document.
 *
 * [HtmlDocumentationTranslator] uses this strategy to decide which elements are
 * translatable, how a translatable segment is serialized for the translation
 * service and how the translation result is applied back to the document.
 *
 * Implementations:
 * - [PlaceholderHtmlTranslationStrategy]: for translation services that can
 *   recognize basic tags but not tags with attributes.
 * - [RawHtmlTranslationStrategy]: for translation services that can recognize
 *   HTML tags in any form.
 * - [PlainTextHtmlTranslationStrategy]: for translation services that cannot
 *   recognize any tags.
 */
interface HtmlTranslationStrategy {

    /**
     * Whether consecutive translatable nodes should be merged into a single
     * translation unit.
     *
     * When `true`, consecutive translatable nodes (text nodes and translatable
     * leaf elements) are translated together as a single unit, preserving the
     * sentence context. When `false`, each translatable node is translated as a
     * separate unit.
     */
    val mergeAdjacentTranslatableNodes: Boolean

    /**
     * Checks if the given [element] is a translatable leaf element.
     *
     * A translatable leaf element is the smallest element whose entire content
     * can be passed to the translation service. Elements that are not
     * translatable leaf elements are traversed recursively.
     */
    fun isTranslatableLeafElement(element: Element): Boolean

    /**
     * Checks if the given [element] should be traversed. Elements for which this
     * returns `false` are skipped along with all their content.
     */
    fun shouldTraverseElement(element: Element): Boolean = !element.shouldSkipTranslation()

    /**
     * Serializes the given [nodes] (a translatable segment) into the text that
     * is passed to the translation service.
     */
    fun serialize(nodes: List<Node>): String

    /**
     * Deserializes the [translatedText] returned by the translation service into
     * nodes that replace the [originalNodes], or returns `null` if the
     * translation could not be applied, in which case the original nodes are
     * left untouched.
     */
    fun deserialize(originalNodes: List<Node>, translatedText: String): List<Node>?
}

/** Tags whose contents should not be translated. */
internal val SKIP_TAGS: Set<String> = setOf(
    "head", "script", "style", "pre",
    "object", "address", "noscript", "embed", "map", "area",
    "hr", "input", "title", "br", "frame", "iframe", "textarea",
    "select", "option", "optgroup", "button",
    "img", "picture", "audio", "video", "source", "track", "canvas",
    "svg", "math", "meta", "link", "base", "param", "wbr"
)

/** Inline tags. An element is a translatable leaf if all its element descendants are inline. */
internal val INLINE_TAGS: Set<String> = setOf(
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
internal val PROTECTED_INLINE_TAGS: Set<String> = setOf("code", "samp", "kbd", "var")

/** Matches characters that can be translated. */
private val TRANSLATABLE_TEXT_REGEX = Regex("""[a-zA-Z0-9?!\u00BF\u00A1.,:|\u00C0-\uFFFF]""")

internal fun escapeText(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

internal fun Element.shouldSkipTranslation(): Boolean {
    if (tagName().lowercase() in SKIP_TAGS) {
        return true
    }
    if (attr("translate").equals("no", ignoreCase = true)) {
        return true
    }

    val classes = className().split(Regex("\\s+")).map { it.lowercase() }
    return classes.any {
        it == "notranslate" || it == "skiptranslate"
    }
}

internal fun hasTranslatableText(nodes: List<Node>, start: Int, end: Int): Boolean {
    return (start until end).any { index ->
        when (val node = nodes[index]) {
            is TextNode -> node.text().contains(TRANSLATABLE_TEXT_REGEX)
            is Element -> node.tagName().lowercase() !in PROTECTED_INLINE_TAGS &&
                    hasTranslatableText(node.childNodes(), 0, node.childNodes().size)

            else -> false
        }
    }
}

/**
 * Checks if [element] is a translatable inline leaf element: an inline tag
 * whose element descendants are all inline.
 */
internal fun isTranslatableInlineLeafElement(element: Element): Boolean {
    return element.tagName().lowercase() in INLINE_TAGS && element.children().all { child ->
        !child.shouldSkipTranslation() && isTranslatableInlineLeafElement(child)
    }
}
