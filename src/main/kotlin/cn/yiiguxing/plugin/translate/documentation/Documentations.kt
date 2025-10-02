package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.openapi.documentation.IgnoredDocumentationElementProvider
import cn.yiiguxing.plugin.translate.trans.DocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.ui.scaled
import com.intellij.lang.Language
import com.intellij.ui.ColorUtil
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.awt.Color
import java.io.StringReader
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit

/**
 * Help class that provide document operations.
 */
internal object Documentations {

    /**
     * Checks whether the specified [documentation] is translated.
     *
     * @param parse Whether to parse the [documentation] string into a [Document] before checking.
     */
    fun isTranslated(documentation: String, parse: Boolean = false): Boolean {
        return if (parse) {
            isTranslated(parseDocumentation(documentation))
        } else {
            documentation.startsWith("<html $ATTR_TRANSLATED>", ignoreCase = true) ||
                    TRANSLATED_DOCUMENTATION_REGEX.containsMatchIn(documentation)
        }
    }

    /**
     * Checks whether the specified [documentation] is translated.
     */
    fun isTranslated(documentation: Document): Boolean {
        return documentation.htmlEl?.hasAttr(ATTR_TRANSLATED) ?: false
    }

    /**
     * Parses the specified [documentation] string.
     */
    fun parseDocumentation(documentation: String): Document = Jsoup.parse(documentation)

    /**
     * Returns the documentation string of the specified [documentation] object.
     *
     * @param prettyPrint Enable or disable pretty printing.
     */
    fun getDocumentationString(documentation: Document, prettyPrint: Boolean = false): String {
        documentation.outputSettings().prettyPrint(prettyPrint)
        return documentation.html()
    }

    /**
     * Adds the specified inline [message] to the [documentation].
     */
    fun addMessage(documentation: String, message: String, color: Color): String {
        return parseDocumentation(documentation)
            .addMessage(message, color)
            .documentationString
    }

}


internal const val CSS_QUERY_DEFINITION = ".definition"
internal const val CSS_QUERY_CONTENT = ".content"
internal const val CSS_QUERY_BOTTOM = ".bottom"

private const val ID_BOTTOM = "bottom"

private const val TAG_HTML = "html"
private const val TAG_DIV = "div"
private const val TAG_PRE = "pre"
private const val ATTR_TRANSLATED = "translated"

private val TRANSLATED_DOCUMENTATION_REGEX = Regex(
    """<html.+?$ATTR_TRANSLATED(=("true"|'true'))?.*?>""",
    RegexOption.IGNORE_CASE
)


/**
 * Documentation string of this [Document].
 */
internal val Document.documentationString: String
    get() = Documentations.getDocumentationString(this, false)


internal fun Translator.translateDocumentation(documentation: String, language: Language?): String {
    val document: Document = Documentations.parseDocumentation(documentation)
    val translatedDocumentation = translateDocumentation(document, language)

    return translatedDocumentation.documentationString
}

internal fun Translator.translateDocumentation(documentation: Document, language: Language?): Document {
    if (Documentations.isTranslated(documentation)) {
        return documentation
    }

    return if (this is DocumentationTranslator) {
        getTranslatedDocumentation(documentation, language)
    } else {
        getTranslatedDocumentation(documentation)
    }.also {
        it.htmlEl?.attributes()?.put(ATTR_TRANSLATED, true)
    }
}

private val Document.htmlEl: Element?
    get() = firstElementChild()
        ?.takeIf { it.nodeName().equals(TAG_HTML, true) }
        ?: selectFirst(TAG_HTML)

private fun Document.addMessage(message: String, color: Color): Document = apply {
    val colorHex = ColorUtil.toHtmlColor(color)
    val contentEl = body().selectFirst(CSS_QUERY_CONTENT) ?: return@apply
    val messageEl = contentEl.prependElement("div")
        .attr("style", "color: $colorHex; margin: ${3.scaled}px 0px;")
    messageEl.appendElement("icon")
        .attr("src", "AllIcons.General.Information")
    messageEl.append("&nbsp;").appendText(message)
}

private fun Element.isEmptyParagraph(): Boolean = "p".equals(tagName(), true) && html().isBlank()

private fun DocumentationTranslator.getTranslatedDocumentation(document: Document, language: Language?): Document {
    val body = document.body()
    val definition = body.selectFirst(CSS_QUERY_DEFINITION)
    val definitions = definition
        ?.previousElementSiblings()
        ?.toMutableList()
        ?.apply {
            reverse()
            add(definition)
            forEach { it.remove() }
        }

    // 删除多余的 `p` 标签。
    body.selectFirst(CSS_QUERY_CONTENT)
        ?.nextElementSibling()
        ?.takeIf { it.isEmptyParagraph() }
        ?.remove()

    // Remove the `bottom` section of the documentation and replace it with an empty element
    // so that it can be restored after translation. Whether the `bottom` section of all
    // documentation should be excluded from translation still needs to be confirmed.
    val bottom = body.selectFirst(CSS_QUERY_BOTTOM)
    bottom?.replaceWith(Element(TAG_DIV).attr("id", ID_BOTTOM))

    val preElements = body.select(TAG_PRE)
    preElements.forEachIndexed { index, element ->
        element.replaceWith(Element(TAG_PRE).attr("id", index.toString()))
    }

    val ignoredElementProvider = language?.let { IgnoredDocumentationElementProvider.forLanguage(it) }
    val ignoredElements = ignoredElementProvider?.ignoreElements(document)

    val translatedDocument = translateDocumentation(document, Lang.AUTO, (this as Translator).primaryLanguage)
    val translatedBody = translatedDocument.body()

    bottom?.let { translatedBody.selectFirst("#$ID_BOTTOM")?.replaceWith(it) }
    preElements.forEachIndexed { index, element ->
        translatedBody.selectFirst("""${TAG_PRE}[id="$index"]""")?.replaceWith(element)
    }
    ignoredElements?.let { ignoredElementProvider.restoreIgnoredElements(translatedBody, it) }
    definitions?.let { translatedBody.prependChildren(it) }

    return translatedDocument
}

private fun Translator.getTranslatedDocumentation(document: Document): Document {
    val body = document.body()

    val definition = body.selectFirst(CSS_QUERY_DEFINITION)?.apply { remove() }

    val htmlDocument = HTMLDocument().also { HTMLEditorKit().read(StringReader(body.html()), it, 0) }
    val formatted = htmlDocument.getText(0, htmlDocument.length).trim()
    val translation =
        if (formatted.isEmpty()) ""
        else translate(formatted, Lang.AUTO, primaryLanguage).translation ?: ""

    val newBody = Element("body")
    definition?.let { newBody.appendChild(it) }

    val contentEl = Element("div").addClass("content")
    translation.lines().forEach { contentEl.appendElement("p").appendText(it) }
    newBody.appendChild(contentEl)

    body.replaceWith(newBody)

    return document
}
