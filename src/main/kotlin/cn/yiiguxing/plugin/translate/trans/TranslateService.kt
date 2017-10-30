package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.model.QueryResult
import cn.yiiguxing.plugin.translate.util.LruCache
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager


/**
 * TranslateService
 *
 * Created by Yii.Guxing on 2017/10/30
 */
class TranslateService private constructor() {

    val settings: Settings = Settings.instance
    var translator: Translator = YoudaoTranslator()
    val cache = LruCache<CacheKey, Translation>(500)

    companion object {
        val INSTANCE: TranslateService
            get() = ServiceManager.getService(TranslateService::class.java)
    }

    fun getCache(text: String, srcLang: Lang? = null, targetLang: Lang? = null): QueryResult? {
        if (text.isBlank()) return null

        return null
    }

    fun translate(text: String, targetLang: Lang? = null, callback: (String, QueryResult?) -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            translator.translate(text, Lang.AUTO, Lang.AUTO)
        }
    }

}