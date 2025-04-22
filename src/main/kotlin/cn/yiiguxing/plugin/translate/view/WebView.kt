package cn.yiiguxing.plugin.translate.view

import cn.yiiguxing.plugin.translate.TranslationPlugin
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.jcef.JCEFHtmlPanel
import org.apache.http.client.utils.URIBuilder
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import org.jetbrains.annotations.Nls
import java.beans.PropertyChangeListener
import java.net.URISyntaxException
import javax.swing.JComponent

internal class WebView(
    private val project: Project,
    private val file: LightVirtualFile,
    html: String,
    errorHtml: String? = null
) :
    UserDataHolderBase(),
    FileEditor {

    private val contentPanel = JCEFHtmlPanel(true, null, null)

    init {
        val cefClient = contentPanel.jbCefClient
        cefClient.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onStatusMessage(browser: CefBrowser, text: String) {
                StatusBar.Info.set(text, project)
            }
        }, contentPanel.cefBrowser)
        cefClient.addRequestHandler(object : CefRequestHandlerAdapter() {
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
        cefClient.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
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

        if (errorHtml != null) {
            contentPanel.setErrorPage { errorCode, errorText, failedUrl -> errorHtml }
        }

        contentPanel.loadHTML(html)
    }


    private fun browse(url: String) {
        val augmented = if (!url.startsWith(WebPages.BASE_URL)) try {
            URIBuilder(url)
                .setParameter("utm_source", "plugin")
                .setParameter("utm_medium", "link")
                .setParameter("utm_campaign", TranslationPlugin.adName)
                .setParameter("utm_content", TranslationPlugin.version)
                .build()
                .toString()
        } catch (e: URISyntaxException) {
            thisLogger().warn(url, e);
            null
        } else null

        BrowserUtil.browse(augmented ?: url)
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