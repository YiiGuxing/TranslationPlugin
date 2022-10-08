package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.getCommonMessage
import com.intellij.util.io.HttpRequests
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.IOException

/**
 * Provides a skeletal implementation of the [Translator] interface.
 */
abstract class AbstractTranslator : Translator {

    override val defaultLangForLocale: Lang by lazy {
        Lang.default.takeIf { it in supportedTargetLanguages } ?: Lang.ENGLISH
    }

    final override fun translate(text: String, srcLang: Lang, targetLang: Lang): Translation = checkError {
        checkContentLength(text, contentLengthLimit)
        doTranslate(text, srcLang, targetLang)
    }

    protected abstract fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation

    protected inline fun <T> checkError(action: () -> T): T = try {
        action()
    } catch (throwable: Throwable) {
        onError(throwable)
    }

    protected open fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        val errorMessage = when (throwable) {
            is UnsupportedLanguageException -> message("error.unsupportedLanguage", throwable.lang.langName)
            is ContentLengthLimitException -> message("error.text.too.long")
            is HttpRequests.HttpStatusException -> when (throwable.statusCode) {
                HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE.code() -> message("error.text.too.long")
                else -> throwable.getCommonMessage()
            }

            is IOException -> throwable.getCommonMessage()
            else -> return null
        }

        return ErrorInfo(errorMessage)
    }

    protected fun onError(throwable: Throwable): Nothing {
        val errorInfo = createErrorInfo(throwable) ?: throw throwable
        throw TranslateException(id, name, errorInfo, throwable)
    }

}