package cn.yiiguxing.plugin.translate.openapi.documentation

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import org.jsoup.nodes.Element

/**
 * Provider for ignoring certain elements in documentation during translation.
 */
interface IgnoredDocumentationElementProvider {

    /**
     * Ignores specific elements within the given [body] of documentation.
     */
    fun ignoreElements(body: Element): List<Element> = emptyList()

    /**
     * Restores previously ignored elements back into the [body] of the translated documentation.
     */
    fun restoreIgnoredElements(body: Element, ignoredElements: List<Element>) = Unit

    companion object {
        private val DEFAULT_PROVIDER = object : IgnoredDocumentationElementProvider {}

        private val EP_NAME = LanguageExtension<IgnoredDocumentationElementProvider>(
            "cn.yiiguxing.plugin.translate.documentation.ignoredElementProvider",
            DEFAULT_PROVIDER
        )

        /**
         * Returns the [IgnoredDocumentationElementProvider] for the specified [language].
         */
        fun forLanguage(language: Language): IgnoredDocumentationElementProvider = EP_NAME.forLanguage(language)
    }
}