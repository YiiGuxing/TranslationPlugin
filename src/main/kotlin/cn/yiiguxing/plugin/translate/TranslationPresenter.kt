package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.TranslateListener
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.util.AppStorage
import cn.yiiguxing.plugin.translate.util.TextToSpeech
import cn.yiiguxing.plugin.translate.util.TranslateService
import java.lang.ref.WeakReference

class TranslationPresenter(private val view: View) : Presenter {

    private val translateService = TranslateService
    private val appStorage = AppStorage
    private var lastRequest: Request? = null

    override val histories: List<String> get() = appStorage.getHistories()

    override val primaryLanguage: Lang get() = translateService.translator.primaryLanguage

    override val supportedLanguages: SupportedLanguages
        get() = with(translateService.translator) {
            SupportedLanguages(supportedSourceLanguages, supportedTargetLanguages)
        }

    data class Request(val text: String, val srcLang: Lang, val targetLang: Lang, val translatorId: String)

    override fun getCache(text: String, srcLang: Lang, targetLang: Lang): Translation? {
        return translateService.getCache(text, srcLang, targetLang)
    }

    override fun translate(text: String, srcLang: Lang, targetLang: Lang) {
        val request = Request(text, srcLang, targetLang, translateService.translator.id)
        if (text.isBlank() || request == lastRequest) {
            return
        }

        TextToSpeech.stop()

        lastRequest = request
        with(appStorage) {
            addHistory(text)
            lastSourceLanguage = srcLang
            lastTargetLanguage = targetLang
        }

        getCache(text, srcLang, targetLang)?.let { cache ->
            onPostResult(request) { showTranslation(cache) }
            return
        }

        view.showStartTranslate(text)
        translateService.translate(text, srcLang, targetLang, ResultListener(this, request))
    }

    private inline fun onPostResult(request: Request, block: View.() -> Unit) {
        if (request == lastRequest && !view.disposed) {
            view.block()
        }
    }

    private class ResultListener(presenter: TranslationPresenter, val request: Request) : TranslateListener {

        private val presenterRef: WeakReference<TranslationPresenter> = WeakReference(presenter)

        override fun onSuccess(translation: Translation) {
            presenterRef.get()?.onPostResult(request) {
                showTranslation(translation)
            }
        }

        override fun onError(message: String, throwable: Throwable) {
            presenterRef.get()?.onPostResult(request) {
                showError(message, throwable)
            }
        }
    }
}
