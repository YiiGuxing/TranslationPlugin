package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.util.toJVMReadOnlyList

/**
 * GoogleTranslator
 * <p>
 * Created by Yii.Guxing on 2017/11/10
 */
object GoogleTranslator : AbstractTranslator() {

    const val TRANSLATOR_ID = "Google"

    override val id: String = TRANSLATOR_ID

    override val supportedSourceLanguages: List<Lang> = Lang.values().asList()
    override val supportedTargetLanguages: List<Lang> =
            mutableListOf(*Lang.values()).apply { remove(Lang.AUTO) }.toJVMReadOnlyList()

    override fun getTranslateUrl(text: String, srcLang: Lang, targetLang: Lang): String {
        TODO("not implemented")
    }

    override fun parserResult(result: String): Translation {
        TODO("not implemented")
    }
}