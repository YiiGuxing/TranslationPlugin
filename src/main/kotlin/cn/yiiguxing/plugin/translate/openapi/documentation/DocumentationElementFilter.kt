package cn.yiiguxing.plugin.translate.openapi.documentation

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import org.jsoup.nodes.Element

/**
 * Filters specific elements in code documentation for translation purposes.
 * Implementations can ignore certain elements during translation and
 * restore them afterward.
 */
interface DocumentationElementFilter {

    /**
     * Filters out specific elements from the [body] of the documentation
     * that should be ignored during translation.
     */
    fun filterElements(body: Element): List<Element> = emptyList()

    /**
     * Restores the previously filtered [elements] back into the [body]
     * of the documentation after translation.
     */
    fun restoreElements(body: Element, elements: List<Element>) = Unit

    companion object {
        private val DEFAULT = object : DocumentationElementFilter {}

        private val EP_NAME = LanguageExtension<DocumentationElementFilter>(
            "cn.yiiguxing.plugin.translate.documentation.elementFilter",
            DEFAULT
        )

        /**
         * Returns the [DocumentationElementFilter] for the specified [language].
         */
        fun forLanguage(language: Language): DocumentationElementFilter = EP_NAME.forLanguage(language)
    }
}