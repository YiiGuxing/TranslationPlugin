package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.util.Plugin
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import org.jsoup.nodes.Element

interface IgnoredDocumentationElementsProvider {

    fun ignoreElements(body: Element): List<Element> = emptyList()

    fun restoreIgnoredElements(body: Element, ignoredElements: List<Element>) = Unit

    companion object {
        private val DEFAULT_PROVIDER = object : IgnoredDocumentationElementsProvider {}

        private val PROVIDERS = LanguageExtension<IgnoredDocumentationElementsProvider>(
            "${Plugin.PLUGIN_ID}.ignoredDocumentationElementsProvider",
            DEFAULT_PROVIDER
        )

        fun forLanguage(language: Language): IgnoredDocumentationElementsProvider = PROVIDERS.forLanguage(language)
    }
}