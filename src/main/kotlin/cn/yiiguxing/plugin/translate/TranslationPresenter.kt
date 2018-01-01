package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.TranslateListener
import cn.yiiguxing.plugin.translate.trans.TranslateService
import cn.yiiguxing.plugin.translate.trans.Translation
import java.lang.ref.WeakReference

class TranslationPresenter(private val view: View) : Presenter {

    private val appStorage: AppStorage = AppStorage.instance
    private val translateService: TranslateService = TranslateService.INSTANCE
    private var currentQuery: CurrentQuery? = null

    override val histories: List<String> get() = appStorage.getHistories()

    override val primaryLanguage: Lang get() = translateService.translator.primaryLanguage

    override val supportedLanguages: SupportedLanguages
        get() = with(translateService.translator) {
            SupportedLanguages(supportedSourceLanguages, supportedTargetLanguages)
        }

    data class CurrentQuery(val srcLang: Lang, val targetLang: Lang, val text: String)

    override fun getCache(srcLang: Lang, targetLang: Lang, text: String): Translation? {
        return translateService.getCache(srcLang, targetLang, text)
    }

    override fun translate(srcLang: Lang, targetLang: Lang, text: String) {
        val currQuery = CurrentQuery(srcLang, targetLang, text)
        if (text.isBlank() || currQuery == currentQuery) {
            return
        }

        currentQuery = currQuery
        appStorage.addHistory(text)
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
