package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.trans.ali.AliTranslator
import cn.yiiguxing.plugin.translate.trans.baidu.BaiduTranslator
import cn.yiiguxing.plugin.translate.trans.edge.EdgeTranslator
import cn.yiiguxing.plugin.translate.trans.google.GoogleTranslator
import cn.yiiguxing.plugin.translate.trans.youdao.YoudaoTranslator
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import cn.yiiguxing.plugin.translate.wordbook.WordBookListener
import cn.yiiguxing.plugin.translate.wordbook.WordBookService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
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
                TranslationEngine.ALI -> AliTranslator
                TranslationEngine.EDGE -> EdgeTranslator
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

        val modalityState = ModalityState.current()
        executeOnPooledThread {
            try {
                with(translator) {
                    translate(text, srcLang, targetLang).let { translation ->
                        translation.favoriteId = getFavoriteId(translation)
                        CacheService.putMemoryCache(text, srcLang, targetLang, id, translation)
                        invokeLater(modalityState) { listeners.run(key) { onSuccess(translation) } }
                    }
                }
            } catch (error: Throwable) {
                if (error is TranslateException) {
                    // 已知异常，仅记录日志
                    LOG.w("Translation error", error)
                } else {
                    // 将异常写入IDE异常池，以便用户反馈
                    investigate(text, srcLang, targetLang, error)
                }
                invokeLater(modalityState) { listeners.run(key) { onError(error) } }
            }
        }
    }

    private fun getFavoriteId(translation: Translation): Long? {
        return try {
            WordBookService.instance
                .takeIf { it.isInitialized && it.canAddToWordbook(translation.original) }
                ?.getWordId(translation.original, translation.srcLang, translation.targetLang)
        } catch (e: Throwable) {
            LOG.w("Failed to get favorite id", e)
            null
        }
    }

    private fun investigate(requestText: String, srcLang: Lang, targetLang: Lang, error: Throwable) {
        val requestAttachment = TranslationAttachmentFactory
            .createRequestAttachment(translator, requestText, srcLang, targetLang)
        LOG.error("Translation error[${translator.id}]: ${error.message}", error, requestAttachment)
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
            get() = ApplicationManager.getApplication().getService(TranslateService::class.java)

        private val LOG = Logger.getInstance(TranslateService::class.java)

        private fun checkThread() = checkDispatchThread(TranslateService::class.java)
    }
}