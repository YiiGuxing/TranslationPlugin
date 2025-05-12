package cn.yiiguxing.plugin.translate.view

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.UrlTrackingParametersProvider
import cn.yiiguxing.plugin.translate.view.WebViewProvider.LoadingPageStrategy
import cn.yiiguxing.plugin.translate.view.utils.JsQueryDispatcher
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.jcef.JBCefBrowserBase.Properties.NO_CONTEXT_MENU
import com.intellij.ui.jcef.JCEFHtmlPanel
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.handler.*
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import org.jetbrains.annotations.Nls
import java.beans.PropertyChangeListener
import javax.swing.JComponent

internal class WebView(
    private val project: Project,
    private val file: LightVirtualFile,
    request: WebViewProvider.Request,
) : UserDataHolderBase(), FileEditor {

    private val contentPanel = JCEFHtmlPanel(true, null, null)
    private val itpResources = ITP.createCefResources(this)
    private var lastRequestedUrl: String = ""

    companion object {
        private const val DEVTOOLS = false
        private const val JS_FUNCTION_NAME: String = "itpCefQuery"
    }

    init {
        val jbCefClient = contentPanel.jbCefClient

        // must be called before add `itpResources`
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
                return false
            }
        }, contentPanel.cefBrowser)
        val originRequest = request
        jbCefClient.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun getResourceRequestHandler(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest,
                isNavigation: Boolean,
                isDownload: Boolean,
                requestInitiator: String?,
                disableDefaultHandling: BoolRef?
            ): CefResourceRequestHandler? {
                return originRequest.resourceRequestHandler?.invoke(browser, frame, request, this@WebView)
                    ?: itpResources
            }
        }, contentPanel.cefBrowser)
        jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadError(
                browser: CefBrowser,
                frame: CefFrame,
                errorCode: CefLoadHandler.ErrorCode,
                errorText: String?,
                failedUrl: String
            ) {
                if (!frame.isMain) {
                    return
                }
                synchronized(this@WebView) {
                    if (lastRequestedUrl == failedUrl && !itpResources.isErrorPage(failedUrl)) {
                        loadWithReplace(itpResources.getErrorPage(failedUrl))
                    }
                }
            }
        }, contentPanel.cefBrowser)
        jbCefClient.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onStatusMessage(browser: CefBrowser, text: String) {
                StatusBar.Info.set(text, project)
            }
        }, contentPanel.cefBrowser)

        val hyperlinkHandler = ITP.createCefHyperlinkHandler(project) {
            browse(it)
            true
        }
        jbCefClient.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(
                browser: CefBrowser,
                frame: CefFrame,
                targetUrl: String,
                targetFrameName: String?
            ): Boolean = hyperlinkHandler.onHyperlinkActivated(targetUrl)
        }, contentPanel.cefBrowser)

        val config = CefMessageRouter.CefMessageRouterConfig(JS_FUNCTION_NAME, "${JS_FUNCTION_NAME}Cancel")
        val jsRouter = CefMessageRouter.create(config)
        val jsQuery = JsQueryDispatcher()
            .withDefaultHandlers()
            .registerHandlerWithRequest("page-url") { _, _ -> request.url }
        request.queryHandler?.let { jsQuery.setHandler(it) }
        jsRouter.addHandler(jsQuery, true)
        jbCefClient.cefClient.addMessageRouter(jsRouter)

        contentPanel.setProperty(NO_CONTEXT_MENU, true)

        val targetUrl = when (request.loadingPageStrategy) {
            LoadingPageStrategy.AUTO -> if (itpResources.isMyResource(request.url)) {
                request.url
            } else {
                itpResources.loadingPage
            }

            LoadingPageStrategy.ALWAYS -> itpResources.loadingPage
            LoadingPageStrategy.SKIP -> request.url
        }
        contentPanel.loadURL(targetUrl)
    }

    private fun loadWithReplace(url: String) {
        val code = "window.location.replace('$url');"
        contentPanel.cefBrowser.executeJavaScript(code, null, 0)
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

        if (DEVTOOLS) {
            group.add(object : AnAction("DevTools") {
                override fun actionPerformed(e: AnActionEvent) {
                    contentPanel.openDevtools()
                }
            })
        }

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
            itpResources.getFailedUrl(contentPanel.cefBrowser.url)?.let {
                loadWithReplace(it)
                return
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