package cn.yiiguxing.plugin.translate.trans

import javax.swing.Icon

/**
 * Translator
 */
interface Translator : TextTranslator {

    val id: String

    val name: String

    val icon: Icon

    val primaryLanguage: Lang

    val supportedSourceLanguages: List<Lang>

    val supportedTargetLanguages: List<Lang>

    @Deprecated("""Use "RateLimiter" in the "translate" implementation.""")
    val intervalLimit: Int

    fun checkConfiguration(force: Boolean = false): Boolean = true

    /**
     * Returns a token identifying the current state of this translator's
     * configuration which may affect translation results for the given [cacheType].
     * Cached translation results are only reused when the token recorded at cache
     * creation time matches the current token.
     *
     * Returns `null` if the translator has no configuration-dependent
     * translation behavior (default).
     */
    fun translationCacheToken(cacheType: TranslationCacheType): String? = null

    val defaultLangForLocale: Lang

}

/**
 * The type of translation cache.
 */
enum class TranslationCacheType {

    /**
     * Cache for text translation.
     */
    TEXT,

    /**
     * Cache for documentation translation.
     */
    DOCUMENTATION
}