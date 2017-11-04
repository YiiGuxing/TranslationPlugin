package cn.yiiguxing.plugin.translate.trans

import com.google.gson.JsonSyntaxException
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * AbstractTranslator
 *
 * Created by Yii.Guxing on 2017-10-29 0029.
 */
abstract class AbstractTranslator : Translator {

    protected abstract fun getTranslateUrl(text: String, srcLang: Lang, targetLang: Lang): String

    protected open fun buildRequest(builder: RequestBuilder) {}

    protected abstract fun parserResult(result: String): Translation

    protected open fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is UnsupportedLanguageException -> "不支持的语言类型：${throwable.lang.langName}"
        is ConnectException -> "网络连接失败"
        is SocketTimeoutException -> "网络连接超时，请检查网络连接"
        is JsonSyntaxException -> "无法解析翻译结果"
        else -> "翻译失败"
    }

    override fun translate(text: String, srcLang: Lang, targetLang: Lang): Translation = try {
        if (srcLang !in supportedSourceLanguages) {
            throw UnsupportedLanguageException(srcLang)
        }
        if (targetLang !in supportedTargetLanguages) {
            throw UnsupportedLanguageException(targetLang)
        }

        HttpRequests.request(getTranslateUrl(text, srcLang, targetLang))
                .also { buildRequest(it) }
                .connect {
                    parserResult(it.readString(null))
                }
    } catch (throwable: Throwable) {
        throw TranslateException(createErrorMessage(throwable), throwable)
    }
}