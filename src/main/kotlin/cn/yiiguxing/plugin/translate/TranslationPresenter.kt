package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.model.QueryResult
import cn.yiiguxing.plugin.translate.trans.TranslateService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import java.lang.ref.WeakReference

class TranslationPresenter(private val view: View) : Presenter {

    private val appStorage: AppStorage = AppStorage.instance
    private val translator: TranslateService = TranslateService.INSTANCE
    private var currentQuery: String? = null

    override val histories: List<String>
        get() = appStorage.getHistories()

    override fun getCache(query: String): QueryResult? = translator.getCache(query)

    override fun translate(query: String) {
        if (query.isBlank() || query == currentQuery) {
            return
        }

        currentQuery = query
        appStorage.addHistory(query)
        view.showStartTranslate(query)

        val presenterRef = WeakReference(this)
        translator.translate(query) { _, result ->
            ApplicationManager.getApplication().invokeLater({
                presenterRef.get()?.onPostResult(query, result)
            }, ModalityState.any())
        }
    }

    private fun onPostResult(query: String, result: QueryResult?) {
        if (query != currentQuery) {
            return
        }

        currentQuery = null
        if (result != null && result.isSuccessful) {
            view.showResult(query, result)
        } else {
            val msg = result?.message ?: "Nothing to show"
            view.showError(query, if (msg.isBlank()) "未知错误" else msg)
        }
    }
}
