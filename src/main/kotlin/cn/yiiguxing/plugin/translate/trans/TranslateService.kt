package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import cn.yiiguxing.plugin.translate.wordbook.WordBookListener
import cn.yiiguxing.plugin.translate.wordbook.WordBookService
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.MessageBusConnection


/**
 * TranslateService
 */
class TranslateService private constructor() {

    @Volatile
    var translator: Translator = DEFAULT_TRANSLATOR
        private set

    private val cache = LruCache<CacheKey, Translation>(500)

    private val listeners = mutableMapOf<ListenerKey, MutableSet<TranslateListener>>()

    init {
        setTranslator(Settings.instance.translator)
        Application.messageBus
            .connect()
            .subscribeSettingsTopic()
            .subscribeWordBookTopic()
    }

    fun setTranslator(translatorId: String) {
        checkThread()
        if (translatorId != translator.id) {
            translator = when (translatorId) {
                YoudaoTranslator.TRANSLATOR_ID -> YoudaoTranslator
                BaiduTranslator.TRANSLATOR_ID -> BaiduTranslator
                else -> DEFAULT_TRANSLATOR
            }
        }
    }

    fun getTranslators(): List<Translator> = listOf(GoogleTranslator, YoudaoTranslator, BaiduTranslator)

    fun getCache(text: String, srcLang: Lang, targetLang: Lang): Translation? {
        checkThread()
        return cache[CacheKey(text, srcLang, targetLang, translator.id)]
    }

    fun clearCaches() {
        checkThread()
        cache.evictAll()
    }

    fun translate(text: String, srcLang: Lang, targetLang: Lang, listener: TranslateListener) {
        checkThread()
        cache[CacheKey(text, srcLang, targetLang, translator.id)]?.let {
            listener.onSuccess(it)
            return
        }

        val key = ListenerKey(text, srcLang, targetLang)
        listeners[key]?.let {
            it += listener
            return
        }
        listeners[key] = mutableSetOf(listener)

        executeOnPooledThread {
            try {
                with(translator) {
                    translate(text, srcLang, targetLang).let { translation ->
                        translation.favoriteId = WordBookService.instance
                            .takeIf { it.canAddToWordbook(text) }
                            // 这里的`sourceLanguage`参数不能直接使用`srcLang`，因为`srcLang`的值可能为`Lang.AUTO`
                            ?.getWordId(text, translation.srcLang, translation.targetLang)
                        translation.cache(text, srcLang, targetLang, id)
                        invokeLater(ModalityState.any()) {
                            listeners.run(key) { onSuccess(translation) }
                        }
                    }
                }
            } catch (e: TranslateException) {
                LOGGER.w("translate", e)
                invokeLater(ModalityState.any()) {
                    listeners.run(key) { onError(e.message, e) }
                }
            }
        }
    }

    private inline fun MutableMap<ListenerKey, MutableSet<TranslateListener>>.run(
        key: ListenerKey,
        action: TranslateListener.() -> Unit
    ) {
        remove(key)?.forEach { it.action() }
    }

    private fun Translation.cache(text: String, srcLang: Lang, targetLang: Lang, translatorId: String) {
        val cache = cache
        cache.put(CacheKey(text, srcLang, targetLang, translatorId), this)
        if (Lang.AUTO == srcLang) {
            cache.put(CacheKey(text, this.srcLang, targetLang, translatorId), this)
        }
        if (Lang.AUTO == targetLang) {
            cache.put(CacheKey(text, srcLang, this.targetLang, translatorId), this)
        }
        if (Lang.AUTO == srcLang && Lang.AUTO == targetLang) {
            cache.put(CacheKey(text, this.srcLang, this.targetLang, translatorId), this)
        }
    }

    private fun notifyFavoriteAdded(item: WordBookItem) {
        checkThread()
        synchronized(cache) {
            for ((_, translation) in cache.snapshot) {
                if (translation.favoriteId == null &&
                    translation.srcLang == item.sourceLanguage &&
                    translation.targetLang == item.targetLanguage &&
                    translation.original.equals(item.word, true)
                ) {
                    translation.favoriteId = item.id
                }
            }
        }
    }

    private fun notifyFavoriteRemoved(favoriteId: Long) {
        checkThread()
        synchronized(cache) {
            for ((_, translation) in cache.snapshot) {
                if (translation.favoriteId == favoriteId) {
                    translation.favoriteId = null
                }
            }
        }
    }

    private fun MessageBusConnection.subscribeSettingsTopic() = apply {
        subscribe(SettingsChangeListener.TOPIC, object : SettingsChangeListener {
            override fun onTranslatorChanged(settings: Settings, translatorId: String) {
                setTranslator(translatorId)
            }
        })
    }

    private fun MessageBusConnection.subscribeWordBookTopic() = apply {
        subscribe(WordBookListener.TOPIC, object : WordBookListener {
            override fun onWordAdded(service: WordBookService, wordBookItem: WordBookItem) {
                notifyFavoriteAdded(wordBookItem)
            }

            override fun onWordRemoved(service: WordBookService, id: Long) = notifyFavoriteRemoved(id)
        })
    }

    private data class ListenerKey(val text: String, val srcLang: Lang, val targetLang: Lang)

    companion object {
        val DEFAULT_TRANSLATOR: Translator = GoogleTranslator

        val instance: TranslateService
            get() = ServiceManager.getService(TranslateService::class.java)

        private val LOGGER = Logger.getInstance(TranslateService::class.java)

        private fun checkThread() = checkDispatchThread(TranslateService::class.java)
    }
}