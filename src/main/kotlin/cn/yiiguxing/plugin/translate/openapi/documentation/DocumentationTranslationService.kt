package cn.yiiguxing.plugin.translate.openapi.documentation

import cn.yiiguxing.plugin.translate.documentation.Documentations
import cn.yiiguxing.plugin.translate.documentation.translateDocumentation
import cn.yiiguxing.plugin.translate.trans.TranslateService
import com.intellij.lang.Language
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.jsoup.nodes.Document

/**
 * Provides services for translating code documentation to the main language.
 */
@Service
class DocumentationTranslationService private constructor() {

    companion object {
        /**
         * Returns the instance of [DocumentationTranslationService].
         */
        @JvmStatic
        fun getInstance(): DocumentationTranslationService = service()
    }

    /**
     * Checks whether the specified [documentation] is translated.
     *
     * @param parse Whether to parse the [documentation] string into a [Document] before checking.
     */
    fun isTranslated(documentation: String, parse: Boolean = false): Boolean {
        return Documentations.isTranslated(documentation, parse)
    }

    /**
     * Checks whether the specified [documentation] is translated.
     */
    fun isTranslated(documentation: Document): Boolean {
        return Documentations.isTranslated(documentation)
    }

    /**
     * Returns the translation provider used for the specified [documentation],
     * or `null` if the [documentation] is not translated or the provider is unset.
     */
    fun getTranslationProvider(documentation: Document): String? {
        return Documentations.getTranslationProvider(documentation)
    }

    /**
     * Translates the specified [documentation] to the main language.
     *
     * @param documentation The documentation content to translate, in HTML format.
     * @param language The programming language of the documentation.
     *   Used to get [DocumentationElementFilter] for language-specific element ignoring.
     *   If `null`, no ignoring is performed.
     * @return The translated documentation string, or the original if already translated.
     *
     * @see DocumentationElementFilter
     */
    @RequiresBackgroundThread
    fun translate(documentation: String, language: Language? = null): String {
        if (isTranslated(documentation, false)) {
            return documentation
        }

        return TranslateService.getInstance().translator.translateDocumentation(documentation, language)
    }

    /**
     * Translates the specified [documentation] to the main language.
     *
     * @param documentation The documentation in [Document] format.
     * @param language The programming language of the documentation.
     *   Used to get [DocumentationElementFilter] for language-specific element ignoring.
     *   If `null`, no ignoring is performed.
     *
     * @see DocumentationElementFilter
     */
    @RequiresBackgroundThread
    fun translate(documentation: Document, language: Language? = null) {
        if (isTranslated(documentation)) {
            return
        }

        TranslateService.getInstance().translator.translateDocumentation(documentation, language)
    }

    /**
     * Translates the specified [documentation] to the main language and returns a new [Document].
     *
     * @param documentation The documentation in [Document] format.
     * @param language The programming language of the documentation.
     *   Used to get [DocumentationElementFilter] for language-specific element ignoring.
     *   If `null`, no ignoring is performed.
     * @return A new [Document] containing the translated documentation,
     *
     * @see DocumentationElementFilter
     */
    @RequiresBackgroundThread
    fun getTranslatedDocumentation(documentation: Document, language: Language? = null): Document {
        return documentation.clone().also { translate(it, language) }
    }
}