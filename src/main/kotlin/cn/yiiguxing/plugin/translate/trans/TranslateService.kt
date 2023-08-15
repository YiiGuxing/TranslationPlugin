package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.trans.ali.AliTranslator
import cn.yiiguxing.plugin.translate.trans.baidu.BaiduTranslator
import cn.yiiguxing.plugin.translate.trans.deepl.DeeplTranslator
import cn.yiiguxing.plugin.translate.trans.google.GoogleTranslator
import cn.yiiguxing.plugin.translate.trans.microsoft.MicrosoftTranslator
import cn.yiiguxing.plugin.translate.trans.openai.OpenAITranslator
import cn.yiiguxing.plugin.translate.trans.youdao.YoudaoTranslator
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import cn.yiiguxing.plugin.translate.wordbook.WordBookListener
import cn.yiiguxing.plugin.translate.wordbook.WordBookService
import cn.yiiguxing.plugin.translate.wordbook.WordBookViewListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.MessageBusConnection
import java.util.*


/**
 * TranslateService
 */
@Service
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
            .subscribeWordBookViewTopic()
    }

    fun setTranslator(newTranslator: TranslationEngine) {
        if (newTranslator.id != translator.id) {
            translator = when (newTranslator) {
                TranslationEngine.MICROSOFT -> MicrosoftTranslator
                TranslationEngine.GOOGLE -> GoogleTranslator
                TranslationEngine.YOUDAO -> YoudaoTranslator
                TranslationEngine.BAIDU -> BaiduTranslator
                TranslationEngine.ALI -> AliTranslator
                TranslationEngine.DEEPL -> DeeplTranslator
                TranslationEngine.OPEN_AI -> OpenAITranslator
            }
        }
    }

    fun getCache(text: String, srcLang: Lang, targetLang: Lang): Translation? {
        checkThread()
        return CacheService.getMemoryCache(text, srcLang, targetLang, translator.id)
    }

    fun translate(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        listener: TranslateListener,
        modalityState: ModalityState = ModalityState.defaultModalityState()
    ) {
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

    private fun Translation.updateFavoriteStateIfNeed(favorites: List<WordBookItem>) {
        favorites.find { favorite ->
            srcLang == favorite.sourceLanguage
                    && targetLang == favorite.targetLanguage
                    && original.equals(favorite.word, true)
        }?.let { favorite ->
            favoriteId = favorite.id
        }
    }

    private fun updateFavorites(favorites: List<WordBookItem>) {
        checkThread()
        for ((_, translation) in CacheService.getMemoryCacheSnapshot()) {
            translation.favoriteId = null
            translation.updateFavoriteStateIfNeed(favorites)
        }
    }

    private fun notifyFavoriteAdded(favorites: List<WordBookItem>) {
        checkThread()
        for ((_, translation) in CacheService.getMemoryCacheSnapshot()) {
            translation.updateFavoriteStateIfNeed(favorites)
        }
    }

    private fun notifyFavoriteRemoved(favoriteIds: List<Long>) {
        checkThread()
        for ((_, translation) in CacheService.getMemoryCacheSnapshot()) {
            if (translation.favoriteId in favoriteIds) {
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
            override fun onWordsAdded(service: WordBookService, words: List<WordBookItem>) = notifyFavoriteAdded(words)
            override fun onWordsRemoved(service: WordBookService, wordIds: List<Long>) = notifyFavoriteRemoved(wordIds)
        })
    }

    private fun MessageBusConnection.subscribeWordBookViewTopic() = apply {
        subscribe(WordBookViewListener.TOPIC, object : WordBookViewListener {
            override fun onWordBookRefreshed(words: List<WordBookItem>) = updateFavorites(words)
        })
    }

    override fun dispose() {}

    private data class ListenerKey(val text: String, val srcLang: Lang, val targetLang: Lang)

    companion object {
        val DEFAULT_TRANSLATOR: Translator by lazy {
            if (Locale.getDefault() == Locale.CHINA) MicrosoftTranslator else GoogleTranslator
        }

        val instance: TranslateService
            get() = ApplicationManager.getApplication().getService(TranslateService::class.java)

        private val LOG = Logger.getInstance(TranslateService::class.java)

        private fun checkThread() = checkDispatchThread<TranslateService>()
    }
}