package cn.yiiguxing.plugin.translate.trans.deepl

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.SupportedLanguages


/**
 * Supported languages for DeepL Translator.
 */
object DeeplSupportedLanguages : SupportedLanguages {
    /**
     * The supported source languages.
     */
    override val sourceLanguages: List<Lang> = listOf(
        Lang.AUTO,
        Lang.ARABIC,
        Lang.BULGARIAN,
        Lang.CHINESE,
        Lang.CZECH,
        Lang.DANISH,
        Lang.DUTCH,
        Lang.ENGLISH,
        Lang.ESTONIAN,
        Lang.FINNISH,
        Lang.FRENCH,
        Lang.GERMAN,
        Lang.GREEK,
        Lang.HEBREW,
        Lang.HUNGARIAN,
        Lang.INDONESIAN,
        Lang.ITALIAN,
        Lang.JAPANESE,
        Lang.KOREAN,
        Lang.LATVIAN,
        Lang.LITHUANIAN,
        Lang.NORWEGIAN,
        Lang.POLISH,
        Lang.PORTUGUESE,
        Lang.ROMANIAN,
        Lang.RUSSIAN,
        Lang.SLOVAK,
        Lang.SLOVENIAN,
        Lang.SPANISH,
        Lang.SWEDISH,
        Lang.THAI,
        Lang.TURKISH,
        Lang.UKRAINIAN,
        Lang.VIETNAMESE,
    )

    /**
     * The supported target languages.
     */
    override val targetLanguages: List<Lang> = listOf(
        Lang.ARABIC,
        Lang.BULGARIAN,
        Lang.CHINESE_SIMPLIFIED,
        Lang.CHINESE_TRADITIONAL,
        Lang.CZECH,
        Lang.DANISH,
        Lang.DUTCH,
        Lang.ENGLISH,
        Lang.ENGLISH_AMERICAN,
        Lang.ENGLISH_BRITISH,
        Lang.ESTONIAN,
        Lang.FINNISH,
        Lang.FRENCH,
        Lang.GERMAN,
        Lang.GREEK,
        Lang.HEBREW,
        Lang.HUNGARIAN,
        Lang.INDONESIAN,
        Lang.ITALIAN,
        Lang.JAPANESE,
        Lang.KOREAN,
        Lang.LATVIAN,
        Lang.LITHUANIAN,
        Lang.NORWEGIAN,
        Lang.POLISH,
        Lang.PORTUGUESE,
        Lang.PORTUGUESE_BRAZILIAN,
        Lang.ROMANIAN,
        Lang.RUSSIAN,
        Lang.SLOVAK,
        Lang.SLOVENIAN,
        Lang.SPANISH,
        Lang.SWEDISH,
        Lang.THAI,
        Lang.TURKISH,
        Lang.UKRAINIAN,
        Lang.VIETNAMESE,
    )
}

private val adapter = object : BaseLanguageAdapter() {
    override fun getAdaptedLanguages(): Map<String, Lang> = mapOf(
        "AR" to Lang.ARABIC,
        "BG" to Lang.BULGARIAN,
        "ZH" to Lang.CHINESE,
        "ZH-HANS" to Lang.CHINESE_SIMPLIFIED,
        "ZH-HANT" to Lang.CHINESE_TRADITIONAL,
        "CS" to Lang.CZECH,
        "DA" to Lang.DANISH,
        "NL" to Lang.DUTCH,
        "EN" to Lang.ENGLISH,
        "EN-US" to Lang.ENGLISH_AMERICAN,
        "EN-GB" to Lang.ENGLISH_BRITISH,
        "ET" to Lang.ESTONIAN,
        "FI" to Lang.FINNISH,
        "FR" to Lang.FRENCH,
        "DE" to Lang.GERMAN,
        "EL" to Lang.GREEK,
        "HE" to Lang.HEBREW,
        "HU" to Lang.HUNGARIAN,
        "IT" to Lang.ITALIAN,
        "ID" to Lang.INDONESIAN,
        "JA" to Lang.JAPANESE,
        "KO" to Lang.KOREAN,
        "LV" to Lang.LATVIAN,
        "LT" to Lang.LITHUANIAN,
        "NB" to Lang.NORWEGIAN,
        "PL" to Lang.POLISH,

        // PORTUGUESE is ambiguous, and the regions it
        // represents differ between the source and target languages.
        "PT" to Lang.PORTUGUESE,
        "PT-PT" to Lang.PORTUGUESE,

        "PT-BR" to Lang.PORTUGUESE_BRAZILIAN,
        "RO" to Lang.ROMANIAN,
        "RU" to Lang.RUSSIAN,
        "SK" to Lang.SLOVAK,
        "SL" to Lang.SLOVENIAN,
        "ES" to Lang.SPANISH,
        "SV" to Lang.SWEDISH,
        "TH" to Lang.THAI,
        "TR" to Lang.TURKISH,
        "UK" to Lang.UKRAINIAN,
        "VI" to Lang.VIETNAMESE,
    )
}

/**
 * Source Language Code for DeepL Translator.
 */
val Lang.deeplLanguageCodeForSource: String
    get() = when (this) {
        Lang.PORTUGUESE -> "PT"
        else -> adapter.getLanguageCode(this)
    }

/**
 * Target Language Code for DeepL Translator.
 */
val Lang.deeplLanguageCodeForTarget: String
    get() = when (this) {
        Lang.PORTUGUESE -> "PT-PT"
        else -> adapter.getLanguageCode(this)
    }

/**
 * Returns the [language][Lang] for the specified DeepL Translator language [code].
 */
fun Lang.Companion.fromDeeplLanguageCode(code: String): Lang {
    return adapter.getLanguage(code)
}