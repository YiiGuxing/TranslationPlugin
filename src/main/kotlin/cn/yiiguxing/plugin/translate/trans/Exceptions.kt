package cn.yiiguxing.plugin.translate.trans

/**
 * TranslateException
 *
 * Created by Yii.Guxing on 2017/10/30
 */
open class TranslateException(
    override val message: String,
    private val translatorName: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    override fun getLocalizedMessage(): String {
        return "$translatorName :: $message"
    }
}

class UnsupportedLanguageException(val lang: Lang, translatorName: String) :
    TranslateException("Unsupported language: ${lang.langName}", translatorName)

class TranslateResultException(val code: Int, translatorName: String) :
    TranslateException("Translate failed: $code", translatorName)