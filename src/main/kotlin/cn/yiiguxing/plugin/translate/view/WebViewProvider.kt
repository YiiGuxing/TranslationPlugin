package cn.yiiguxing.plugin.translate.view

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.view.utils.JsQueryDispatcher
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileTypes.ex.FakeFileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.jcef.JBCefApp
import icons.TranslationIcons
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefResourceRequestHandler
import org.cef.network.CefRequest
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class WebViewProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean =
        JBCefApp.isSupported() && file.fileType === WebViewFileType

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        file.getUserData(WEBVIEW_KEY) ?: WebView(project, file as LightVirtualFile, REQUEST_KEY.get(file)).also {
            file.putUserData(WEBVIEW_KEY, it)
        }

    override fun getEditorTypeId(): @NonNls String = "translation.webview"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR


    private object WebViewFileType : FakeFileType() {
        override fun isMyFileType(file: VirtualFile): Boolean = file.fileType === this
        override fun getName(): @NonNls String = "TranslationWebView"
        override fun getDescription(): @NlsContexts.Label String = "Translation webview"
        override fun getIcon(): Icon = TranslationIcons.Logo
    }

    @Suppress("CompanionObjectInExtension")
    companion object {
        @JvmStatic
        private val REQUEST_KEY: Key<Request> = Key.create("translation.webview.request.key")

        @JvmStatic
        private val WEBVIEW_KEY: Key<FileEditor> = Key.create("translation.webview.component.key")


        @JvmStatic
        fun open(
            project: Project,
            url: String,
            @DialogTitle title: String = TranslationPlugin.name,
        ): Boolean {
            return open(project, Request(url), title)
        }

        @JvmStatic
        fun open(
            project: Project,
            request: Request,
            @DialogTitle title: String = TranslationPlugin.name,
        ): Boolean {
            if (!project.isDefault && !project.isDisposed && JBCefApp.isSupported()) {
                val file = object : LightVirtualFile(title, WebViewFileType, "") {
                    override fun getPath(): String = title
                }
                REQUEST_KEY.set(file, request)
                val editors = FileEditorManager.getInstance(project).openFile(file, true)
                return editors.find { it is WebView } != null
            }

            return false
        }
    }

    data class Request(
        val url: String,
        val loadingPageStrategy: LoadingPageStrategy = LoadingPageStrategy.AUTO,
        val resourceRequestHandler: ((
            browser: CefBrowser?,
            frame: CefFrame?,
            request: CefRequest,
            parent: Disposable
        ) -> CefResourceRequestHandler?)? = null,
        val queryHandler: JsQueryDispatcher.JsQueryHandler? = null
    )

    enum class LoadingPageStrategy {
        AUTO, ALWAYS, SKIP,
    }
}

