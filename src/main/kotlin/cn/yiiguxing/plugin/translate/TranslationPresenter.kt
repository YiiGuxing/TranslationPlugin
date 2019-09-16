package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.TargetLanguageSelection.*
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.TranslateListener
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.diagnostic.Logger
import java.lang.ref.WeakReference

class TranslationPresenter(private val view: View, private val recordHistory: Boolean = true) : Presenter {

    private val translateService = TranslateService
    private val settings = Settings.instance
    private val appStorage = AppStorage.instance
    private var currentRequest: Presenter.Request? = null

    override val translatorId: String
        get() = translateService.translator.id

    override val histories: List<String> get() = appStorage.getHistories()

    override val primaryLanguage: Lang get() = translateService.translator.primaryLanguage

    override val supportedLanguages: SupportedLanguages
        get() = with(translateService.translator) {
            SupportedLanguages(supportedSourceLanguages, supportedTargetLanguages)
        }

    override fun getCache(text: String, srcLang: Lang, targetLang: Lang): Translation? {
        return translateService.getCache(text, srcLang, targetLang)
    }

    override fun getTargetLang(text: String): Lang {
        return when (settings.targetLanguageSelection) {
            DEFAULT -> if (text.any(NON_LATIN_CONDITION)) Lang.ENGLISH else primaryLanguage
            PRIMARY_LANGUAGE -> primaryLanguage
            LAST -> appStorage.lastLanguages.target.takeIf {
                translateService.translator.supportedTargetLanguages.contains(it)
            } ?: primaryLanguage
        }
    }

    override fun updateLastLanguages(srcLang: Lang, targetLang: Lang) {
        with(appStorage.lastLanguages) {
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
            appStorage.addHistory(text)
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

        override fun onError(message: String, throwable: Throwable) {
            val presenter = presenterRef.get()
            if (presenter !== null) {
                presenter.onPostResult(request) { showError(request, message, throwable) }
            } else {
                LOGGER.w("We lost the presenter!")
            }
        }
    }

    companion object {
        private val LOGGER = Logger.getInstance(TranslationPresenter::class.java)
    }
}
