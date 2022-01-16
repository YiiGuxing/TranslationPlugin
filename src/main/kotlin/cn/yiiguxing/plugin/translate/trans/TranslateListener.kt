package cn.yiiguxing.plugin.translate.trans

/**
 * TranslateListener
 */
interface TranslateListener {

    fun onSuccess(translation: Translation)

    fun onError(throwable: Throwable)

}