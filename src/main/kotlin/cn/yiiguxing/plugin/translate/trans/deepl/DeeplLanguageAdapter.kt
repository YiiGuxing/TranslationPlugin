package cn.yiiguxing.plugin.translate.trans.deepl

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

/**
 * Language adapter for DeepL Translator.
 */
object DeeplLanguageAdapter : BaseLanguageAdapter() {

    override val supportedSourceLanguages: List<Lang> = listOf(
        Lang.AUTO,
        Lang.BULGARIAN,
        Lang.CHINESE,
        Lang.CZECH,
        Lang.DANISH,
        Lang.DUTCH,
        Lang.GERMAN,
        Lang.GREEK,
        Lang.ENGLISH,
        Lang.ESTONIAN,
        Lang.FINNISH,
        Lang.FRENCH,
        Lang.HUNGARIAN,
        Lang.ITALIAN,
        Lang.JAPANESE,
        Lang.LATVIAN,
        Lang.LITHUANIAN,
        Lang.POLISH,
        Lang.PORTUGUESE,
        Lang.ROMANIAN,
        Lang.RUSSIAN,
        Lang.SLOVAK,
        Lang.SLOVENIAN,
        Lang.SPANISH,
        Lang.SWEDISH,
    )

    override val supportedTargetLanguages: List<Lang> = listOf(
        Lang.BULGARIAN,
        Lang.CHINESE,
        Lang.CZECH,
        Lang.DANISH,
        Lang.DUTCH,
        Lang.GERMAN,
        Lang.GREEK,
        Lang.ENGLISH,
        Lang.ENGLISH_AMERICAN,
        Lang.ENGLISH_BRITISH,
        Lang.ESTONIAN,
        Lang.FINNISH,
        Lang.FRENCH,
        Lang.HUNGARIAN,
        Lang.ITALIAN,
        Lang.JAPANESE,
        Lang.LATVIAN,
        Lang.LITHUANIAN,
        Lang.POLISH,
        Lang.PORTUGUESE,
        Lang.PORTUGUESE_BRAZILIAN,
        Lang.PORTUGUESE_PORTUGUESE,
        Lang.ROMANIAN,
        Lang.RUSSIAN,
        Lang.SLOVAK,
        Lang.SLOVENIAN,
        Lang.SPANISH,
        Lang.SWEDISH,
    )

    override fun getAdaptedLanguages(): Map<String, Lang> = mapOf(
        "BG" to Lang.BULGARIAN,
        "ZH" to Lang.CHINESE,
        "CS" to Lang.CZECH,
        "DA" to Lang.DANISH,
        "NL" to Lang.DUTCH,
        "DE" to Lang.GERMAN,
        "EL" to Lang.GREEK,
        "EN" to Lang.ENGLISH,
        "EN-US" to Lang.ENGLISH_AMERICAN,
        "EN-GB" to Lang.ENGLISH_BRITISH,
        "ET" to Lang.ESTONIAN,
        "FI" to Lang.FINNISH,
        "FR" to Lang.FRENCH,
        "HU" to Lang.HUNGARIAN,
        "IT" to Lang.ITALIAN,
        "JA" to Lang.JAPANESE,
        "LV" to Lang.LATVIAN,
        "LT" to Lang.LITHUANIAN,
        "PL" to Lang.POLISH,
        "PT" to Lang.PORTUGUESE,
        "PT-BR" to Lang.PORTUGUESE_BRAZILIAN,
        "PT-PT" to Lang.PORTUGUESE_PORTUGUESE,
        "RO" to Lang.ROMANIAN,
        "RU" to Lang.RUSSIAN,
        "SK" to Lang.SLOVAK,
        "SL" to Lang.SLOVENIAN,
        "ES" to Lang.SPANISH,
        "SV" to Lang.SWEDISH,
    )
}


/**
 * Language code for DeepL Translator.
 */
val Lang.deeplLanguageCode: String
    get() = DeeplLanguageAdapter.getLanguageCode(this)

/**
 * Returns the [language][Lang] for the specified DeepL Translator language [code].
 */
fun Lang.Companion.fromDeeplLanguageCode(code: String): Lang {
    return DeeplLanguageAdapter.getLanguage(code)
}