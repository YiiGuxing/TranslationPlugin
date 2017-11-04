package cn.yiiguxing.plugin.translate.trans

/**
 * TranslatorFactory
 *
 * Created by Yii.Guxing on 2017-11-05 0005.
 */
object TranslatorFactory {

    val DEFAULT_TRANSLATOR = YoudaoTranslator()

    fun create(translatorId: String): Translator = when (translatorId) {
        YoudaoTranslator.TRANSLATOR_ID -> YoudaoTranslator()
        else -> DEFAULT_TRANSLATOR
    }

}