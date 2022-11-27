package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.TargetLanguageSelection.*
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.TranslateListener
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.diagnostic.Logger
import java.lang.ref.WeakReference

class TranslationPresenter(private val view: View, private val recordHistory: Boolean = true) : Presenter {

    private val translateService = TranslateService
    private val settings = Settings.instance
    private val states = TranslationStates.instance
    private var currentRequest: Presenter.Request? = null

    override val translator: Translator
        get() = translateService.translator

    override val histories: List<String> get() = states.getHistories()

    override val primaryLanguage: Lang get() = translator.primaryLanguage

    override val supportedLanguages: SupportedLanguages
        get() = with(translator) {
            SupportedLanguages(supportedSourceLanguages, supportedTargetLanguages)
        }

    override fun isSupportedSourceLanguage(sourceLanguage: Lang): Boolean {
        return translator.supportedSourceLanguages.contains(sourceLanguage)
    }

    override fun isSupportedTargetLanguage(targetLanguage: Lang): Boolean {
        return translator.supportedTargetLanguages.contains(targetLanguage)
    }

    override fun getCache(text: String, srcLang: Lang, targetLang: Lang): Translation? {
        return translateService.getCache(text, srcLang, targetLang)
    }

    override fun getTargetLang(text: String): Lang {
        return when (settings.targetLanguageSelection) {
            DEFAULT -> Lang.AUTO.takeIf { isSupportedTargetLanguage(it) }
                ?: if (text.isEmpty() || text.any(NON_LATIN_CONDITION)) Lang.ENGLISH else primaryLanguage
            PRIMARY_LANGUAGE -> primaryLanguage
            LAST -> states.lastLanguages.target.takeIf { isSupportedTargetLanguage(it) } ?: primaryLanguage
        }
    }

    override fun updateLastLanguages(srcLang: Lang, targetLang: Lang) {
        with(states.lastLanguages) {
            source = srcLang
            target = targetLang
        }
    }

    override fun translate(text: String, srcLang: Lang, targetLang: Lang) {
        val request = Presenter.Request(text, srcLang, targetLang, translateService.translator.id)
        if (text.isBlank() || request == currentRequest) {
            return
        }

        TextToSpeech.stop()

        currentRequest = request
        if (recordHistory) {
            states.addHistory(text)
        }

        getCache(text, srcLang, targetLang)?.let { cache ->
            onPostResult(request) { showTranslation(request, cache, true) }
            return
        }

        view.showStartTranslate(request, text)

        translateService.translate(text, srcLang, targetLang, ResultListener(this, request))
    }

    private inline fun onPostResult(request: Presenter.Request, block: View.() -> Unit) {
        if (request == currentRequest && !view.disposed) {
            view.block()
            currentRequest = null
        }
    }

    private class ResultListener(presenter: TranslationPresenter, val request: Presenter.Request) : TranslateListener {

        private val presenterRef: WeakReference<TranslationPresenter> = WeakReference(presenter)

        override fun onSuccess(translation: Translation) {
            val presenter = presenterRef.get()
            if (presenter !== null) {
                presenter.onPostResult(request) { showTranslation(request, translation, false) }
            } else {
                LOGGER.w("We lost the presenter!")
            }
        }

        override fun onError(throwable: Throwable) {
            val presenter = presenterRef.get()
            if (presenter !== null) {
                presenter.onPostResult(request) { showError(request, throwable) }
            } else {
                LOGGER.w("We lost the presenter!")
            }
        }
    }

    companion object {
        private val LOGGER = Logger.getInstance(TranslationPresenter::class.java)
    }
}
