package cn.yiiguxing.plugin.translate.trans

/**
 * Interface for language adapter.
 */
interface LanguageAdapter {

    /**
     * List of supported source languages.
     */
    val supportedSourceLanguages: List<Lang>

    /**
     * List of supported target languages.
     */
    val supportedTargetLanguages: List<Lang>

    /**
     * Returns the [language][Lang] corresponding to the given [code].
     */
    fun getLanguage(code: String): Lang = Lang[code]

    /**
     * Returns the code corresponding to the given [language][lang].
     */
    fun getLanguageCode(lang: Lang): String = lang.code

}


abstract class BaseLanguageAdapter : LanguageAdapter {

    private val codeMapping: Map<String, Lang> by lazy {
        getAdaptedLanguages()
    }

    private val langMapping: Map<Lang, String> by lazy {
        codeMapping.map { (code, lang) -> lang to code }.toMap()
    }

    /**
     * Returns an adapted languages map (`languageCode` - [Lang]).
     */
    protected abstract fun getAdaptedLanguages(): Map<String, Lang>

    final override fun getLanguage(code: String): Lang {
        return codeMapping[code] ?: super.getLanguage(code)
    }

    final override fun getLanguageCode(lang: Lang): String {
        return langMapping[lang] ?: super.getLanguageCode(lang)
    }

}