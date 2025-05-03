package cn.yiiguxing.plugin.translate.view

import cn.yiiguxing.plugin.translate.util.UrlTrackingParametersProvider
import cn.yiiguxing.plugin.translate.view.utils.*
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.jcef.JCEFHtmlPanel
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import org.jetbrains.annotations.Nls
import java.beans.PropertyChangeListener
import java.io.ByteArrayInputStream
import javax.swing.JComponent

internal class WebView(
    private val project: Project,
    private val file: LightVirtualFile,
    url: String,
) : UserDataHolderBase(), FileEditor {

    private val contentPanel = JCEFHtmlPanel(true, null, null)

    companion object {
        private val LOG = logger<WebView>()

        private const val PROTOCOL = "http"
        private const val HOST_NAME = "itp"

        private const val LOADING_PATH = "/loading.html"
        private const val ERROR_PAGE_PATH = "/web/error.html"
        private const val BASE_CSS_PATH = "/base.css"
        private const val SCROLLBARS_CSS_PATH = "/scrollbars.css"

        private const val JS_FUNCTION_NAME: String = "itpCefQuery"

        private val ERROR_PAGE_READER: String? by lazy {
            try {
                WebView::class.java.getResourceAsStream(ERROR_PAGE_PATH)
                    ?.use { it.reader().readText() }
            } catch (e: Exception) {
                LOG.warn("couldn't find $ERROR_PAGE_PATH", e)
                null
            }
        }
    }

    init {
        val jbCefClient = contentPanel.jbCefClient
        val resourceRequestHandler = CefLocalRequestHandler(PROTOCOL, HOST_NAME)
        val loadingUrl = resourceRequestHandler.addWebResource(LOADING_PATH, "text/html", this)
        resourceRequestHandler.addResource(SCROLLBARS_CSS_PATH) {
            CefStreamResourceHandler(
                ByteArrayInputStream(
                    JBCefScrollbarsHelper.buildScrollbarsStyle().toByteArray(Charsets.UTF_8)
                ), "text/css", this
            )
        }
        resourceRequestHandler.addResource(BASE_CSS_PATH) {
            CefStreamResourceHandler(
                ByteArrayInputStream(
                    CefStylesheetHelper.buildBaseStyle().toByteArray(Charsets.UTF_8)
                ), "text/css", this
            )
        }

        // must be called before add `resourceRequestHandler`
        jbCefClient.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: CefBrowser,
                frame: CefFrame,
                request: CefRequest,
                userGesture: Boolean,
                isRedirect: Boolean
            ): Boolean = if (userGesture) {
                browse(request.url)
                true
            } else false
        }, contentPanel.cefBrowser)
        jbCefClient.addRequestHandler(resourceRequestHandler, contentPanel.cefBrowser)
        jbCefClient.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onStatusMessage(browser: CefBrowser, text: String) {
                StatusBar.Info.set(text, project)
            }
        }, contentPanel.cefBrowser)
        jbCefClient.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(
                browser: CefBrowser,
                frame: CefFrame,
                targetUrl: String,
                targetFrameName: String?
            ): Boolean {
                browse(targetUrl)
                return true
            }
        }, contentPanel.cefBrowser)

        val config = CefMessageRouter.CefMessageRouterConfig(JS_FUNCTION_NAME, "${JS_FUNCTION_NAME}Cancel")
        val jsRouter = CefMessageRouter.create(config)
        val jsQuery = JsQueryDispatcher()
            .withDefaultHandlers()
            .registerHandler("page-url") { _, _ -> url }
        jsRouter.addHandler(jsQuery, true)
        jbCefClient.cefClient.addMessageRouter(jsRouter)

        contentPanel.setErrorPage { _, _, failedUrl ->
            ERROR_PAGE_READER?.replace("{{url}}", failedUrl)
        }

        contentPanel.loadURL(loadingUrl)
    }


    private fun browse(url: String) {
        val targetUrl = if (url.startsWith(WebPages.BASE_URL)) {
            UrlTrackingParametersProvider.augmentIdeUrl(url, "compact" to false.toString())
        } else {
            UrlTrackingParametersProvider.augmentUrl(url)
        }
        BrowserUtil.browse(targetUrl)
    }


    override fun getComponent(): JComponent = contentPanel.component
    override fun getPreferredFocusedComponent(): JComponent = contentPanel.component
    override fun getName(): @Nls(capitalization = Nls.Capitalization.Title) String = "WebView"
    override fun getFile(): VirtualFile = file
    override fun setState(state: FileEditorState) = Unit
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = true
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit
    override fun dispose() = Unit
}