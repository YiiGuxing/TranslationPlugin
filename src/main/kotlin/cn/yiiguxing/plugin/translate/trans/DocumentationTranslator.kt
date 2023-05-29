package cn.yiiguxing.plugin.translate.trans

import org.jsoup.nodes.Document

interface DocumentationTranslator {

    fun translateDocumentation(documentation: Document, srcLang: Lang, targetLang: Lang): Document

}


/**
 * Translate document body HTML by the specified function [translate].
 */
inline fun Document.translateBody(translate: (bodyHTML: String) -> String): Document = apply {
    val body = body()
    val bodyHTML = body.html().trim()
    if (bodyHTML.isNotEmpty()) {
        body.html(translate(bodyHTML))
    }
}