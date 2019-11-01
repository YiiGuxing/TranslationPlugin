package cn.yiiguxing.plugin.translate.trans

/**
 * CacheKey
 */
data class CacheKey(val text: String, val srcLang: Lang, val targetLang: Lang, val translator: String = "unknown")
