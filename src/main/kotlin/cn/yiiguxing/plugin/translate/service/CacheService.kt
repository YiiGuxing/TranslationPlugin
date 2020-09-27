package cn.yiiguxing.plugin.translate.service

import cn.yiiguxing.plugin.translate.STORAGE_NAME
import cn.yiiguxing.plugin.translate.TRANSLATION_DIRECTORY
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.createDirectories
import com.intellij.util.io.delete
import com.intellij.util.io.readText
import java.io.IOException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

@Service
@State(name = "Cache", storages = [(Storage(STORAGE_NAME))])
class CacheService : PersistentStateComponent<CacheService.State> {

    private val state = State()

    private val memoryCache = LruCache<CacheKey, Translation>(MAX_MEMORY_CACHE_SIZE)

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state.lastTrimTime = state.lastTrimTime
    }

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

    private fun getDiskCacheKey(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        translatorId: String,
        isDocumentation: Boolean
    ): String {
        return "$text:$srcLang:$targetLang:$translatorId:$isDocumentation".md5()
    }

    fun putDiskCache(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        translatorId: String,
        isDocumentation: Boolean,
        translation: String
    ) {
        try {
            val cacheKey = getDiskCacheKey(text, srcLang, targetLang, translatorId, isDocumentation)
            CACHE_DIR.createDirectories()
            CACHE_DIR.resolve(cacheKey).writeSafe { it.write(translation.toByteArray()) }
            println("DEBUG - Puts disk cache: $cacheKey")
            trimDiskCachesIfNeed()
        } catch (e: Exception) {
            LOG.w(e)
        }
    }

    fun getDiskCache(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        translatorId: String,
        isDocumentation: Boolean
    ): String? {
        return try {
            val cacheKey = getDiskCacheKey(text, srcLang, targetLang, translatorId, isDocumentation)
            CACHE_DIR.resolve(cacheKey).takeIf { Files.isRegularFile(it) }?.readText()?.apply {
                println("DEBUG - Disk cache hit: $cacheKey")
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
        val names = CACHE_DIR
            .toFile()
            .list { _, name -> !name.endsWith(".tmp") }
            ?.takeIf { it.size > MAX_DISK_CACHE_SIZE }
            ?: return

        names.asSequence()
            .map { name -> CACHE_DIR.resolve(name) }
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
        val names = CACHE_DIR
            .toFile()
            .list { _, name -> !name.endsWith(".tmp") }
            ?: return 0

        return names.asSequence()
            .map { name ->
                try {
                    Files.size(CACHE_DIR.resolve(name))
                } catch (e: IOException) {
                    0L
                }
            }
            .sum()
    }

    fun evictAllDiskCaches() {
        try {
            CACHE_DIR.delete(true)
        } catch (e: Throwable) {
            // ignore
        }
    }

    /**
     * CacheKey
     */
    data class CacheKey(val text: String, val srcLang: Lang, val targetLang: Lang, val translator: String = "unknown")

    data class State(@Volatile var lastTrimTime: Long = System.currentTimeMillis())

    companion object {
        private const val MAX_MEMORY_CACHE_SIZE = 1024
        private const val MAX_DISK_CACHE_SIZE = 1024
        private const val TRIM_INTERVAL = 5 * 24 * 60 * 60 * 1000 // 5 days

        private val CACHE_DIR = TRANSLATION_DIRECTORY.resolve("caches")

        private val LOG = Logger.getInstance(CacheService::class.java)

        val instance: CacheService
            get() = ServiceManager.getService(CacheService::class.java)
    }
}