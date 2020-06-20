package cn.yiiguxing.plugin.translate.service

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.util.LruCache
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager

@Service
class CacheService {

    private val memoryCache = LruCache<CacheKey, Translation>(1024)

    fun putMemoryCache(text: String, srcLang: Lang, targetLang: Lang, translatorId: String, translation: Translation) {
        memoryCache.put(CacheKey(text, srcLang, targetLang, translatorId), translation)
        if (Lang.AUTO == srcLang) {
            memoryCache.put(CacheKey(text, translation.srcLang, targetLang, translatorId), translation)
        }
        if (Lang.AUTO == targetLang) {
            memoryCache.put(CacheKey(text, srcLang, translation.targetLang, translatorId), translation)
        }
        if (Lang.AUTO == srcLang && Lang.AUTO == targetLang) {
            memoryCache.put(CacheKey(text, translation.srcLang, translation.targetLang, translatorId), translation)
        }
    }

    fun getMemoryCache(text: String, srcLang: Lang, targetLang: Lang, translatorId: String): Translation? {
        return memoryCache[CacheKey(text, srcLang, targetLang, translatorId)]
    }

    fun getMemoryCacheSnapshot(): Map<CacheKey, Translation> {
        return memoryCache.snapshot
    }

    /**
     * CacheKey
     */
    data class CacheKey(val text: String, val srcLang: Lang, val targetLang: Lang, val translator: String = "unknown")

    companion object {
        val instance: CacheService
            get() = ServiceManager.getService(CacheService::class.java)
    }
}