package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger


/**
 * TranslateService
 *
 * Created by Yii.Guxing on 2017/10/30
 */
class TranslateService private constructor() {

    @Volatile
    var translator: Translator = DEFAULT_TRANSLATOR
    private val settings: Settings = Settings.instance
    private val cache = LruCache<CacheKey, Translation>(500)

    companion object {
        val DEFAULT_TRANSLATOR: Translator = GoogleTranslator

        val instance: TranslateService
            get() = ServiceManager.getService(TranslateService::class.java)

        private val LOGGER = Logger.getInstance(TranslateService::class.java)

        private fun checkThread() = checkDispatchThread(TranslateService::class.java)
    }

    init {
        invokeOnDispatchThread { setTranslator(settings.translator) }
        ApplicationManager
                .getApplication()
                .messageBus
                .connect()
                .subscribe(SettingsChangeListener.TOPIC, object : SettingsChangeListener {
                    override fun onTranslatorChanged(settings: Settings, translatorId: String) {
                        setTranslator(translatorId)
                    }
                })
    }

    fun setTranslator(translatorId: String) {
        checkThread()
        if (translatorId != translator.id) {
            translator = when (translatorId) {
                YoudaoTranslator.TRANSLATOR_ID -> YoudaoTranslator
                else -> DEFAULT_TRANSLATOR
            }
        }
    }

    fun getTranslators(): List<Translator> = listOf(GoogleTranslator, YoudaoTranslator)

    fun getCache(text: String, srcLang: Lang, targetLang: Lang): Translation? {
        checkThread()
        return cache[CacheKey(text, srcLang, targetLang, translator.id)]
    }

    fun translate(text: String, srcLang: Lang, targetLang: Lang, listener: TranslateListener) {
        checkThread()
        cache[CacheKey(text, srcLang, targetLang, translator.id)]?.let {
            listener.onSuccess(it)
            return
        }

        executeOnPooledThread {
            try {
                with(translator) {
                    translate(text, srcLang, targetLang).let {
                        it.cache(text, srcLang, targetLang, id)
                        invokeLater(ModalityState.any()) { listener.onSuccess(it) }
                    }
                }
            } catch (e: TranslateException) {
                LOGGER.w(e) { "translate" }
                invokeLater(ModalityState.any()) { listener.onError(e.message, e) }
            }
        }
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
}