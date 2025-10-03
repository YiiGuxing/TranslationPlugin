package cn.yiiguxing.plugin.translate.documentation.provider

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import org.jsoup.nodes.Element

interface IgnoredDocumentationElementProvider {

    fun ignoreElements(body: Element): List<Element> = emptyList()

    fun restoreIgnoredElements(body: Element, ignoredElements: List<Element>) = Unit

    companion object {
        private val DEFAULT_PROVIDER = object : IgnoredDocumentationElementProvider {}

        private val EP_NAME = LanguageExtension<IgnoredDocumentationElementProvider>(
            "cn.yiiguxing.plugin.translate.documentation.ignoredElementProvider",
            DEFAULT_PROVIDER
        )

        fun forLanguage(language: Language): IgnoredDocumentationElementProvider = EP_NAME.forLanguage(language)
    }
}