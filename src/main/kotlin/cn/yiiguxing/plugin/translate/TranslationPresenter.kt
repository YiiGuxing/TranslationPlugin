package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.model.QueryResult
import cn.yiiguxing.plugin.translate.trans.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import java.lang.ref.WeakReference

class TranslationPresenter(private val view: View) : Presenter {

    private val appStorage: AppStorage = AppStorage.instance
    private val translator: TranslateService = TranslateService.INSTANCE
    private var currentQuery: String? = null

    override val histories: List<String>
        get() = appStorage.getHistories()

    override fun getCache(text: String): Translation? = translator.getCache(text)

    override fun translate(text: String) {
        if (text.isBlank() || text == currentQuery) {
            return
        }

        currentQuery = text
        appStorage.addHistory(text)
        view.showStartTranslate(text)

        val presenterRef = WeakReference(this)
        translator.translate(text) { _, result ->
            ApplicationManager.getApplication().invokeLater({
                presenterRef.get()?.onPostResult(text, result)
            }, ModalityState.any())
        }
    }

    private fun onPostResult(query: String, result: QueryResult?) {
        if (query != currentQuery || view.disposed) {
            return
        }

        currentQuery = null
        if (result != null && result.isSuccessful) {
            val dictionaries = listOf(
                    Dict("动词", entries = listOf(
                            DictEntry("显示", listOf("display", "show", "demonstrate", "illustrate")),
                            DictEntry("陈列", listOf("display", "exhibit", "set out")),
                            DictEntry("展出", listOf("display", "exhibit", "be on show")),
                            DictEntry("展览", listOf("exhibit", "display")),
                            DictEntry("display", listOf("显示", "陈列", "展出", "展览")),
                            DictEntry("表现",
                                    listOf("show", "express", "behave", "display", "represent", "manifest")),
                            DictEntry("陈设", listOf("display", "furnish", "set out")),
                            DictEntry("陈设2", listOf("display", "furnish", "set out"))
                    )),
                    Dict("名词", entries = listOf(
                            DictEntry("显示", listOf("display")),
                            DictEntry("表现", listOf("performance", "show", "expression", "manifestation",
                                    "representation", "display")),
                            DictEntry("炫耀", listOf("display")),
                            DictEntry("橱窗", listOf("showcase", "show window", "display", "shopwindow",
                                    "glass-fronted billboard")),
                            DictEntry("罗", listOf("silk", "net", "display", "shift"))
                    ))
            )

            val trans = Translation(
                    "If baby only wanted to, he could fly up to heaven this moment. It is not for nothing that he does not leave us.",
                    "显示",
                    Lang.ENGLISH,
                    Lang.CHINESE,
                    Symbol("dɪ'spleɪ", "xiǎn shì"),
                    dictionaries
            )

            view.showTranslation(query, trans)
        } else {
            val msg = result?.message ?: "Nothing to show"
            view.showError(query, if (msg.isBlank()) "未知错误" else msg)
        }
    }
}
