package cn.yiiguxing.plugin.translate.trans

/**
 * CacheKey
 *
 * Created by Yii.Guxing on 2017-06-11.
 */
data class CacheKey(val text: String, val srcLang: Lang, val targetLang: Lang, val translator: String = "unknown")
