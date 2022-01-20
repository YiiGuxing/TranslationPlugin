package cn.yiiguxing.plugin.translate.trans

import org.jsoup.nodes.Document

interface DocumentationTranslator {

    fun translateDocumentation(documentation: Document, srcLang: Lang, targetLang: Lang): Document

}
