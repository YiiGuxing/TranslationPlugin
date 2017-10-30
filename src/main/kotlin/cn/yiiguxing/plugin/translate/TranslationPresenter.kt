package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.model.QueryResult
import cn.yiiguxing.plugin.translate.trans.TranslateService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import java.lang.ref.WeakReference

class TranslationPresenter(private val view: View) : Presenter {

    private val mAppStorage: AppStorage = AppStorage.instance

    private val mTranslator: TranslateService = TranslateService.INSTANCE

    private var mCurrentQuery: String? = null

    override val histories: List<String>
        get() = mAppStorage.getHistories()

    override fun getCache(query: String): QueryResult? = mTranslator.getCache(query)

    override fun translate(query: String) {
        if (query.isBlank() || query == mCurrentQuery)
            return

        mCurrentQuery = query
        mAppStorage.addHistory(query)
        view.showStartTranslate(query)

        val presenterRef = WeakReference(this)
        mTranslator.translate(query) { _, result ->
            ApplicationManager.getApplication().invokeLater({
                presenterRef.get()?.onPostResult(query, result)
            }, ModalityState.any())
        }
    }

    private fun onPostResult(query: String, result: QueryResult?) {
        if (query != mCurrentQuery)
            return

        mCurrentQuery = null
        if (result != null && result.isSuccessful) {
            view.showResult(query, result)
        } else {
            val msg = result?.message ?: "Nothing to show"
            view.showError(query, if (msg.isBlank()) "未知错误" else msg)
        }
    }
}
