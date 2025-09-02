package cn.yiiguxing.plugin.translate.view

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.util.UrlTrackingParametersProvider
import cn.yiiguxing.plugin.translate.util.urlEncode
import cn.yiiguxing.plugin.translate.view.WebPages.updates
import cn.yiiguxing.plugin.translate.view.utils.CefStreamResourceHandler
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefResourceHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.network.CefRequest
import java.util.*

/**
 * Primary entry point for accessing web pages.
 */
object WebPages {

    const val BASE_URL = "https://intellij-translation.yiiguxing.top"

    private const val RELEASE_NOTES_PAGE_URL = "$BASE_URL/release-notes"
    private const val SPONSORS_PAGE_URL = "$BASE_URL/sponsor"
    private const val RELEASE_NOTES_PAGE_PATH = "/web/release-notes.html"

    /**
     * Get a [PageFragment] for the specified path.
     */
    fun get(vararg path: String, locale: Locale = Locale.getDefault()): PageFragment {
        return PageFragment(*path, language = getSupportedLanguage(locale))
    }

    fun getSupportedLanguage(locale: Locale = Locale.getDefault()): Language {
        return when (locale.language) {
            Locale.CHINESE.language -> if (locale.country == "CN") Language.CHINESE else Language.ENGLISH
            Locale.JAPANESE.language -> Language.JAPANESE
            Locale.KOREAN.language -> Language.KOREAN
            else -> Language.ENGLISH
        }
    }

    /**
     * Get the home page.
     */
    fun home(): PageFragment = get()

    /**
     * Get the documentation page.
     */
    fun docs(): PageFragment = get("docs")

    /**
     * Get the update page for the specified version.
     */
    fun updates(version: String = ""): PageFragment {
        return get("updates").let { if (version.isEmpty()) it else it.resolvePath("v$version") }
    }

    /**
     * Get the release note page for the specified version.
     * Unlike the [update page][updates], the release notes page does not include the website framework.
     */
    fun releaseNote(version: String): PageFragment {
        return updates(version).copy(compact = true)
    }

    /**
     * Get the support page.
     */
    fun support(): PageFragment = get("support")

    /**
     * Get the donor page.
     */
    fun donors(): PageFragment = support().copy(id = "translation-plugin-sponsors")

    /**
     * Get the full URL for the specified [PageFragment].
     */
    fun getUrl(pageFragment: PageFragment): String {
        return UrlTrackingParametersProvider.augmentIdeUrl(
            "${BASE_URL}/${pageFragment}",
            "compact" to pageFragment.compact.toString()
        )
    }

    fun getSponsorsPageUrl(locale: Locale = Locale.getDefault()): String {
        return UrlTrackingParametersProvider.augmentIdeUrl(
            SPONSORS_PAGE_URL,
            "language" to getSupportedLanguage(locale).code,
            "dark" to JBColor.isBright().not().toString()
        )
    }

    /**
     * Whether the IDE can browse in the webview.
     */
    fun canBrowseInWebView(): Boolean = JBCefApp.isSupported()

    fun browseReleaseNotesPage(project: Project?, title: String = TranslationPlugin.name): Boolean {
        if (project == null || !canBrowseInWebView()) {
            return false
        }

        val request = WebViewProvider.Request(
            url = UrlTrackingParametersProvider.augmentIdeUrl(RELEASE_NOTES_PAGE_URL),
            loadingPageStrategy = WebViewProvider.LoadingPageStrategy.SKIP,
            resourceRequestHandler = { _, _, request, parent ->
                if (request.url?.startsWith(RELEASE_NOTES_PAGE_URL) == true) {
                    object : CefResourceRequestHandlerAdapter() {
                        override fun getResourceHandler(
                            browser: CefBrowser?,
                            frame: CefFrame?,
                            request: CefRequest
                        ): CefResourceHandler? {
                            return WebPages::class.java.getResourceAsStream(RELEASE_NOTES_PAGE_PATH)?.let {
                                CefStreamResourceHandler(it, "text/html", parent)
                            }
                        }
                    }
                } else null
            },
        )

        return WebViewProvider.open(project, request, title)
    }

    /**
     * Browse the specified [PageFragment].
     */
    fun browse(
        project: Project?,
        pageFragment: PageFragment = home(),
        title: String = TranslationPlugin.name,
        browseInWebView: Boolean = true,
        fallbackToBrowser: Boolean = true
    ): Boolean {
        if (browseInWebView) {
            val url = pageFragment.getUrl()
            val success = project != null && canBrowseInWebView() && WebViewProvider.open(project, url, title)
            if (success || !fallbackToBrowser) {
                return success
            }
        }

        val target = if (pageFragment.compact) pageFragment.copy(compact = false) else pageFragment
        BrowserUtil.browse(target.getUrl())
        return true
    }

    /**
     * Browse the specified [url].
     */
    fun browse(
        project: Project?,
        url: String,
        title: String = TranslationPlugin.name,
        browseInWebView: Boolean = true,
        fallbackToBrowser: Boolean = true
    ): Boolean {
        if (browseInWebView) {
            val success = project != null && canBrowseInWebView() && WebViewProvider.open(project, url, title)
            if (success || !fallbackToBrowser) {
                return success
            }
        }

        BrowserUtil.browse(url)
        return true
    }


    /**
     * Supported languages.
     */
    enum class Language(val code: String) {
        CHINESE("zh-CN"), ENGLISH("en"), JAPANESE("ja"), KOREAN("ko");
    }

    private val Language.path: String
        get() = when (this) {
            Language.CHINESE -> ""
            else -> "/$code"
        }

    /**
     * A fragment of a web page.
     */
    class PageFragment(
        private vararg val paths: String,
        /** The query string. */
        val id: String? = null,
        /** The language of the page. */
        val language: Language = Language.CHINESE,
        /** Whether to use compact mode. */
        val compact: Boolean = false,
    ) {
        /** The path of the fragment. */
        val path: String
            get() = paths.joinToString("/") { it.urlEncode() }

        override fun toString(): String {
            val idPart = if (id.isNullOrBlank()) "" else "?id=${id.urlEncode()}"
            return "#${language.path}/${path.trimStart('/')}$idPart"
        }

        /**
         * Resolve the path with the specified [path].
         */
        fun resolvePath(vararg path: String): PageFragment {
            return PageFragment(*this.paths, *path, id = id, language = language)
        }

        /**
         * Copy this [PageFragment] with the specified parameters.
         */
        fun copy(
            vararg path: String = this.paths,
            id: String? = this.id,
            language: Language = this.language,
            compact: Boolean = this.compact
        ): PageFragment {
            return PageFragment(*path, id = id, language = language, compact = compact)
        }

        /**
         * Get the full URL of the page fragment.
         */
        fun getUrl(): String {
            return getUrl(this)
        }
    }
}