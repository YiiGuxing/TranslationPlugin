package cn.yiiguxing.plugin.translate.provider

import org.jsoup.nodes.Element

class JavaScriptIgnoredDocumentationElementProvider : IgnoredDocumentationElementProvider {

    override fun ignoreElements(body: Element): List<Element> {
        val ignoredElement = body.selectFirst(CSS_QUERY)
            ?.parent()
            ?: return emptyList()

        ignoredElement.replaceWith(Element("td").attr("id", "js-ignored"))

        return listOf(ignoredElement)
    }

    override fun restoreIgnoredElements(body: Element, ignoredElements: List<Element>) {
        val element = ignoredElements.firstOrNull() ?: return
        body.selectFirst(RESTORE_CSS_QUERY)?.replaceWith(element)
    }

    companion object {
        private const val CSS_QUERY = """table.sections td > icon[src*="FileTypes"]"""
        private const val RESTORE_CSS_QUERY = """table.sections td[id="js-ignored"]"""
    }

}