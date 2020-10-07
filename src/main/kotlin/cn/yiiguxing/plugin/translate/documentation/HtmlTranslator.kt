package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.provider.IgnoredDocumentationElementsProvider
import cn.yiiguxing.plugin.translate.trans.GoogleTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translator
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.lang.Language
import com.intellij.ui.ColorUtil
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.StringReader
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit

private const val CSS_QUERY_DEFINITION = ".definition"
private const val CSS_QUERY_CONTENT = ".content"
private const val TAG_PRE = "pre"
private const val TAG_I = "i"
private const val TAG_EM = "em"
private const val TAG_B = "b"
private const val TAG_STRONG = "strong"
private const val TAG_SPAN = "span"

private const val TRANSLATED_ATTR = "translated"

private val HTML_HEAD_REGEX = Regex("""<(?<tag>.+?) class="(?<class>.+?)">""")
private const val HTML_HEAD_REPLACEMENT = "<${'$'}{tag} class='${'$'}{class}'>"

private val HTML_KIT = HTMLEditorKit()

private class ContentLengthLimitException : Exception()

fun Translator.getTranslatedDocumentation(documentation: String, language: Language?): String {
    val document: Document = Jsoup.parse(documentation)
    if (document.body().hasAttr(TRANSLATED_ATTR)) {
        return documentation
    }

    val translatedDocumentation =
        try {
            if (this is GoogleTranslator) {
                getTranslatedDocumentation(document, language)
            } else {
                getTranslatedDocumentation(document)
            }
        } catch (e: ContentLengthLimitException) {
            document.addLimitHint()
        }

    translatedDocumentation.body().attributes().put(TRANSLATED_ATTR, null)

    return translatedDocumentation.outerHtml().fixHtml()
}

private fun Document.addLimitHint(): Document {
    val hintColor = ColorUtil.toHex(JBUI.CurrentTheme.Label.disabledForeground())
    body().selectFirst(CSS_QUERY_CONTENT).insertChildren(
        0,
        Element("div")
            .attr(
                "style",
                "color: $hintColor;" +
                        "margin-bottom: ${JBUIScale.scale(4)}px;" +
                        "padding: ${JBUIScale.scale(1)}px ${JBUIScale.scale(6)}px;" +
                        "border-left: ${JBUIScale.scale(3)}px $hintColor solid;"
            )
            .text(message("translate.documentation.limitHint"))
    )
    return this
}

/**
 * 修复HTML格式。[DocumentationComponent]识别不了`attr="val"`的属性表达形式，只识别`attr='val'`的表达形式，导致样式显示异常。
 */
private fun String.fixHtml(): String = replace(
    HTML_HEAD_REGEX,
    HTML_HEAD_REPLACEMENT
)

private fun GoogleTranslator.getTranslatedDocumentation(document: Document, language: Language?): Document {
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

    // 翻译内容会带有原文与译文，分号包在 `i` 标签和 `b` 标签内，因此替换掉这两个标签以免影响到翻译后的处理。
    // 无需检查内容长度，因为谷歌翻好像没有限制
    val content = body.html()
        .replaceTag(TAG_B, TAG_STRONG)
        .replaceTag(TAG_I, TAG_EM)
    val translation =
        if (content.isBlank()) ""
        else translateDocumentation(content, Lang.AUTO, primaryLanguage).translation ?: ""

    body.html(translation)
    // 去除原文标签。
    body.select(TAG_I).remove()
    // 去除译文的粗体效果，`b` 标签替换为 `span` 标签。
    body.select(TAG_B).forEach { it.replaceWith(Element(TAG_SPAN).html(it.html())) }

    preElements.forEachIndexed { index, element ->
        body.selectFirst("""${TAG_PRE}[id="$index"]""").replaceWith(element)
    }
    ignoredElements?.let { ignoredElementsProvider.restoreIgnoredElements(body, it) }

    definition?.let { body.prependChild(it) }

    return document
}

private fun String.replaceTag(targetTag: String, replacementTag: String): String {
    return replace(Regex("<(?<pre>/??)$targetTag(?<pos>( .+?)*?)>"), "<${'$'}{pre}$replacementTag${'$'}{pos}>")
}

private fun Element.isEmptyParagraph(): Boolean = "p".equals(tagName(), true) && html().isBlank()

private fun Translator.checkContentLengthLimit(content: String): String {
    if (contentLengthLimit > 0 && content.length > contentLengthLimit) throw ContentLengthLimitException()
    return content
}

private fun Translator.getTranslatedDocumentation(document: Document): Document {
    val body = document.body()
    val definition = body.selectFirst(CSS_QUERY_DEFINITION)?.apply { remove() }

    val htmlDocument = HTMLDocument().also { HTML_KIT.read(StringReader(body.html()), it, 0) }
    val formatted = try {
        checkContentLengthLimit(htmlDocument.getText(0, htmlDocument.length).trim())
    } catch (e: ContentLengthLimitException) {
        definition?.let { body.insertChildren(0, it) }
        throw e
    }
    val translation =
        if (formatted.isEmpty()) ""
        else translateDocumentation(formatted, Lang.AUTO, primaryLanguage).translation ?: ""

    val newBody = Element("body")
    definition?.let { newBody.appendChild(it) }
    Element("div")
        .addClass("content")
        .append(translation.replace("\n", "<br/>"))
        .let { newBody.appendChild(it) }

    body.replaceWith(newBody)

    return document
}
