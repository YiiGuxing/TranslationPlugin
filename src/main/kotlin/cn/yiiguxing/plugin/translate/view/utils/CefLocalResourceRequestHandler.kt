package cn.yiiguxing.plugin.translate.view.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.handler.CefResourceHandler
import org.cef.handler.CefResourceHandlerAdapter
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.network.CefRequest
import java.net.URL

/**
 * Handles local protocol-specific CEF resource requests for a defined `protocol` and `authority`.
 *
 * This class implements a mechanism to serve protocol-specific resources based on mappings provided
 * through the `addResource` function. Only requests matching the configured protocol and authority are processed,
 * while others are rejected.
 *
 * @param protocol The protocol to handle (e.g., "http", "file").
 * @param authority The authority of the requests (e.g., "localhost", "mydomain").
 */
open class CefLocalResourceRequestHandler(
    private val protocol: String,
    private val authority: String
) : CefResourceRequestHandlerAdapter() {
    private val resources: MutableMap<String, () -> CefResourceHandler?> = HashMap()

    fun addResource(resourcePath: String, resourceProvider: () -> CefResourceHandler?): String {
        val normalisedPath = resourcePath.trim('/')
        resources[normalisedPath] = resourceProvider
        return "$protocol://$authority/$normalisedPath"
    }

    fun addResource(resourcePath: String, mimeType: String, parent: Disposable): String {
        return addResource(resourcePath) {
            CefLocalResourceRequestHandler::class.java.getResourceAsStream(resourcePath)?.let {
                CefStreamResourceHandler(it, mimeType, parent)
            }
        }
    }

    override fun getResourceHandler(
        browser: CefBrowser?,
        frame: CefFrame?,
        request: CefRequest
    ): CefResourceHandler? {
        val url = URL(request.url)
        if (!url.protocol.equals(protocol) || !url.authority.equals(authority)) {
            return null
        }

        return try {
            val path = url.path.trim('/')
            resources[path]?.let { it() } ?: RejectingResourceHandler
        } catch (e: RuntimeException) {
            logger<CefLocalResourceRequestHandler>().trace(e)
            RejectingResourceHandler
        }
    }

    private object RejectingResourceHandler : CefResourceHandlerAdapter() {
        override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {
            callback.cancel()
            return false
        }
    }
}
