package cn.yiiguxing.plugin.translate.view.utils

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.util.IdeVersion
import com.google.gson.Gson
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter

open class JsQueryDispatcher : CefMessageRouterHandlerAdapter() {
    private val resources: MutableMap<String, JsQueryHandler> = HashMap()

    fun registerHandler(request: String, handler: JsQueryHandler): JsQueryDispatcher = apply {
        resources[request] = handler
    }

    fun withDefaultHandlers(): JsQueryDispatcher = apply {
        registerHandler(IdeInfoQueryHandler.REQUEST, IdeInfoQueryHandler)
        registerHandler(PluginInfoQueryHandler.REQUEST, PluginInfoQueryHandler)
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

        val handler = resources[request] ?: return false
        try {
            callback.success(handler.query(queryId, request))
        } catch (e: Throwable) {
            callback.failure(-1, "${e.javaClass}: ${e.message}")
        }

        return true
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
            val info = mapOf(
                "name" to TranslationPlugin.name,
                "adName" to TranslationPlugin.adName,
                "version" to TranslationPlugin.version,
                "changeNotes" to TranslationPlugin.descriptor.changeNotes
            )
            return Gson().toJson(info)
        }
    }
}