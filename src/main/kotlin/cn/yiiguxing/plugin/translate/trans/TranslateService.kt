package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import cn.yiiguxing.plugin.translate.wordbook.WordBookListener
import cn.yiiguxing.plugin.translate.wordbook.WordBookService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.MessageBusConnection


/**
 * TranslateService
 */
class TranslateService private constructor() : Disposable {

    @Volatile
    var translator: Translator = DEFAULT_TRANSLATOR
        private set

    private val listeners = mutableMapOf<ListenerKey, MutableSet<TranslateListener>>()

    init {
        setTranslator(Settings.instance.translator)
        Application.messageBus
            .connect(this)
            .subscribeSettingsTopic()
            .subscribeWordBookTopic()
    }

    fun setTranslator(newTranslator: TranslationEngine) {
        if (newTranslator.id != translator.id) {
            translator = when (newTranslator) {
                TranslationEngine.YOUDAO -> YoudaoTranslator
                TranslationEngine.BAIDU -> BaiduTranslator
                else -> DEFAULT_TRANSLATOR
            }
        }
    }

    fun getCache(text: String, srcLang: Lang, targetLang: Lang): Translation? {
        checkThread()
        return CacheService.getMemoryCache(text, srcLang, targetLang, translator.id)
    }

    fun translate(text: String, srcLang: Lang, targetLang: Lang, listener: TranslateListener) {
        checkThread()
        CacheService.getMemoryCache(text, srcLang, targetLang, translator.id)?.let {
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
                            .takeIf { it.isInitialized && it.canAddToWordbook(text) }
                            // 这里的`sourceLanguage`参数不能直接使用`srcLang`，因为`srcLang`的值可能为`Lang.AUTO`
                            ?.getWordId(text, translation.srcLang, translation.targetLang)
                        CacheService.putMemoryCache(text, srcLang, targetLang, id, translation)
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

    private fun notifyFavoriteAdded(item: WordBookItem) {
        checkThread()
        for ((_, translation) in CacheService.getMemoryCacheSnapshot()) {
            if (translation.favoriteId == null &&
                translation.srcLang == item.sourceLanguage &&
                translation.targetLang == item.targetLanguage &&
                translation.original.equals(item.word, true)
            ) {
                translation.favoriteId = item.id
            }
        }
    }

    private fun notifyFavoriteRemoved(favoriteId: Long) {
        checkThread()
        for ((_, translation) in CacheService.getMemoryCacheSnapshot()) {
            if (translation.favoriteId == favoriteId) {
                translation.favoriteId = null
            }
        }
    }

    private fun MessageBusConnection.subscribeSettingsTopic() = apply {
        subscribe(SettingsChangeListener.TOPIC, object : SettingsChangeListener {
            override fun onTranslatorChanged(settings: Settings, translationEngine: TranslationEngine) {
                setTranslator(translationEngine)
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

    override fun dispose() {}

    private data class ListenerKey(val text: String, val srcLang: Lang, val targetLang: Lang)

    companion object {
        val DEFAULT_TRANSLATOR: Translator = GoogleTranslator

        val instance: TranslateService
            get() = ServiceManager.getService(TranslateService::class.java)

        private val LOGGER = Logger.getInstance(TranslateService::class.java)

        private fun checkThread() = checkDispatchThread(TranslateService::class.java)
    }
}