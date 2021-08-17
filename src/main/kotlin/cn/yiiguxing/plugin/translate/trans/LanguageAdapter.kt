package cn.yiiguxing.plugin.translate.trans

import kotlinx.collections.immutable.toImmutableMap

/**
 * Interface for language adapter.
 */
interface LanguageAdapter {

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
        getAdaptedLanguages().toImmutableMap()
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