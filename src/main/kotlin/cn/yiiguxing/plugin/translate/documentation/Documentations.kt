package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.provider.IgnoredDocumentationElementProvider
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.scaled
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.lang.Language
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.JBUI
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
        return documentation.outerHtml().fixHtml()
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


private const val CSS_QUERY_DEFINITION = ".definition"
private const val CSS_QUERY_CONTENT = ".content"

private const val TAG_PRE = "pre"
private const val ATTR_TRANSLATED = "translated"

private const val FIX_HTML_CLASS_EXPRESSION_REPLACEMENT = "<${'$'}{tag} class='${'$'}{class}'>"
private val fixHtmlClassExpressionRegex = Regex("""<(?<tag>.+?) class="(?<class>.+?)">""")


/**
 * Documentation string of this [Document].
 */
internal val Document.documentationString: String
    get() = Documentations.getDocumentationString(this, false)


internal fun Translator.getTranslatedDocumentation(documentation: String, language: Language?): String {
    val document: Document = Documentations.parseDocumentation(documentation)
    if (document.body().hasAttr(ATTR_TRANSLATED)) {
        return documentation
    }

    val translatedDocumentation = try {
        if (this is DocumentationTranslator) {
            getTranslatedDocumentation(document, language)
        } else {
            getTranslatedDocumentation(document)
        }
    } catch (e: ContentLengthLimitException) {
        document.addLimitHint()
    } catch (e: TranslateException) {
        if (e.cause is ContentLengthLimitException) {
            document.addLimitHint()
        } else {
            throw e
        }
    }

    translatedDocumentation.body().attributes().put(ATTR_TRANSLATED, true)

    return translatedDocumentation.documentationString
}

private fun Document.addLimitHint(): Document {
    val hintColor = JBUI.CurrentTheme.Label.disabledForeground()
    return addMessage(message("translate.documentation.limitHint"), hintColor)
}

private fun Document.addMessage(message: String, color: Color): Document = apply {
    val colorHex = ColorUtil.toHex(color)
    val contentEl = body().selectFirst(CSS_QUERY_CONTENT) ?: return@apply
    val messageEl = contentEl.prependElement("div")
        .attr("style", "color: $colorHex; margin: ${3.scaled}px 0px;")
    messageEl.appendElement("icon")
        .attr("src", "AllIcons.General.Information")
    messageEl.append("&nbsp;").appendText(message)
}

/**
 * 修复HTML格式。[DocumentationComponent]识别不了 `class="class"` 的表达形式，
 * 只识别 `class='class'`，导致样式显示异常。
 */
private fun String.fixHtml(): String = replace(fixHtmlClassExpressionRegex, FIX_HTML_CLASS_EXPRESSION_REPLACEMENT)

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

    val preElements = body.select(TAG_PRE)
    preElements.forEachIndexed { index, element ->
        element.replaceWith(Element(TAG_PRE).attr("id", index.toString()))
    }

    val ignoredElementProvider = language?.let { IgnoredDocumentationElementProvider.forLanguage(it) }
    val ignoredElements = ignoredElementProvider?.ignoreElements(body)

    val translatedDocument = translateDocumentation(document, Lang.AUTO, (this as Translator).primaryLanguage)
    val translatedBody = translatedDocument.body()

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
    val formatted = try {
        val content = htmlDocument.getText(0, htmlDocument.length).trim()
        checkContentLength(content, contentLengthLimit)
    } catch (e: ContentLengthLimitException) {
        definition?.let { body.insertChildren(0, it) }
        throw e
    }
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
