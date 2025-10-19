package cn.yiiguxing.plugin.translate.documentation.filter

import cn.yiiguxing.plugin.translate.openapi.documentation.DocumentationElementFilter
import org.jsoup.nodes.Element

class JavaScriptDocumentationElementFilter : DocumentationElementFilter {

    override fun filterElements(body: Element): List<Element> {
        val ignoredElement = body.selectFirst(CSS_QUERY)
            ?.parent()
            ?: return emptyList()

        ignoredElement.replaceWith(Element("td").attr("id", "js-ignored"))

        return listOf(ignoredElement)
    }

    override fun restoreElements(body: Element, elements: List<Element>) {
        val element = elements.firstOrNull() ?: return
        body.selectFirst(RESTORE_CSS_QUERY)?.replaceWith(element)
    }

    companion object {
        private const val CSS_QUERY = """table.sections td > icon[src*="FileTypes"]"""
        private const val RESTORE_CSS_QUERY = """table.sections td[id="js-ignored"]"""
    }

}