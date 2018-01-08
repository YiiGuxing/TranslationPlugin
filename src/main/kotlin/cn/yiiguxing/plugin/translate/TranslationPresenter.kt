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

    data class CurrentQuery(val text: String, val srcLang: Lang, val targetLang: Lang)

    override fun getCache(text: String, srcLang: Lang, targetLang: Lang): Translation? {
        return translateService.getCache(text, srcLang, targetLang)
    }

    override fun translate(text: String, srcLang: Lang, targetLang: Lang) {
        val currQuery = CurrentQuery(text, srcLang, targetLang)
        if (text.isBlank() || currQuery == currentQuery) {
            return
        }

        currentQuery = currQuery
        with(appStorage) {
            addHistory(text)
            lastSourceLanguage = srcLang
            lastTargetLanguage = targetLang
        }
        view.showStartTranslate(text)

        val presenterRef = WeakReference(this)
        translateService.translate(text, srcLang, targetLang, object : TranslateListener {
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
        })
    }

    private inline fun onPostResult(query: CurrentQuery, action: View.() -> Unit) {
        if (query == currentQuery && !view.disposed) {
            view.action()
        }
    }
}
