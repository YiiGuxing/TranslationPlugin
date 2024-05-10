package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.util.invokeLater
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.UIUtil
import org.jetbrains.concurrency.runAsync
import java.util.*

/**
 * Primary entry point for accessing web pages.
 */
object WebPages {

    private const val BASE_URL = "https://yiiguxing.github.io/TranslationPlugin"

    private val LOG = logger<WebPages>()

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
    fun releaseNote(version: String, dark: Boolean = UIUtil.isUnderDarcula()): PageFragment {
        return updates(version).query("compact=true&dark=$dark")
    }

    /**
     * Get the support page.
     */
    fun support(): PageFragment = get("support")

    /**
     * Get the donor page.
     */
    fun donors(): PageFragment = get("support").query("id=translation-plugin-sponsors")

    /**
     * Get the full URL for the specified [PageFragment].
     */
    fun getUrl(pageFragment: PageFragment): String {
        val fixedFragment = if (pageFragment.query.isNullOrEmpty()) pageFragment else {
            pageFragment.query(pageFragment.query.replace("compact=true", "compact"))
        }
        return "${BASE_URL}/${fixedFragment}"
    }

    /**
     * Whether the IDE can browse in the HTML editor.
     */
    fun canBrowseInHTMLEditor(): Boolean = JBCefApp.isSupported()

    /**
     * Browse the specified [PageFragment].
     */
    fun browse(project: Project?, pageFragment: PageFragment, title: String = TranslationPlugin.descriptor.name) {
        if (project != null && !project.isDefault && !project.isDisposed && canBrowseInHTMLEditor()) {
            val modalityState = ModalityState.defaultModalityState()
            runAsync {
                val html = try {
                    @Suppress("DialogTitleCapitalization")
                    WebPages::class.java.classLoader
                        .getResourceAsStream("/website.html")
                        ?.use { it.reader().readText() }
                        ?.replace(
                            """const TARGET_PATH = "/";""",
                            """const TARGET_PATH = "/$pageFragment";"""
                        )
                } catch (e: Exception) {
                    LOG.warn("Failed to load website.html", e)
                    null
                }
                if (html != null) {
                    invokeLater(modalityState, expired = project.disposed) {
                        try {
                            HTMLEditorProvider.openEditor(project, title, html)
                        } catch (e: Exception) {
                            LOG.warn("Failed to open website", e)
                            BrowserUtil.browse(pageFragment.getUrl())
                        }
                    }
                } else {
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
        val query: String? = null,
        /** The language of the page. */
        val language: Language = Language.CHINESE
    ) {
        /** The path of the fragment. */
        val path: String
            get() = paths.joinToString("/")

        override fun toString(): String {
            return "#${language.path}/${path.trimStart('/')}${if (query.isNullOrEmpty()) "" else "?$query"}"
        }

        /**
         * Create a new [PageFragment] with the specified path.
         */
        fun path(vararg path: String): PageFragment {
            return PageFragment(*path, query = query, language = language)
        }

        /**
         * Resolve the path with the specified path.
         */
        fun resolvePath(vararg path: String): PageFragment {
            return PageFragment(*this.paths, *path, query = query, language = language)
        }

        /**
         * Create a new [PageFragment] with the specified query.
         */
        fun query(query: String?): PageFragment {
            return PageFragment(*paths, query = query, language = language)
        }

        /**
         * Create a new [PageFragment] with the specified language.
         */
        fun language(language: Language): PageFragment {
            return PageFragment(*paths, query = query, language = language)
        }

        /**
         * Get the full URL of the page fragment.
         */
        fun getUrl(): String {
            return getUrl(this)
        }
    }
}