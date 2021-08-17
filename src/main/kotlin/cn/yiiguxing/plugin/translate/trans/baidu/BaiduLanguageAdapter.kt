package cn.yiiguxing.plugin.translate.trans.baidu

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

object BaiduLanguageAdapter : BaseLanguageAdapter() {

    override fun getAdaptedLanguages(): Map<String, Lang> = mapOf(
        "zh" to Lang.CHINESE,
        "cht" to Lang.CHINESE_TRADITIONAL,
        "wyw" to Lang.CHINESE_CLASSICAL,
        "yue" to Lang.CHINESE_CANTONESE,
        "ara" to Lang.ARABIC,
        "est" to Lang.ESTONIAN,
        "bul" to Lang.BULGARIAN,
        "dan" to Lang.DANISH,
        "fra" to Lang.FRENCH,
        "fin" to Lang.FINNISH,
        "kor" to Lang.KOREAN,
        "rom" to Lang.ROMANIAN,
        "jp" to Lang.JAPANESE,
        "swe" to Lang.SWEDISH,
        "slo" to Lang.SLOVENIAN,
        "spa" to Lang.SPANISH,
        "vie" to Lang.VIETNAMESE,
    )

}

val Lang.baiduLanguageCode: String
    get() = BaiduLanguageAdapter.getLanguageCode(this)

fun Lang.Companion.fromBaiduLanguageCode(code: String): Lang {
    return BaiduLanguageAdapter.getLanguage(code)
}