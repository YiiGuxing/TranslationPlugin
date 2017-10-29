package cn.yiiguxing.plugin.translate.trans

/**
 * CacheKey
 *
 * Created by Yii.Guxing on 2017-06-11.
 */
data class CacheKey(val srcLang: Lang, val targetLang: Lang, val text: String, val translator: String = "unknown")
