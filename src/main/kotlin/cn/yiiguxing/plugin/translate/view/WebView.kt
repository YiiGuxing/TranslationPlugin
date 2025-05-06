package cn.yiiguxing.plugin.translate.view

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.UrlTrackingParametersProvider
import cn.yiiguxing.plugin.translate.view.utils.*
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.jcef.JBCefBrowserBase.Properties.NO_CONTEXT_MENU
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.intellij.util.ui.UIUtil
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.handler.*
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
    private var lastRequestedUrl: String = ""

    companion object {
        private val LOG = logger<WebView>()

        private const val PROTOCOL = "http"
        private const val HOST_NAME = "itp"

        private const val LOADING_PATH = "/web/loading.html"
        private const val ERROR_PAGE_PATH = "/web/error.html"
        private const val BASE_CSS_PATH = "/base.css"
        private const val SCROLLBARS_CSS_PATH = "/scrollbars.css"

        private const val JS_FUNCTION_NAME: String = "itpCefQuery"

        // com.intellij.ui.jcef.JBCefFileSchemeHandlerFactory
        private const val LOAD_HTML_URL_PREFIX = "file:///jbcefbrowser/"

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
        val loadingUrl = resourceRequestHandler.addResource(LOADING_PATH, "text/html", this)
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
            ): Boolean {
                synchronized(this@WebView) {
                    lastRequestedUrl = request.url ?: ""
                }

                return if (userGesture) {
                    browse(request.url)
                    true
                } else false
            }
        }, contentPanel.cefBrowser)
        jbCefClient.addRequestHandler(resourceRequestHandler, contentPanel.cefBrowser)
        jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadError(
                browser: CefBrowser,
                frame: CefFrame,
                errorCode: CefLoadHandler.ErrorCode,
                errorText: String?,
                failedUrl: String
            ) {
                if (lastRequestedUrl != failedUrl) {
                    return
                }

                val html = ERROR_PAGE_READER?.replace("{{url}}", failedUrl) ?: return
                UIUtil.invokeLaterIfNeeded {
                    synchronized(this@WebView) {
                        if (lastRequestedUrl == failedUrl) {
                            contentPanel.loadHTML(html, failedUrl)
                        }
                    }
                }
            }
        }, contentPanel.cefBrowser)
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

        contentPanel.setProperty(NO_CONTEXT_MENU, true)
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

    override fun getTabActions(): ActionGroup {
        val group = DefaultActionGroup()
        group.add(ReloadAction())
        group.addSeparator()
        group.add(BackAction())
        group.add(ForwardAction())
        return group
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


    private inner class ReloadAction :
        AnAction({ message("webview.reload.action.name") }, AllIcons.Actions.Refresh) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun actionPerformed(e: AnActionEvent) {
            val url = contentPanel.cefBrowser.url
            if (url?.startsWith(LOAD_HTML_URL_PREFIX) == true) {
                url.split("#url=").getOrNull(1)?.let {
                    contentPanel.cefBrowser.loadURL(it)
                    return
                }
            }

            contentPanel.cefBrowser.reload()
        }
    }

    private inner class BackAction :
        AnAction({ message("webview.go.back.action.name") }, AllIcons.Actions.Back) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun actionPerformed(e: AnActionEvent) = contentPanel.cefBrowser.goBack()
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = contentPanel.cefBrowser.canGoBack()
        }
    }

    private inner class ForwardAction :
        AnAction({ message("webview.forward.action.name") }, AllIcons.Actions.Forward) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun actionPerformed(e: AnActionEvent) = contentPanel.cefBrowser.goForward()
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = contentPanel.cefBrowser.canGoForward()
        }
    }
}