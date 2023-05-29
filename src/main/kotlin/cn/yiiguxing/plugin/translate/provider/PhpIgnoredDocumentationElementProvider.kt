package cn.yiiguxing.plugin.translate.provider

import org.jsoup.nodes.Element

class PhpIgnoredDocumentationElementProvider : IgnoredDocumentationElementProvider {

    override fun ignoreElements(body: Element): List<Element> {
        val ignoredElements = body.select(CSS_QUERY_SOURCE) ?: return emptyList()

        ignoredElements.forEachIndexed { index, element ->
            element.replaceWith(Element("span").attr("id", "php-ignored-$index"))
        }

        return ignoredElements
    }

    override fun restoreIgnoredElements(body: Element, ignoredElements: List<Element>) {
        ignoredElements.forEachIndexed { index, element ->
            body.selectFirst("""table.sections td > span[id="php-ignored-$index"]""")?.replaceWith(element)
        }
    }

    companion object {
        private const val CSS_QUERY_SOURCE = "table.sections td > span[path]"
    }

}