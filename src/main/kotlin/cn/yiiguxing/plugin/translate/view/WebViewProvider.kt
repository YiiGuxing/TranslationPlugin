package cn.yiiguxing.plugin.translate.view

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.view.WebPages.PageFragment
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
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
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import icons.TranslationIcons
import org.jetbrains.annotations.NonNls
import org.jetbrains.concurrency.runAsync
import javax.swing.Icon

class WebViewProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean =
        JBCefApp.isSupported() && file.fileType === WebViewFileType

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        file.getUserData(WEBVIEW_KEY) ?: HTML_KEY.get(file).let { (html, errorHtml) ->
            WebView(project, file as LightVirtualFile, html, errorHtml)
        }.also {
            file.putUserData(WEBVIEW_KEY, it)
        }


    override fun getEditorTypeId(): @NonNls String = "translation.webview"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    companion object {
        private val HTML_KEY: Key<Pair<String, String>> = Key.create("translation.webview.html.key")
        private val WEBVIEW_KEY: Key<FileEditor> = Key.create("translation.webview.component.key")

        private val LOG = logger<WebViewProvider>()

        fun open(
            project: Project,
            pageFragment: PageFragment,
            @DialogTitle title: String = TranslationPlugin.name,
            callback: ((pageFragment: PageFragment, editor: FileEditor?) -> Unit)? = null
        ) {
            val modalityState = ModalityState.defaultModalityState()
            runAsync {
                val html = getHtml(pageFragment)
                if (html != null) {
                    invokeLater(modalityState, expired = project.disposed) {
                        try {
                            val editor = openEditor(project, title, html)
                            callback?.invoke(pageFragment, editor)
                        } catch (e: Throwable) {
                            LOG.debug("Failed to open website", e)
                            callback?.invoke(pageFragment, null)
                        }
                    }
                } else {
                    callback?.invoke(pageFragment, null)
                }
            }
        }

        private fun openEditor(project: Project, @DialogTitle title: String, html: Pair<String, String>): FileEditor? {
            val file = LightVirtualFile(title, WebViewFileType, "")
            HTML_KEY.set(file, html)
            val editors = FileEditorManager.getInstance(project).openFile(file, true)
            return editors.find { it is WebView }
        }

        private fun getHtml(pageFragment: PageFragment): Pair<String, String>? = try {
            val html = WebViewProvider::class.java.classLoader
                .getResourceAsStream("website.html")
                ?.use { it.reader().readText() }
                ?.replace(
                    regex = "// Config Start(.*)// Config End".toRegex(RegexOption.DOT_MATCHES_ALL),
                    replacement = """
                        const config = {
                          fragment: "$pageFragment",
                          intellijPlatform: "${IdeVersion.buildNumber.productCode}",
                          intellijPlatformVersion: "${ApplicationInfo.getInstance().shortVersion}",
                          dark: ${!JBColor.isBright()},
                          compact: ${pageFragment.compact},
                        };
                    """.trimIndent()
                )

            html?.let { it to html.replace("// ERROR", "") }
        } catch (e: Exception) {
            LOG.debug("Failed to load website.html", e)
            null
        }
    }

    private object WebViewFileType : FakeFileType() {
        override fun isMyFileType(file: VirtualFile): Boolean = file.fileType === this
        override fun getName(): @NonNls String = "TranslationWebView"
        override fun getDescription(): @NlsContexts.Label String = "Translation webview"
        override fun getIcon(): Icon = TranslationIcons.Logo
    }
}

