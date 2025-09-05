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
     * @param documentation The documentation content to translate, it should be in HTML format.
     * @param language The programming language of the documentation. If null, the language will be auto-detected.
     * @return The translated documentation string. If the documentation is already translated, return the original.
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
     * @param documentation The documentation in [Document] format to translate.
     * @param language The programming language of the documentation. If `null`, the language will be auto-detected.
     * @param copyBeforeTranslate If `true`, clones the document before translation to avoid modifying the original.
     * @return The translated [Document]. If the document is already translated, return the original.
     *   If [copyBeforeTranslate] is `false`, the original document may be modified and returned.
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