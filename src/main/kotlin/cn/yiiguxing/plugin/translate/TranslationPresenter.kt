package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.TranslateListener
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.util.AppStorage
import cn.yiiguxing.plugin.translate.util.TextToSpeech
import cn.yiiguxing.plugin.translate.util.TranslateService
import java.lang.ref.WeakReference

class TranslationPresenter(private val view: View, private val recordHistory: Boolean = true) : Presenter {

    private val translateService = TranslateService
    private val appStorage = AppStorage
    private var lastRequest: Presenter.Request? = null

    override val histories: List<String> get() = appStorage.getHistories()

    override val primaryLanguage: Lang get() = translateService.translator.primaryLanguage

    override val supportedLanguages: SupportedLanguages
        get() = with(translateService.translator) {
            SupportedLanguages(supportedSourceLanguages, supportedTargetLanguages)
        }

    override fun getCache(text: String, srcLang: Lang, targetLang: Lang): Translation? {
        return translateService.getCache(text, srcLang, targetLang)
    }

    override fun translate(text: String, srcLang: Lang, targetLang: Lang) {
        val request = Presenter.Request(text, srcLang, targetLang, translateService.translator.id)
        if (text.isBlank() || request == lastRequest) {
            return
        }

        TextToSpeech.stop()

        lastRequest = request
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
        if (request == lastRequest && !view.disposed) {
            view.block()
        }
    }

    private class ResultListener(presenter: TranslationPresenter, val request: Presenter.Request) : TranslateListener {

        private val presenterRef: WeakReference<TranslationPresenter> = WeakReference(presenter)

        override fun onSuccess(translation: Translation) {
            presenterRef.get()?.apply {
                onPostResult(request) { showTranslation(request, translation, false) }
                lastRequest = null
            }
        }

        override fun onError(message: String, throwable: Throwable) {
            presenterRef.get()?.apply {
                onPostResult(request) { showError(request, message, throwable) }
                lastRequest = null
            }
        }
    }
}
