package cn.yiiguxing.plugin.translate.view

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.util.urlEncode
import cn.yiiguxing.plugin.translate.view.WebPages.updates
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import java.util.*

/**
 * Primary entry point for accessing web pages.
 */
object WebPages {

    const val BASE_URL = "https://intellij-translation.yiiguxing.top"

    /**
     * Get a [PageFragment] for the specified path.
     */
    fun get(vararg path: String, locale: Locale = Locale.getDefault()): PageFragment {
        val lang = when (locale.language) {
            Locale.CHINESE.language -> Language.CHINESE
            Locale.JAPANESE.language -> Language.JAPANESE
            Locale.KOREAN.language -> Language.KOREAN
            else -> Language.ENGLISH
        }
        return PageFragment(*path, language = lang)
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
        val queryPart = StringBuilder("utm_source=intellij")
            .append("&utm_medium=plugin")
            .append("&utm_campaign=", IdeVersion.buildNumber.productCode)
            .append("&utm_content=", ApplicationInfo.getInstance().shortVersion)
        if (pageFragment.compact) {
            queryPart.append("&compact=true")
        }

        return "${BASE_URL}/?$queryPart${pageFragment.copy(compact = false)}"
    }

    /**
     * Whether the IDE can browse in the webview.
     */
    fun canBrowseInWebView(): Boolean = JBCefApp.isSupported()

    /**
     * Browse the specified [PageFragment].
     */
    fun browse(
        project: Project?,
        pageFragment: PageFragment = home(),
        title: String = TranslationPlugin.name,
        browseInWebView: Boolean = true
    ) {
        if (browseInWebView && project != null && !project.isDefault && !project.isDisposed && canBrowseInWebView()) {
            WebViewProvider.open(project, pageFragment, title) { _, editor ->
                if (editor == null) {
                    BrowserUtil.browse(pageFragment.getUrl())
                }
            }
        } else {
            BrowserUtil.browse(pageFragment.getUrl())
        }
    }


    /**
     * Supported languages.
     */
    enum class Language {
        CHINESE, ENGLISH, JAPANESE, KOREAN;
    }

    private val Language.path: String
        get() = when (this) {
            Language.CHINESE -> ""
            Language.ENGLISH -> "/en"
            Language.JAPANESE -> "/ja"
            Language.KOREAN -> "/ko"
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