package cn.yiiguxing.plugin.translate.provider

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import org.jsoup.nodes.Element

interface IgnoredDocumentationElementProvider {

    fun ignoreElements(body: Element): List<Element> = emptyList()

    fun restoreIgnoredElements(body: Element, ignoredElements: List<Element>) = Unit

    companion object {
        private val DEFAULT_PROVIDER = object : IgnoredDocumentationElementProvider {}

        private val PROVIDERS = LanguageExtension<IgnoredDocumentationElementProvider>(
            "cn.yiiguxing.plugin.translate.ignoredDocumentationElementProvider",
            DEFAULT_PROVIDER
        )

        fun forLanguage(language: Language): IgnoredDocumentationElementProvider = PROVIDERS.forLanguage(language)
    }
}