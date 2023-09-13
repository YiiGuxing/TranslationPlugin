package cn.yiiguxing.plugin.translate.update

import cn.yiiguxing.plugin.compat.HTMLEditorProviderCompat
import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SUPPORT
import cn.yiiguxing.plugin.translate.activity.BaseStartupActivity
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.util.*
import com.google.gson.Gson
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import com.intellij.util.ui.UIUtil
import icons.Icons
import java.util.*

class UpdateManager : BaseStartupActivity(), DumbAware {

    override fun onRunActivity(project: Project) {
        checkUpdate(project)
        checkUpdateFromGithub(project)
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

    private fun checkUpdateFromGithub(project: Project) {
        val key = "${Plugin.PLUGIN_ID}.LAST_CHECKED_TIME"
        val properties: PropertiesComponent = PropertiesComponent.getInstance()
        val last = properties.getInt(key, 0)
        val days = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
        if (days <= last) {
            return
        }
        properties.setValue(key, days.toInt(), last)
        executeOnPooledThread {
            val newVersion = try {
                HttpRequests.request(UPDATES_API)
                    .connect { Gson().fromJson(it.readString(null), Version::class.java) }!!
            } catch (e: Throwable) {
                LOGGER.w("Cannot get release info from Github.", e)
                return@executeOnPooledThread
            }

            LOGGER.d("Latest released plugin version: $newVersion")

            val lastVersionString = properties.getValue(VERSION_IN_GITHUB_PROPERTY, "0.0")
            val lastVersion = Version(lastVersionString)

            if (newVersion > lastVersion) {
                invokeOnDispatchThread {
                    if (!project.isDisposed) {
                        showUpdateIDENotification(project, newVersion)
                    }
                }
            }

            properties.setValue(VERSION_IN_GITHUB_PROPERTY, newVersion.versionString)
        }
    }

    private fun showUpdateNotification(
        project: Project,
        plugin: IdeaPluginDescriptor,
        version: Version,
        isFeatureVersion: Boolean
    ) {
        val displayId = "${plugin.name} Plugin Update"
        val title = message("plugin.name.updated.to.version.notification.title", plugin.name, version.version)
        val content = """
            If you find my plugin helpful, please
            <b><a href="$HTML_DESCRIPTION_SUPPORT">support me</a>.</b>
            If you love this plugin, please consider
            <b><a href="$HTML_DESCRIPTION_SUPPORT">donating</a></b> to sustain the plugin related activities.<br/>
            Thank you for your support!<br/>
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

    private fun showUpdateIDENotification(project: Project, version: Version) {
        NotificationGroup("Translation Plugin Update(IDE)", NotificationDisplayType.STICKY_BALLOON, false)
            .createNotification(
                message("updater.new.version.notification.title", version.versionString),
                message("updater.new.version.notification.content", version.versionString),
                NotificationType.INFORMATION,
                null
            )
            .addAction(SupportAction())
            .addAction(UpdateDetailsAction(version))
            .setImportant(true)
            .show(project)
    }

    private class SupportAction : DumbAwareAction(message("support.notification"), null, Icons.Support) {
        override fun actionPerformed(e: AnActionEvent) = SupportDialog.show()
    }

    private class UpdateDetailsAction(private val version: Version) :
        DumbAwareAction(message("updater.new.version.notification.action.detail")) {
        override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(version.updatesUrl)
    }

    private class WhatsNewAction(version: Version) :
        DumbAwareAction(message("action.WhatsNewInTranslationAction.text", version.versionString), null, null) {
        override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(getWhatsNewUrl(true))
    }


    companion object {
        private const val VERSION_PROPERTY = "${Plugin.PLUGIN_ID}.version"

        private const val VERSION_IN_GITHUB_PROPERTY = "${Plugin.PLUGIN_ID}.version.github"

        private const val BASE_URL_GITEE = "https://yiiguxing.gitee.io/translation-plugin"
        private const val BASE_URL_GITHUB = "https://yiiguxing.github.io/TranslationPlugin"

        private const val UPDATES_BASE_URL = "http://yiiguxing.github.io/TranslationPlugin/updates"

        private const val UPDATES_API = "https://api.github.com/repos/YiiGuxing/TranslationPlugin/releases/latest"

        private val LOGGER: Logger = Logger.getInstance(UpdateManager::class.java)


        private val Version.updatesUrl: String get() = "$UPDATES_BASE_URL.html?v=$versionString"

        fun getWhatsNewUrl(frame: Boolean = false, locale: Locale = Locale.getDefault()): String {
            val version = Version(Plugin.descriptor.version)
            val baseUrl = when (locale) {
                Locale.CHINA,
                Locale.CHINESE,
                Locale.SIMPLIFIED_CHINESE -> BASE_URL_GITEE
                else -> BASE_URL_GITHUB
            }
            val langPath = when (locale) {
                Locale.CHINA,
                Locale.CHINESE,
                Locale.SIMPLIFIED_CHINESE -> ""
                Locale.JAPAN,
                Locale.JAPANESE,
                Locale.KOREA,
                Locale.KOREAN -> "/${locale.language}"
                else -> "/en"
            }

            val v = version.versionString
            return if (frame) {
                "$baseUrl$langPath/#/updates/v$v"
            } else {
                val isDark = UIUtil.isUnderDarcula()
                "$baseUrl$langPath/#/updates/v$v?compact=true&dark=$isDark"
            }
        }

        fun canBrowseWhatsNewHTMLEditor(): Boolean {
            return HTMLEditorProviderCompat.isSupported
        }

        fun browseWhatsNew(project: Project?) {
            if (project != null && canBrowseWhatsNewHTMLEditor()) {
                fun browse() {
                    val whatsNewUrl = getWhatsNewUrl()
                    HTMLEditorProviderCompat.openEditor(
                        project,
                        message("action.WhatsNewInTranslationAction.text", "Translation"),
                        whatsNewUrl,
                        """<div style="text-align: center;padding-top: 3rem">
                            |<div style="padding-top: 1rem">Failed to load!</div>
                            |<div><a href="$whatsNewUrl" target="_blank" style="font-size: 2rem">Open in browser</a></div>
                            |</div>""".trimMargin()
                    )
                }

                DumbService.getInstance(project).smartInvokeLater { browse() }
            } else {
                BrowserUtil.browse(getWhatsNewUrl(true))
            }
        }
    }

}