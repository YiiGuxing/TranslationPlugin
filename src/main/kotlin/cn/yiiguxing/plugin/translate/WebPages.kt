@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.util.IdeVersion
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

    private const val BASE_URL = "https://intellij-translation.yiiguxing.top"

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
        return "${BASE_URL}/${pageFragment.copy(compact = false)}"
    }

    /**
     * Whether the IDE can browse in the HTML editor.
     */
    fun canBrowseInHTMLEditor(): Boolean = JBCefApp.isSupported()

    /**
     * Browse the specified [PageFragment].
     */
    fun browse(project: Project?, pageFragment: PageFragment, title: String = TranslationPlugin.name) {
        if (project != null && !project.isDefault && !project.isDisposed && canBrowseInHTMLEditor()) {
            val modalityState = ModalityState.defaultModalityState()
            runAsync {
                val html = try {
                    WebPages::class.java.classLoader
                        .getResourceAsStream("website.html")
                        ?.use { it.reader().readText() }
                        ?.replace(
                            "// Config Start(.*)// Config End".toRegex(RegexOption.DOT_MATCHES_ALL),
                            """
                            const config = {
                              fragment: "$pageFragment",
                              intellijPlatform: "${IdeVersion.buildNumber.productCode}",
                              dark: ${UIUtil.isUnderDarcula()},
                            };
                            """.trimIndent()
                        )
                } catch (e: Exception) {
                    LOG.warn("Failed to load website.html", e)
                    null
                }
                if (html != null) {
                    invokeLater(modalityState, expired = project.disposed) {
                        try {
                            HTMLEditorProvider.openEditor(project, title, html)
                        } catch (e: Throwable) {
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
        val id: String? = null,
        /** The language of the page. */
        val language: Language = Language.CHINESE,
        /** Whether to use compact mode. */
        val compact: Boolean = false,
    ) {
        /** The path of the fragment. */
        val path: String
            get() = paths.joinToString("/")

        override fun toString(): String {
            val queryPart = StringBuilder("utm_source=intellij")
            if (compact) {
                queryPart.append("&compact=true")
            }
            id?.let { queryPart.append("&id=$it") }

            return "#${language.path}/${path.trimStart('/')}?$queryPart"
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