package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.provider.IgnoredDocumentationElementsProvider
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.scaled
import cn.yiiguxing.plugin.translate.util.IdeVersion
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.lang.Language
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.JBUI
import icons.TranslationIcons
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.awt.Color
import java.io.StringReader
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit

private const val CSS_QUERY_DEFINITION = ".definition"
private const val CSS_QUERY_CONTENT = ".content"
private const val TAG_PRE = "pre"

private const val TRANSLATED_ATTR = "translated"

private const val FIX_HTML_CLASS_EXPRESSION_REPLACEMENT = "<${'$'}{tag} class='${'$'}{class}'>"
private val fixHtmlClassExpressionRegex = Regex("""<(?<tag>.+?) class="(?<class>.+?)">""")


internal object Documentations {

    /**
     * Adds the specified inline [message] to the [documentation].
     */
    fun addMessage(documentation: String, message: String, color: Color): String {
        return Jsoup.parse(documentation)
            .apply { outputSettings().prettyPrint(false) }
            .addMessage(message, color)
            .outerHtml()
            .fixHtml()
    }
}

private fun Document.addMessage(message: String, color: Color): Document = apply {
    val colorHex = ColorUtil.toHex(color)
    val contentEl = body().selectFirst(CSS_QUERY_CONTENT) ?: return@apply

    val trEl = Element("tr")
        .attr("valign", "middle")
        .attr("style", "color: $colorHex; border-left: ${2.scaled}px $colorHex solid;")

    var hasIcon = false
    // 在2021.3版本以下图标显示会有问题
    if (IdeVersion >= IdeVersion.IDE2021_3) {
        TranslationIcons.translationIconUrl?.let { iconUrl ->
            hasIcon = true
            val iconEl = Element("img").attr("src", iconUrl)
            trEl.appendChild(
                Element("td")
                    .attr("style", "margin: 0px ${2.scaled}px 0px ${5.scaled}px;")
                    .appendChild(iconEl)
            )
        }
    }
    trEl.appendChild(
        Element("td")
            .attr("style", "margin: 0px ${if (hasIcon) 0 else 5.scaled}px;")
            .appendText(message)
    )

    val messageTableEl = Element("table")
        .attr("style", "margin-bottom: ${5.scaled}px;")
        .appendChild(Element("tbody").appendChild(trEl))

    contentEl.insertChildren(0, messageTableEl)
}

internal fun Translator.getTranslatedDocumentation(documentation: String, language: Language?): String {
    val document: Document = Jsoup.parse(documentation).apply {
        outputSettings().prettyPrint(false)
    }
    if (document.body().hasAttr(TRANSLATED_ATTR)) {
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

    translatedDocumentation.body().attributes().put(TRANSLATED_ATTR, true)

    return translatedDocumentation.outerHtml().fixHtml()
}

private fun Document.addLimitHint(): Document {
    val hintColor = JBUI.CurrentTheme.Label.disabledForeground()
    return addMessage(message("translate.documentation.limitHint"), hintColor)
}

/**
 * 修复HTML格式。[DocumentationComponent]识别不了 `class="class"` 的表达形式，
 * 只识别 `class='class'`，导致样式显示异常。
 */
private fun String.fixHtml(): String = replace(fixHtmlClassExpressionRegex, FIX_HTML_CLASS_EXPRESSION_REPLACEMENT)

private fun DocumentationTranslator.getTranslatedDocumentation(document: Document, language: Language?): Document {
    val body = document.body()

    val definition = body.selectFirst(CSS_QUERY_DEFINITION)?.apply { remove() }

    // 删除多余的 `p` 标签。
    body.selectFirst(CSS_QUERY_CONTENT)
        ?.nextElementSibling()
        ?.takeIf { it.isEmptyParagraph() }
        ?.remove()

    val preElements = body.select(TAG_PRE)
    preElements.forEachIndexed { index, element ->
        element.replaceWith(Element(TAG_PRE).attr("id", index.toString()))
    }

    val ignoredElementsProvider = language?.let { IgnoredDocumentationElementsProvider.forLanguage(it) }
    val ignoredElements = ignoredElementsProvider?.ignoreElements(body)

    val translatedDocument = translateDocumentation(document, Lang.AUTO, (this as Translator).primaryLanguage)
    val translatedBody = translatedDocument.body()

    preElements.forEachIndexed { index, element ->
        translatedBody.selectFirst("""${TAG_PRE}[id="$index"]""")?.replaceWith(element)
    }
    ignoredElements?.let { ignoredElementsProvider.restoreIgnoredElements(translatedBody, it) }
    definition?.let { translatedBody.prependChild(it) }

    return translatedDocument
}

private fun Element.isEmptyParagraph(): Boolean = "p".equals(tagName(), true) && html().isBlank()


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
    Element("div")
        .addClass("content")
        .append(translation.replace("\n", "<br/>"))
        .let { newBody.appendChild(it) }

    body.replaceWith(newBody)

    return document
}
