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
    message: String = "Unsupported language: ${lang.localeName}"
) : RuntimeException(message)

open class TranslationResultException(val code: Int) : RuntimeException("Translation result code: $code")
