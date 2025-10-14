package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.message

class TranslationException(
    @Suppress("MemberVisibilityCanBePrivate")
    val translatorId: String,
    val translatorName: String,
    val errorInfo: ErrorInfo,
    cause: Throwable? = null
) : RuntimeException("$translatorName[$translatorId] :: ${errorInfo.message}", cause) {
    val translationErrorMessage: String get() = errorInfo.message
}

class UnsupportedLanguageException(
    val lang: Lang,
    message: String = "Unsupported language: ${lang.localeName}"
) : RuntimeException(message)

open class TranslationResultException(val code: Int) : RuntimeException("Translation result code: $code")

fun getTranslationErrorMessage(cause: Throwable): String {
    val errorMessage = when (cause) {
        is TranslationException -> cause.translationErrorMessage
        else -> cause.message
    }?.takeIf { it.isNotBlank() } ?: message("error.unknown")
    return message("documentation.message.translation.failed.with.message", errorMessage)
}
