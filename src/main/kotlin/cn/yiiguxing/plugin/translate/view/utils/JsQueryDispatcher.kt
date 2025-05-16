package cn.yiiguxing.plugin.translate.view.utils

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.update.Version
import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.view.WebPages
import com.google.gson.Gson
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter
import java.util.*

open class JsQueryDispatcher : CefMessageRouterHandlerAdapter() {
    private val requestHandlerMap: MutableMap<String, JsQueryHandler> = HashMap()
    private var handler: JsQueryHandler? = null

    fun setHandler(handler: JsQueryHandler) {
        this.handler = handler
    }

    fun registerHandlerWithRequest(request: String, handler: JsQueryHandler): JsQueryDispatcher = apply {
        requestHandlerMap[request] = handler
    }

    fun withDefaultHandlers(): JsQueryDispatcher = apply {
        registerHandlerWithRequest(IdeInfoQueryHandler.REQUEST, IdeInfoQueryHandler)
        registerHandlerWithRequest(PluginInfoQueryHandler.REQUEST, PluginInfoQueryHandler)
    }

    override fun onQuery(
        browser: CefBrowser,
        frame: CefFrame,
        queryId: Long,
        request: String?,
        persistent: Boolean,
        callback: CefQueryCallback
    ): Boolean {
        if (request == null) {
            return false
        }

        requestHandlerMap[request]?.let {
            handleQuery(it, queryId, request, callback)
            return true
        }
        handler?.let {
            handleQuery(it, queryId, request, callback)
            return true
        }

        return false
    }

    private fun handleQuery(handler: JsQueryHandler, queryId: Long, request: String, callback: CefQueryCallback) {
        try {
            callback.success(handler.query(queryId, request))
        } catch (e: Throwable) {
            callback.failure(-1, "${e.javaClass}: ${e.message}")
        }
    }


    fun interface JsQueryHandler {
        fun query(id: Long, request: String): String
    }


    object IdeInfoQueryHandler : JsQueryHandler {
        const val REQUEST = "ide-info"

        override fun query(id: Long, request: String): String {
            val applicationInfo = ApplicationInfo.getInstance()
            val applicationNamesInfo = ApplicationNamesInfo.getInstance()
            val buildNumber = IdeVersion.buildNumber
            val info = mapOf(
                "productName" to applicationNamesInfo.fullProductName,
                "editionName" to applicationNamesInfo.editionName,
                "fullApplicationName" to applicationInfo.fullApplicationName,
                "buildNumber" to buildNumber.asString(),
                "productCode" to buildNumber.productCode,
                "shortVersion" to applicationInfo.shortVersion,
                "fullVersion" to applicationInfo.fullVersion
            )
            return Gson().toJson(info)
        }
    }

    object PluginInfoQueryHandler : JsQueryHandler {
        const val REQUEST = "plugin-info"

        override fun query(id: Long, request: String): String {
            val version = Version(TranslationPlugin.version)
            val info = mapOf(
                "name" to TranslationPlugin.name,
                "adName" to TranslationPlugin.adName,
                "version" to TranslationPlugin.version,
                "featureVersion" to version.getFeatureUpdateVersion(),
                "changeNotes" to TranslationPlugin.descriptor.changeNotes,
                "gettingStartedUrl" to WebPages.docs().getUrl(),
                "whatsNewUrl" to WebPages.releaseNote(version.getFeatureUpdateVersion()).getUrl(),
                "historicalChangesUrl" to WebPages.updates().getUrl(),
                "sponsorsPageUrl" to WebPages.getSponsorsPageUrl(),
                "language" to Locale.getDefault().toLanguageTag(),
            )
            return Gson().toJson(info)
        }
    }
}