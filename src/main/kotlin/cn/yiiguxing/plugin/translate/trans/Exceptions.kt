package cn.yiiguxing.plugin.translate.trans

/**
 * TranslateException
 *
 * Created by Yii.Guxing on 2017/10/30
 */
open class TranslateException(override val message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class UnsupportedLanguageException(val lang: Lang) : TranslateException("Unsupported language: ${lang.langName}")

class TranslateResultException(val code: Int) : TranslateException("Translate failed: $code")