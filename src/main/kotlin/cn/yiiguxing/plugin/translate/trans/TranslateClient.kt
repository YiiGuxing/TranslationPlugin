package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.util.CacheService
import cn.yiiguxing.plugin.translate.util.toHexString
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.openapi.diagnostic.Logger
import java.security.MessageDigest

abstract class TranslateClient<T : BaseTranslation>(private val translator: Translator) {

    private var cacheKeyUpdater: ((MessageDigest) -> Unit)? = null

    fun updateCacheKey(keyUpdater: (key: MessageDigest) -> Unit) {
        cacheKeyUpdater = keyUpdater
    }

    protected open fun updateCacheKey(
        key: MessageDigest,
        translator: Translator,
        text: String,
        srcLang: Lang,
        targetLang: Lang
    ) {
        key.update("$text$srcLang$targetLang${translator.id}".toByteArray())
    }

    private fun getCacheKey(text: String, srcLang: Lang, targetLang: Lang): String {
        val key = MessageDigest.getInstance("MD5")
        updateCacheKey(key, translator, text, srcLang, targetLang)
        cacheKeyUpdater?.invoke(key)
        return key.digest().toHexString()
    }

    fun execute(text: String, srcLang: Lang, targetLang: Lang): T {
        if (srcLang !in translator.supportedSourceLanguages) {
            throw UnsupportedLanguageException(srcLang, translator.name)
        }
        if (targetLang !in translator.supportedTargetLanguages) {
            throw UnsupportedLanguageException(targetLang, translator.name)
        }

        val cacheKey = getCacheKey(text, srcLang, targetLang)
        val cache = CacheService.getDiskCache(cacheKey)
        if (cache != null) try {
            return parse(cache, text, srcLang, targetLang)
        } catch (e: Throwable) {
            LOG.w(e)
        }

        val result = doExecute(text, srcLang, targetLang)
        val translation = parse(result, text, srcLang, targetLang)
        CacheService.putDiskCache(cacheKey, result)

        return translation
    }

    protected abstract fun doExecute(text: String, srcLang: Lang, targetLang: Lang): String

    protected abstract fun parse(translation: String, original: String, srcLang: Lang, targetLang: Lang): T

    companion object {
        private val LOG = Logger.getInstance(TranslateClient::class.java)
    }

}