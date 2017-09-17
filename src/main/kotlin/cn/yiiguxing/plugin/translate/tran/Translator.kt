package cn.yiiguxing.plugin.translate.tran

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.YOUDAO_TRANSLATE_URL
import cn.yiiguxing.plugin.translate.model.QueryResult
import cn.yiiguxing.plugin.translate.util.LruCache
import cn.yiiguxing.plugin.translate.util.md5
import cn.yiiguxing.plugin.translate.util.urlEncode
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import java.util.concurrent.Future

/**
 * 翻译器
 */
class Translator private constructor() {

    private val mSettings = Settings.instance
    private val mCache = LruCache<CacheKey, QueryResult>(500)
    private var mCurrentTask: Future<*>? = null

    /**
     * 获取缓存
     */
    fun getCache(key: CacheKey) = mCache[key]

    /**
     * 查询翻译
     *
     * @param query    目标字符串
     * @param callback 回调
     */
    fun translate(query: String, callback: (query: String, result: QueryResult?) -> Unit) {
        if (query.isBlank()) {
            callback(query, null)
            return
        }

        mCurrentTask?.apply {
            if (!isDone) cancel(false)
        }
        mCurrentTask = null

        val langFrom = mSettings.langFrom ?: Lang.AUTO
        val langTo = mSettings.langTo ?: Lang.AUTO

        mCache[CacheKey(langFrom, langTo, query)]?.let {
            callback(query, it)
            return
        }

        mCurrentTask = ApplicationManager
                .getApplication()
                .executeOnPooledThread(QueryRequest(langFrom, langTo, query, callback))
    }

    private fun getQueryUrl(langFrom: Lang, langTo: Lang, query: String): String {
        val settings = mSettings
        val appId = settings.appId
        val privateKey = settings.appPrivateKey
        val salt = System.currentTimeMillis().toString()
        val sign = (appId + query + salt + privateKey).md5()

        return "$YOUDAO_TRANSLATE_URL?appKey=${appId.urlEncode()}" +
                "&from=${langFrom.code}" +
                "&to=${langTo.code}" +
                "&salt=$salt" +
                "&sign=$sign" +
                "&q=${query.urlEncode()}"
    }

    private inner class QueryRequest(
            private val langFrom: Lang,
            private val langTo: Lang,
            private val query: String,
            private val callback: (query: String, result: QueryResult?) -> Unit
    ) : Runnable {

        override fun run() {
            val query = query

            val result: QueryResult = try {
                val url = getQueryUrl(langFrom, langTo, query)
                LOGGER.info("query url: $url")

                val json = HttpRequests.request(url).readString(null)
                LOGGER.info(json)

                if (json.isNotBlank())
                    Gson().fromJson(json, QueryResult::class.java)
                else
                    QueryResult(errorCode = QueryResult.CODE_ERROR)
            } catch (e: JsonSyntaxException) {
                LOGGER.warn(e)

                QueryResult(errorCode = QueryResult.CODE_JSON_SYNTAX_ERROR)
            } catch (e: Exception) {
                LOGGER.warn(e)

                QueryResult(errorCode = QueryResult.CODE_ERROR, message = e.message)
            }

            result.apply {
                checkError()
                if (isSuccessful) {
                    mCache.put(CacheKey(langFrom, langTo, query), this)
                }
                if (this.query.isNullOrBlank()) {
                    this.query = query
                }
            }

            println("query: " + query)
            println("result: " + result)

            callback(query, result)
        }
    }

    companion object {

        private val LOGGER = Logger.getInstance("#" + Translator::class.java.canonicalName)

        /**
         * @return [Translator] 的实例
         */
        val instance: Translator
            get() = ServiceManager.getService(Translator::class.java)
    }

}
