package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.DEFAULT_USER_AGENT
import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.util.toJVMReadOnlyList
import com.intellij.util.io.RequestBuilder

/**
 * GoogleTranslator
 *
 * Created by Yii.Guxing on 2017/11/10
 */
object GoogleTranslator : AbstractTranslator() {

    const val TRANSLATOR_ID = "Google"

    private val settings = Settings.instance

    override val id: String = TRANSLATOR_ID

    override val primaryLanguage: Lang
        get() = settings.googleTranslateSettings.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = Lang.values().asList()
    override val supportedTargetLanguages: List<Lang> =
            mutableListOf(*Lang.values()).apply { remove(Lang.AUTO) }.toJVMReadOnlyList()

    override fun buildRequest(builder: RequestBuilder) {
        builder.userAgent(DEFAULT_USER_AGENT)
    }

    override fun getTranslateUrl(text: String, srcLang: Lang, targetLang: Lang): String {
        TODO("not implemented")
    }

    override fun parserResult(original: String, result: String): Translation {
        TODO("not implemented")
    }
}