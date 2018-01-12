package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.TranslateListener
import cn.yiiguxing.plugin.translate.trans.TranslateService
import cn.yiiguxing.plugin.translate.trans.Translation
import java.lang.ref.WeakReference

class TranslationPresenter(private val view: View) : Presenter {

    private val appStorage: AppStorage = AppStorage.instance
    private val translateService: TranslateService = TranslateService.instance
    private var currentQuery: CurrentQuery? = null

    override val histories: List<String> get() = appStorage.getHistories()

    override val primaryLanguage: Lang get() = translateService.translator.primaryLanguage

    override val supportedLanguages: SupportedLanguages
        get() = with(translateService.translator) {
            SupportedLanguages(supportedSourceLanguages, supportedTargetLanguages)
        }

    data class CurrentQuery(val text: String, val srcLang: Lang, val targetLang: Lang, val translatorId: String)

    override fun getCache(text: String, srcLang: Lang, targetLang: Lang): Translation? {
        return translateService.getCache(text, srcLang, targetLang)
    }

    override fun translate(text: String, srcLang: Lang, targetLang: Lang) {
        val currQuery = CurrentQuery(text, srcLang, targetLang, translateService.translator.id)
        if (text.isBlank() || currQuery == currentQuery) {
            return
        }

        currentQuery = currQuery
        with(appStorage) {
            addHistory(text)
            lastSourceLanguage = srcLang
            lastTargetLanguage = targetLang
        }

        getCache(text, srcLang, targetLang)?.let { cache ->
            onPostResult(currQuery) { showTranslation(cache) }
            return
        }

        view.showStartTranslate(text)
        translateService.translate(text, srcLang, targetLang, ResultListener(this, currQuery))
    }

    private inline fun onPostResult(query: CurrentQuery, action: View.() -> Unit) {
        if (query == currentQuery && !view.disposed) {
            view.action()
        }
    }

    private class ResultListener(presenter: TranslationPresenter, val currQuery: CurrentQuery) : TranslateListener {

        private val presenterRef: WeakReference<TranslationPresenter> = WeakReference(presenter)

        override fun onSuccess(translation: Translation) {
            presenterRef.get()?.onPostResult(currQuery) {
                showTranslation(translation)
            }
        }

        override fun onError(message: String, throwable: Throwable) {
            presenterRef.get()?.onPostResult(currQuery) {
                showError(message, throwable)
            }
        }
    }
}
