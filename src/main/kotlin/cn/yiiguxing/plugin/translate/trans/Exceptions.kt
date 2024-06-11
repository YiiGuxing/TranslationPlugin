package cn.yiiguxing.plugin.translate.trans

class TranslateException(
    @Suppress("MemberVisibilityCanBePrivate")
    val translatorId: String,
    val translatorName: String,
    val errorInfo: ErrorInfo,
    cause: Throwable? = null
) : RuntimeException("$translatorName[$translatorId] :: ${errorInfo.message}", cause)

class UnsupportedLanguageException(
    val lang: Lang,
    message: String = "Unsupported language: ${lang.langName}"
) : RuntimeException(message)

open class TranslationResultException(val code: Int) : RuntimeException("Translation result code: $code")

class ContentLengthLimitException(message: String = "Content length limit exceeded") : Exception(message) {
    constructor(limit: Int, actual: Int) : this("Content length limit exceeded, limit:$limit, actual:$actual")
}

fun checkContentLength(value: String, limit: Int): String {
    if (limit > 0 && value.length > limit) {
        throw ContentLengthLimitException(limit, value.length)
    }
    return value
}