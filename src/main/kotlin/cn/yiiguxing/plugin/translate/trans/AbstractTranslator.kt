package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.diagnostic.ReportException
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.getCommonMessage
import com.intellij.openapi.diagnostic.thisLogger
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
        doTranslate(text, srcLang, targetLang)
    }

    protected abstract fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation

    protected inline fun <T> checkError(action: () -> T): T = try {
        action()
    } catch (throwable: Throwable) {
        var error = throwable
        if (throwable is ReportException) {
            thisLogger().error(throwable.message, throwable, *throwable.attachments)
            throwable.cause?.let { error = it }
        }

        onError(error)
    }

    protected open fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        val errorMessage = when (throwable) {
            is UnsupportedLanguageException -> message("error.unsupportedLanguage", throwable.lang.localeName)
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
        throw TranslationException(id, name, errorInfo, throwable)
    }

}