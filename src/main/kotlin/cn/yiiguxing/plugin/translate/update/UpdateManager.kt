package cn.yiiguxing.plugin.translate.update

import cn.yiiguxing.plugin.translate.WebPages
import cn.yiiguxing.plugin.translate.activity.BaseStartupActivity
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.TranslationIcons
import java.awt.Color
import java.util.*
import javax.swing.UIManager

class UpdateManager : BaseStartupActivity(), DumbAware {

    override fun onRunActivity(project: Project) {
        checkUpdate(project)
    }

    private fun checkUpdate(project: Project) {
        val plugin = Plugin.descriptor
        val versionString = plugin.version
        val properties: PropertiesComponent = PropertiesComponent.getInstance()
        val lastVersionString = properties.getValue(VERSION_PROPERTY, "0.0")
        if (versionString == lastVersionString) {
            return
        }

        val version = Version(versionString)
        val lastVersion = Version(lastVersionString)
        val isFeatureVersion = version > lastVersion
        showUpdateNotification(project, plugin, version, isFeatureVersion)
        properties.setValue(VERSION_PROPERTY, versionString)
    }

    private fun showUpdateNotification(
        project: Project,
        plugin: IdeaPluginDescriptor,
        version: Version,
        isFeatureVersion: Boolean
    ) {
        val title = message("plugin.name.updated.to.version.notification.title", plugin.name, version.version)
        val color = getBorderColor()
        val partStyle = "margin: ${JBUI.scale(8)}px 0;"
        val refStyle = "padding: ${JBUI.scale(3)}px ${JBUI.scale(6)}px; border-left: ${JBUI.scale(3)}px solid #$color;"
        val content = """
            If you find my plugin helpful, please
            <b><a href="${Hyperlinks.SUPPORT_DESCRIPTION}">support me</a>.</b>
            If you love this plugin, please consider
            <b><a href="${Hyperlinks.SUPPORT_DESCRIPTION}">donating</a></b> to sustain the plugin related activities.<br/>
            Thank you for your support!
            <div style="$partStyle $refStyle">
                This update addresses these <a href="${MILESTONE_URL.format(version.version)}">issues</a>.
            </div>
            Change notes:<br/>
            ${plugin.changeNotes}
        """.trimIndent()

        val canBrowseWhatsNewHTMLEditor = canBrowseWhatsNewHTMLEditor()
        NotificationGroupManager.getInstance()
            .getNotificationGroup(UPDATE_NOTIFICATION_GROUP_ID)
            .createNotification(content, NotificationType.INFORMATION)
            .setTitle(title)
            .setImportant(true)
            .setListener(Notifications.UrlOpeningListener(false))
            .apply {
                if (isFeatureVersion && !canBrowseWhatsNewHTMLEditor) {
                    addAction(WhatsNewAction(version))
                }
            }
            .addAction(GetStartedAction())
            .addAction(SupportAction())
            .whenExpired {
                if (isFeatureVersion && canBrowseWhatsNewHTMLEditor) {
                    browseWhatsNew(project)
                }
            }
            .show(project)
    }

    private class SupportAction : DumbAwareAction(message("support.notification"), null, TranslationIcons.Support) {
        override fun actionPerformed(e: AnActionEvent) = SupportDialog.show()
    }

    private class WhatsNewAction(version: Version) :
        DumbAwareAction(
            message("action.WhatsNewInTranslationAction.text", version.versionString),
            null,
            AllIcons.General.Web
        ) {
        override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(getWhatsNewUrl(true))
    }

    private class GetStartedAction :
        DumbAwareAction(message("action.GetStartedAction.text"), null, AllIcons.General.Web) {
        override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(WebPages.getStarted())
    }


    companion object {
        const val UPDATE_NOTIFICATION_GROUP_ID = "Translation Plugin Updates"

        private const val VERSION_PROPERTY = "${Plugin.PLUGIN_ID}.version"

        private val DEFAULT_BORDER_COLOR: Color = JBColor(0xD0D0D0, 0x555555)

        private const val MILESTONE_URL =
            "https://github.com/YiiGuxing/TranslationPlugin/issues?q=milestone%%3Av%s+is%%3Aclosed"


        private fun getBorderColor(): String {
            val color = UIManager.getColor("DialogWrapper.southPanelDivider") ?: DEFAULT_BORDER_COLOR
            return (color.rgb and 0xffffff).toString(16)
        }

        fun getWhatsNewUrl(frame: Boolean = false, locale: Locale = Locale.getDefault()): String {
            val version = Version(Plugin.descriptor.version)
            val v = version.versionString
            return if (frame) {
                WebPages.updates(v, locale = locale)
            } else {
                val isDark = UIUtil.isUnderDarcula()
                WebPages.releaseNote(v, isDark, locale = locale)
            }
        }

        fun canBrowseWhatsNewHTMLEditor(): Boolean {
            return JBCefApp.isSupported()
        }

        fun browseWhatsNew(project: Project?) {
            if (project != null && canBrowseWhatsNewHTMLEditor()) {
                invokeLater {
                    if (project.isDisposed) {
                        return@invokeLater
                    }
                    HTMLEditorProvider.openEditor(
                        project,
                        adaptedMessage("action.WhatsNewInTranslationAction.text", "Translation"),
                        getWhatsNewUrl(),
                        """<div style="text-align: center;padding-top: 3rem">
                            |<div style="padding-top: 1rem; margin-bottom: 0.8rem;">Failed to load!</div>
                            |<div><a href="${getWhatsNewUrl(true)}" target="_blank" style="font-size: 2rem">Open in browser</a></div>
                            |</div>""".trimMargin()
                    )
                }
            } else {
                BrowserUtil.browse(getWhatsNewUrl(true))
            }
        }
    }

}