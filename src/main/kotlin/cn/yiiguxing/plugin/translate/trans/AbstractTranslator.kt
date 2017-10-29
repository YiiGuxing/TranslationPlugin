package cn.yiiguxing.plugin.translate.trans

import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder

/**
 * AbstractTranslator
 *
 * Created by Yii.Guxing on 2017-10-29 0029.
 */
abstract class AbstractTranslator : Translator {

    protected abstract fun getTranslateUrl(text: String, srcLang: Lang, targetLang: Lang): String

    protected open fun buildRequest(builder: RequestBuilder) {}

    protected abstract fun parserResult(text: String): Translation

    override fun translate(text: String, srcLang: Lang, targetLang: Lang): Translation = HttpRequests
            .request(getTranslateUrl(text, srcLang, targetLang))
            .also { buildRequest(it) }
            .connect {
                parserResult(it.readString(null))
            }
}