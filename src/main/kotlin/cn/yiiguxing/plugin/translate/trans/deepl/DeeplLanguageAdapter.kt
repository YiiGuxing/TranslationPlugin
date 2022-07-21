package cn.yiiguxing.plugin.translate.trans.deepl

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

object DeeplLanguageAdapter : BaseLanguageAdapter() {
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

val Lang.deeplLanguageCode: String
    get() = DeeplLanguageAdapter.getLanguageCode(this)

fun Lang.Companion.fromDeeplLanguageCode(code: String): Lang {
    return DeeplLanguageAdapter.getLanguage(code)
}