package cn.yiiguxing.plugin.translate.documentation.filter

import cn.yiiguxing.plugin.translate.openapi.documentation.DocumentationElementFilter
import org.jsoup.nodes.Element

class PhpDocumentationElementFilter : DocumentationElementFilter {

    override fun filterElements(body: Element): List<Element> {
        val ignoredElements = body.select(CSS_QUERY_SOURCE)

        ignoredElements.forEachIndexed { index, element ->
            element.replaceWith(Element("span").attr("id", "php-ignored-$index"))
        }

        return ignoredElements
    }

    override fun restoreElements(body: Element, elements: List<Element>) {
        elements.forEachIndexed { index, element ->
            body.selectFirst("""table.sections td > span[id="php-ignored-$index"]""")?.replaceWith(element)
        }
    }

    companion object {
        private const val CSS_QUERY_SOURCE = "table.sections td > span[path]"
    }

}