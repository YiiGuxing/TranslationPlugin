package cn.yiiguxing.plugin.translate.view

import cn.yiiguxing.plugin.translate.RegistryKeys
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.UrlTrackingParametersProvider
import cn.yiiguxing.plugin.translate.view.WebViewProvider.LoadingPageStrategy
import cn.yiiguxing.plugin.translate.view.utils.CefStylesheetHelper
import cn.yiiguxing.plugin.translate.view.utils.JsQueryDispatcher
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.registry.RegistryManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLayeredPane
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.jcef.JBCefBrowserBase.Properties.NO_CONTEXT_MENU
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.intellij.util.ui.EdtInvocationManager
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.handler.*
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.Color
import java.beans.PropertyChangeListener
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent


internal class WebView(
    private val project: Project,
    private val file: LightVirtualFile,
    request: WebViewProvider.Request,
) : UserDataHolderBase(), FileEditor {

    companion object {
        private const val JS_FUNCTION_NAME: String = "itpCefQuery"
    }

    private val cefPanel = JCEFHtmlPanel(true, null, null)
    private val itpResources = ITP.createCefResources(this)
    private val jsRouter: CefMessageRouter
    private val requestHandler: CefRequestHandler
    private var lastRequestedUrl: String = ""
    private val isLoaded = AtomicBoolean(false)
    private val contentPanel = MyContentPanel(cefPanel.component, this)


    init {
        Disposer.register(this, cefPanel)
        contentPanel.background = CefStylesheetHelper.BACKGROUND

        val jbCefClient = cefPanel.jbCefClient
        requestHandler = object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: CefBrowser,
                frame: CefFrame,
                cefRequest: CefRequest,
                userGesture: Boolean,
                isRedirect: Boolean
            ): Boolean {
                synchronized(this@WebView) {
                    lastRequestedUrl = cefRequest.url ?: ""
                }
                return false
            }

            override fun getResourceRequestHandler(
                browser: CefBrowser?,
                frame: CefFrame?,
                cefRequest: CefRequest,
                isNavigation: Boolean,
                isDownload: Boolean,
                requestInitiator: String?,
                disableDefaultHandling: BoolRef?
            ): CefResourceRequestHandler? {
                return request.resourceRequestHandler
                    ?.invoke(browser, frame, cefRequest, this@WebView)
                    ?: itpResources
            }
        }
        jbCefClient.addRequestHandler(requestHandler, cefPanel.cefBrowser)
        jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(
                browser: CefBrowser,
                isLoading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean
            ) {
                if (!isLoading) {
                    stopLoading()
                }
            }

            override fun onLoadError(
                browser: CefBrowser,
                frame: CefFrame,
                errorCode: CefLoadHandler.ErrorCode,
                errorText: String?,
                failedUrl: String
            ) {
                if (frame.isMain) {
                    synchronized(this@WebView) {
                        if (lastRequestedUrl == failedUrl && !itpResources.isErrorPage(failedUrl)) {
                            loadWithReplace(itpResources.getErrorPage(failedUrl))
                        }
                    }
                }
            }
        }, cefPanel.cefBrowser)
        jbCefClient.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onStatusMessage(browser: CefBrowser, text: String) {
                StatusBar.Info.set(text, project)
            }
        }, cefPanel.cefBrowser)

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
        }, cefPanel.cefBrowser)

        val config = CefMessageRouter.CefMessageRouterConfig(JS_FUNCTION_NAME, "${JS_FUNCTION_NAME}Cancel")
        jsRouter = CefMessageRouter.create(config)
        val jsQuery = JsQueryDispatcher()
            .withDefaultHandlers()
            .registerHandlerWithRequest("page-url") { _, _ -> request.url }
        request.queryHandler?.let { jsQuery.setHandler(it) }
        jsRouter.addHandler(jsQuery, true)
        jbCefClient.cefClient.addMessageRouter(jsRouter)

        cefPanel.setProperty(NO_CONTEXT_MENU, true)
        contentPanel.startLoading()

        val targetUrl = when (request.loadingPageStrategy) {
            LoadingPageStrategy.AUTO -> if (itpResources.isMyResource(request.url)) {
                request.url
            } else {
                itpResources.loadingPage
            }

            LoadingPageStrategy.ALWAYS -> itpResources.loadingPage
            LoadingPageStrategy.SKIP -> request.url
        }
        cefPanel.loadURL(targetUrl)
    }

    private fun stopLoading() {
        if (isLoaded.compareAndSet(false, true)) {
            EdtInvocationManager.getInstance().invokeLater {
                contentPanel.stopLoading()
            }
        }
    }

    private fun loadWithReplace(url: String) {
        val code = "window.location.replace('$url');"
        cefPanel.cefBrowser.executeJavaScript(code, null, 0)
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

        if (RegistryManager.getInstance().`is`(RegistryKeys.WEBVIEW_DEBUG)) {
            group.addSeparator()
            group.add(object : AnAction("DevTools") {
                override fun actionPerformed(e: AnActionEvent) {
                    cefPanel.openDevtools()
                }
            })
        }

        return group
    }

    override fun dispose() {
        cefPanel.jbCefClient.let {
            it.removeRequestHandler(requestHandler, cefPanel.cefBrowser)
            it.cefClient.removeMessageRouter(jsRouter)
        }
        jsRouter.dispose()
    }


    override fun getComponent(): JComponent = contentPanel
    override fun getPreferredFocusedComponent(): JComponent = contentPanel
    override fun getName(): @Nls(capitalization = Nls.Capitalization.Title) String = "WebView"
    override fun getFile(): VirtualFile = file
    override fun setState(state: FileEditorState) = Unit
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = true
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit


    private class MyContentPanel(private val cefPanel: JComponent, parent: Disposable) : JBLayeredPane() {

        private val loadingDisposable = Disposer.newDisposable(parent)
        private var loadingPanel: JBLoadingPanel? = JBLoadingPanel(BorderLayout(), loadingDisposable)

        init {
            add(cefPanel, DEFAULT_LAYER, 0)
            add(loadingPanel, DRAG_LAYER, 1)
        }

        override fun setBackground(color: Color) {
            super.setBackground(color)
            loadingPanel?.background = color
            cefPanel.background = color
        }

        fun startLoading() {
            loadingPanel?.startLoading() ?: throw IllegalStateException("Loading panel is disposed")
        }

        fun stopLoading() {
            loadingPanel?.let {
                it.stopLoading()
                remove(it)
                Disposer.dispose(loadingDisposable)
                loadingPanel = null

                revalidate()
                repaint()
            }
        }

        override fun doLayout() {
            super.doLayout()
            cefPanel.setBounds(0, 0, width, height)
            loadingPanel?.setBounds(0, 0, width, height)
        }
    }

    private inner class ReloadAction :
        AnAction({ message("webview.reload.action.name") }, AllIcons.Actions.Refresh) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun actionPerformed(e: AnActionEvent) {
            itpResources.getFailedUrl(cefPanel.cefBrowser.url)?.let {
                loadWithReplace(it)
                return
            }

            cefPanel.cefBrowser.reload()
        }
    }

    private inner class BackAction :
        AnAction({ message("webview.go.back.action.name") }, AllIcons.Actions.Back) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun actionPerformed(e: AnActionEvent) = cefPanel.cefBrowser.goBack()
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = cefPanel.cefBrowser.canGoBack()
        }
    }

    private inner class ForwardAction :
        AnAction({ message("webview.forward.action.name") }, AllIcons.Actions.Forward) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun actionPerformed(e: AnActionEvent) = cefPanel.cefBrowser.goForward()
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = cefPanel.cefBrowser.canGoForward()
        }
    }
}