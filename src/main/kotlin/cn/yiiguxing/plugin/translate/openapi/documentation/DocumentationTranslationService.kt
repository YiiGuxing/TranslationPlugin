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
        if (Documentations.isTranslated(documentation, false)) {
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
     * @param copyBeforeTranslate If `true`, clones the document before translation.
     * @return The translated [Document], or the original if already translated.
     *
     * @see DocumentationElementFilter
     */
    @RequiresBackgroundThread
    fun translate(
        documentation: Document,
        language: Language? = null,
        copyBeforeTranslate: Boolean = false
    ): Document {
        if (Documentations.isTranslated(documentation)) {
            return documentation
        }

        val docToTranslate = if (copyBeforeTranslate) documentation.clone() else documentation
        return TranslateService.getInstance().translator.translateDocumentation(docToTranslate, language)
    }
}