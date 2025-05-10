package cn.yiiguxing.plugin.translate.view

import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.util.invokeLaterIfNeeded
import cn.yiiguxing.plugin.translate.view.utils.CefLocalRequestHandler
import cn.yiiguxing.plugin.translate.view.utils.CefStreamResourceHandler
import cn.yiiguxing.plugin.translate.view.utils.CefStylesheetHelper
import cn.yiiguxing.plugin.translate.view.utils.JBCefScrollbarsHelper
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import java.io.ByteArrayInputStream

internal object ITP {

    private const val SCHEME = "itp"
    private const val SCHEME_SEPARATOR = "://"
    private const val DIALOG_URI_PREFIX = "$SCHEME$SCHEME_SEPARATOR/dialog/"

    const val SUPPORT_DIALOG_URI = DIALOG_URI_PREFIX + "support"

    private const val LOCAL_REQUEST_PROTOCOL = "http"
    private const val LOCAL_REQUEST_HOST_NAME = "itp"

    private const val LOADING_PATH = "/web/loading.html"
    private const val RELEASE_NOTES_PAGE_PATH = "/web/release-notes.html"
    private const val ERROR_PAGE_PATH = "/web/error.html"
    private const val BASE_CSS_PATH = "/base.css"
    private const val SCROLLBARS_CSS_PATH = "/scrollbars.css"

    const val RELEASE_NOTES_PAGE_URL = "$LOCAL_REQUEST_PROTOCOL://$LOCAL_REQUEST_HOST_NAME/$RELEASE_NOTES_PAGE_PATH"

    fun createCefHyperlinkHandler(project: Project, handler: CefHyperlinkHandler? = null): CefHyperlinkHandler {
        return object : DefaultCefHyperlinkHandler(project) {
            override fun onHyperlinkActivated(url: String): Boolean {
                if (super.onHyperlinkActivated(url)) {
                    return true
                }

                return handler?.onHyperlinkActivated(url) ?: false
            }
        }
    }

    fun createCefResources(parent: Disposable): CefResources = CefResources(parent)

    @Suppress("unused")
    class CefResources(parent: Disposable) : CefLocalRequestHandler(
        LOCAL_REQUEST_PROTOCOL,
        LOCAL_REQUEST_HOST_NAME
    ) {
        private val errorPage: String = addResource(ERROR_PAGE_PATH, "text/html", parent)
        val loadingPage: String = addResource(LOADING_PATH, "text/html", parent)
        val releaseNotesPage: String = addResource(RELEASE_NOTES_PAGE_PATH, "text/html", parent)
        val baseCss: String = addResource(BASE_CSS_PATH) {
            CefStreamResourceHandler(
                ByteArrayInputStream(
                    CefStylesheetHelper.buildBaseStyle().toByteArray(Charsets.UTF_8)
                ), "text/css", parent
            )
        }
        val scrollbarsCss: String = addResource(SCROLLBARS_CSS_PATH) {
            CefStreamResourceHandler(
                ByteArrayInputStream(
                    JBCefScrollbarsHelper.buildScrollbarsStyle().toByteArray(Charsets.UTF_8)
                ), "text/css", parent
            )
        }

        fun isMyResource(url: String): Boolean {
            return url.startsWith("$LOCAL_REQUEST_PROTOCOL://$LOCAL_REQUEST_HOST_NAME/")
        }

        fun getErrorPage(failedUrl: String): String {
            return "$errorPage#url=$failedUrl"
        }

        fun isErrorPage(url: String): Boolean {
            return url.startsWith(errorPage)
        }

        fun getFailedUrl(url: String): String? {
            return if (isErrorPage(url)) url.substringAfter("#url=", "").takeIf { it.isNotEmpty() } else null
        }
    }

    fun interface CefHyperlinkHandler {
        fun onHyperlinkActivated(url: String): Boolean
    }

    open class DefaultCefHyperlinkHandler(private val project: Project) : CefHyperlinkHandler {
        override fun onHyperlinkActivated(url: String): Boolean {
            when (url) {
                SUPPORT_DIALOG_URI -> invokeLater { SupportDialog.show() }

                else -> return false
            }

            return true
        }

        protected inline fun invokeLater(crossinline action: (Project) -> Unit) {
            invokeLaterIfNeeded(expired = project.disposed) { action(project) }
        }
    }
}