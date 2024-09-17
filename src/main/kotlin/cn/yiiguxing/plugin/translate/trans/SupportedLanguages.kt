package cn.yiiguxing.plugin.translate.trans

/**
 * Supported languages.
 */
interface SupportedLanguages {
    /**
     * The supported source languages.
     */
    val sourceLanguages: List<Lang>

    /**
     * The supported target languages.
     */
    val targetLanguages: List<Lang>
}

data class SupportedLanguagesData(
    override val sourceLanguages: List<Lang>,
    override val targetLanguages: List<Lang>
) : SupportedLanguages