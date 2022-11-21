package cn.yiiguxing.plugin.translate.trans.edge

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

object EdgeLanguageAdapter : BaseLanguageAdapter() {

    override fun getAdaptedLanguages(): Map<String, Lang> = mapOf(
        "" to Lang.AUTO,
        "zh-Hans" to Lang.CHINESE,
        "zh-Hant" to Lang.CHINESE_TRADITIONAL,
    )

}

val Lang.edgeLanguageCode: String
    get() = EdgeLanguageAdapter.getLanguageCode(this)

@Suppress("unused")
fun Lang.Companion.fromEdgeLanguageCode(code: String): Lang {
    return EdgeLanguageAdapter.getLanguage(code)
}