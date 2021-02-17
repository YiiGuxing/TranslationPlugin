package cn.yiiguxing.plugin.translate.update

import cn.yiiguxing.plugin.compat.HTMLEditorProviderCompat
import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SUPPORT
import cn.yiiguxing.plugin.translate.activity.BaseStartupActivity
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.Icons
import org.jetbrains.concurrency.runAsync
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
        val displayId = "${plugin.name} Plugin Update"
        val title = "${plugin.name} plugin updated to v${version.version}"
        val color = getBorderColor()
        val partStyle = "margin: ${JBUI.scale(8)}px 0;"
        val refStyle = "padding: ${JBUI.scale(3)}px ${JBUI.scale(6)}px; border-left: ${JBUI.scale(3)}px solid #$color;"
        val content = """
            If you find my plugin helpful, please
            <b><a href="$HTML_DESCRIPTION_SUPPORT">support me</a>.</b>
            If you love this plugin, please consider
            <b><a href="$HTML_DESCRIPTION_SUPPORT">donating</a></b> to sustain the plugin related activities.<br/>
            Thank you for your support!
            <div style="$partStyle $refStyle">
                This update addresses these <a href="${MILESTONE_URL.format(version.version)}">issues</a>.
            </div>
            Change notes:<br/>
            ${plugin.changeNotes}
        """.trimIndent()

        val canBrowseWhatsNewHTMLEditor = canBrowseWhatsNewHTMLEditor()
        NotificationGroup(displayId, NotificationDisplayType.STICKY_BALLOON, false)
            .createNotification(title, content, NotificationType.INFORMATION, Notifications.UrlOpeningListener(false))
            .setImportant(true)
            .addAction(SupportAction())
            .apply {
                if (isFeatureVersion && !canBrowseWhatsNewHTMLEditor) {
                    addAction(WhatsNewAction(version))
                }
            }
            .whenExpired {
                if (isFeatureVersion && canBrowseWhatsNewHTMLEditor) {
                    browseWhatsNew(project)
                }
            }
            .show(project)
    }

    private class SupportAction : DumbAwareAction(message("support.notification"), null, Icons.Support) {
        override fun actionPerformed(e: AnActionEvent) = SupportDialog.show()
    }

    private class WhatsNewAction(version: Version) :
        DumbAwareAction("What's New in ${version.versionString}", null, null) {
        override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(getWhatsNewUrl())
    }


    companion object {
        private const val VERSION_PROPERTY = "${Plugin.PLUGIN_ID}.version"

        private val DEFAULT_BORDER_COLOR: Color = JBColor(0xD0D0D0, 0x555555)

        private const val GITEE_UPDATES_BASE_URL = "https://yiiguxing.gitee.io/translation-plugin/updates"
        private const val GITHUB_UPDATES_BASE_URL = "https://yiiguxing.github.io/TranslationPlugin/updates"

        private const val MILESTONE_URL =
            "https://github.com/YiiGuxing/TranslationPlugin/issues?q=milestone%%3Av%s+is%%3Aclosed"


        private fun getBorderColor(): String {
            val color = UIManager.getColor("DialogWrapper.southPanelDivider") ?: DEFAULT_BORDER_COLOR
            return (color.rgb and 0xffffff).toString(16)
        }

        private fun getWhatsNewHtml(locale: Locale = Locale.getDefault()): String {
            val name = StringBuilder(locale.language)
            if (locale.country.isNotEmpty()) {
                name.append("_", locale.country)
            }

            val loader = UpdateManager::class.java
            var htmlContent = with(loader) {
                getResource("whats_new/${name}.html")
                    ?: getResource("whats_new/default.html")
            }.readText()
            val stylesheet = loader.getResource("whats_new/stylesheet.css")!!.readText()
            htmlContent = htmlContent.replace("<style></style>", "<style>${stylesheet}</style>")

            if (UIUtil.isUnderDarcula()) {
                htmlContent = htmlContent.replace("<body>", "<body class='dark'>")
            }

            // 删除编码设定，否则显示会乱码，保留编码设定是为了方便编辑预览（去除在浏览器预览时会乱码）
            return htmlContent.replace("<meta charset=\"UTF-8\">", "")
        }

        fun getWhatsNewUrl(locale: Locale = Locale.getDefault()): String {
            val version = Version(Plugin.descriptor.version)
            val baseUrl = when (locale) {
                Locale.CHINESE,
                Locale.SIMPLIFIED_CHINESE -> GITEE_UPDATES_BASE_URL
                else -> GITHUB_UPDATES_BASE_URL
            }

            return "$baseUrl.html?v=${version.versionString}"
        }

        fun canBrowseWhatsNewHTMLEditor(): Boolean {
            return HTMLEditorProviderCompat.isSupported
        }

        fun browseWhatsNew(project: Project?) {
            println(getWhatsNewHtml())
            if (project != null && canBrowseWhatsNewHTMLEditor()) {
                fun browse(html: String) {
                    HTMLEditorProviderCompat.openEditor(project, "What's New in Translation", html)
                }

                runAsync { getWhatsNewHtml() }
                    .onSuccess { html ->
                        if (IdeVersion.isIde2020_3OrNewer) {
                            invokeLater { browse(html) }
                        } else {
                            DumbService.getInstance(project).smartInvokeLater { browse(html) }
                        }
                    }
            } else {
                val whatsNewUrl = getWhatsNewUrl()
                BrowserUtil.browse(whatsNewUrl)
            }
        }
    }

}