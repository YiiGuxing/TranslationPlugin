package cn.yiiguxing.plugin.translate.trans

/**
 * TranslateListener
 *
 * Created by Yii.Guxing on 2017-11-05 0005.
 */
interface TranslateListener {

    fun onSuccess(translation: Translation)

    fun onError(message: String, throwable: Throwable)

}