package cn.yiiguxing.plugin.translate.service

import cn.yiiguxing.plugin.translate.TranslationStorages
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.isExplicit
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.delete
import java.io.IOException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.readText

@Service
@State(name = "Translation.Cache", storages = [(Storage(TranslationStorages.PREFERENCES_STORAGE_NAME))])
class CacheService : PersistentStateComponent<CacheService.State> {

    private val state = State()

    private val memoryCache = LruCache<MemoryCacheKey, Translation>(MAX_MEMORY_CACHE_SIZE)

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state.lastTrimTime = state.lastTrimTime
    }

    fun putMemoryCache(text: String, srcLang: Lang, targetLang: Lang, translatorId: String, translation: Translation) {
        memoryCache.put(MemoryCacheKey(text, srcLang, targetLang, translatorId), translation)

        val srcLangToCache = when {
            srcLang.isExplicit() -> srcLang
            translation.srcLang.isExplicit() -> translation.srcLang
            else -> return
        }
        val targetLangToCache = when {
            targetLang.isExplicit() -> targetLang
            translation.targetLang.isExplicit() -> translation.targetLang
            else -> return
        }
        if (srcLangToCache != srcLang || targetLangToCache != targetLang) {
            memoryCache.put(MemoryCacheKey(text, srcLangToCache, targetLangToCache, translatorId), translation)
        }
    }

    fun getMemoryCache(text: String, srcLang: Lang, targetLang: Lang, translatorId: String): Translation? {
        return memoryCache[MemoryCacheKey(text, srcLang, targetLang, translatorId)]
    }

    fun removeMemoryCache(
        predicate: (MemoryCacheKey, Translation) -> Boolean
    ): Set<Map.Entry<MemoryCacheKey, Translation>> {
        return memoryCache.removeIf(predicate)
    }

    fun getMemoryCacheSnapshot(): Map<MemoryCacheKey, Translation> {
        return memoryCache.snapshot
    }

    fun putDiskCache(key: String, translation: String) {
        try {
            TranslationStorages.createCacheDirectoriesIfNotExists()
            getCacheFilePath(key).writeSafe { it.write(translation.toByteArray()) }
            LOG.d("Puts disk cache: $key")
            trimDiskCachesIfNeed()
        } catch (e: Exception) {
            LOG.w(e)
        }
    }

    fun getDiskCache(key: String): String? {
        return try {
            getCacheFilePath(key).takeIf { Files.isRegularFile(it) }?.readText()?.apply {
                LOG.d("Disk cache hit: $key")
            }
        } catch (e: Exception) {
            LOG.w(e)
            null
        }
    }

    @Synchronized
    private fun trimDiskCachesIfNeed() {
        val now = System.currentTimeMillis()
        val duration = now - state.lastTrimTime
        if (duration < 0 || duration > TRIM_INTERVAL) {
            state.lastTrimTime = now
            executeOnPooledThread {
                try {
                    trimDiskCaches()
                } catch (e: Exception) {
                    LOG.w(e)
                }
            }
        }
    }

    private fun trimDiskCaches() {
        val names = TranslationStorages.CACHE_DIRECTORY
            .toFile()
            .list { _, name -> !name.endsWith(".tmp") }
            ?.takeIf { it.size > MAX_DISK_CACHE_SIZE }
            ?: return

        names.asSequence()
            .map { name -> getCacheFilePath(name) }
            .sortedBy { file ->
                try {
                    Files.readAttributes(file, BasicFileAttributes::class.java).lastAccessTime().toMillis()
                } catch (e: NoSuchFileException) {
                    -1L
                }
            }
            .take(names.size - MAX_DISK_CACHE_SIZE)
            .forEach { file ->
                try {
                    Files.deleteIfExists(file)
                } catch (e: DirectoryNotEmptyException) {
                    // ignore
                }
            }

        LOG.d("Disk cache has been trimmed.")
    }

    fun getDiskCacheSize(): Long {
        val names = TranslationStorages.CACHE_DIRECTORY
            .toFile()
            .list { _, name -> !name.endsWith(".tmp") }
            ?: return 0

        return names.asSequence()
            .map { name ->
                try {
                    Files.size(getCacheFilePath(name))
                } catch (e: IOException) {
                    0L
                }
            }
            .sum()
    }

    fun evictAllDiskCaches() {
        try {
            TranslationStorages.CACHE_DIRECTORY.delete(true)
        } catch (e: Throwable) {
            // ignore
        }
    }

    /**
     * Memory cache key data class
     */
    data class MemoryCacheKey(
        val text: String,
        val srcLang: Lang,
        val targetLang: Lang,
        val translator: String = "unknown"
    )

    data class State(@Volatile var lastTrimTime: Long = System.currentTimeMillis())

    companion object {
        private const val MAX_MEMORY_CACHE_SIZE = 1024
        private const val MAX_DISK_CACHE_SIZE = 1024
        private const val TRIM_INTERVAL = 5 * 24 * 60 * 60 * 1000 // 5 days

        private val LOG = logger<CacheService>()

        fun getInstance(): CacheService = service()

        fun getCacheFilePath(key: String): Path = TranslationStorages.CACHE_DIRECTORY.resolve(key)
    }
}