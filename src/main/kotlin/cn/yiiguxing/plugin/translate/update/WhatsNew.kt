package cn.yiiguxing.plugin.translate.update

import cn.yiiguxing.plugin.translate.WebPages
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.UIUtil
import java.util.*

internal object WhatsNew {

    private val LOG = logger<WhatsNew>()

    val canBrowseInHTMLEditor: Boolean get() = JBCefApp.isSupported()

    fun browse(version: Version, project: Project?) {
        if (project != null && canBrowseInHTMLEditor) {
            invokeLater(ModalityState.NON_MODAL, expired = project.disposed) {
                try {
                    HTMLEditorProvider.openEditor(
                        project,
                        adaptedMessage("action.WhatsNewInTranslationAction.text", "Translation"),
                        version.getWhatsNewUrl(),
                        //language=HTML
                        """<div style="text-align: center;padding-top: 3rem">
                            |<div style="padding-top: 1rem; margin-bottom: 0.8rem;">Failed to load!</div>
                            |<div><a href="${version.getWhatsNewUrl(true)}" target="_blank"
                            |        style="font-size: 2rem">Open in browser</a></div>
                            |</div>""".trimMargin()
                    )
                } catch (e: Throwable) {
                    LOG.w("""Failed to load "What's New" page""", e)
                    BrowserUtil.browse(version.getWhatsNewUrl(true))
                }
            }
        } else {
            BrowserUtil.browse(version.getWhatsNewUrl(true))
        }
    }

    private fun Version.getWhatsNewUrl(frame: Boolean = false, locale: Locale = Locale.getDefault()): String {
        val v = getFeatureUpdateVersion()
        return if (frame) {
            WebPages.updates(v, locale = locale)
        } else {
            val isDark = UIUtil.isUnderDarcula()
            WebPages.releaseNote(v, isDark, locale = locale)
        }
    }

    class Action(private val version: Version) : DumbAwareAction(
        message("action.WhatsNewInTranslationAction.text", version.getFeatureUpdateVersion()),
        null,
        AllIcons.General.Web
    ) {
        override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(version.getWhatsNewUrl(true))
    }
}