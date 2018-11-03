package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.BaiduTranslator
import cn.yiiguxing.plugin.translate.trans.TKK
import cn.yiiguxing.plugin.translate.trans.YoudaoTranslator
import cn.yiiguxing.plugin.translate.util.App
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.isNullOrBlank
import cn.yiiguxing.plugin.translate.util.show
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.notification.NotificationListener.URL_OPENING_LISTENER
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import javax.swing.event.HyperlinkEvent

class TranslationProjectComponent(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
        TKK.update()
        checkConfig()
        checkUpdate()
        TranslationUIManager.installStatusWidget(myProject)
    }

    @Suppress("InvalidBundleOrProperty")
    private fun checkConfig() {
        if (!needShowNotification(Settings.translator)) {
            return
        }

        val displayId = "${App.plugin.name} App Key"
        val group = NotificationGroup(displayId, NotificationDisplayType.STICKY_BALLOON, true)
        val title = message("notification.title.settings.appKey")
        val content = message("notification.content.settings.appKey", HTML_DESCRIPTION_DISABLE,
                HTML_DESCRIPTION_SETTINGS)
        group.createNotification(title, content, NotificationType.WARNING,
                object : NotificationListener.Adapter() {
                    override fun hyperlinkActivated(notification: Notification, hyperlinkEvent: HyperlinkEvent) {
                        notification.expire()
                        when (hyperlinkEvent.description) {
                            HTML_DESCRIPTION_SETTINGS -> OptionsConfigurable.showSettingsDialog(myProject)
                            HTML_DESCRIPTION_DISABLE -> Settings.isDisableAppKeyNotification = true
                        }
                    }
                }
        ).show(myProject)
    }

    private fun needShowNotification(translatorId: String): Boolean = with(Settings) {
        when (translatorId) {
            YoudaoTranslator.TRANSLATOR_ID -> youdaoTranslateSettings
            BaiduTranslator.TRANSLATOR_ID -> baiduTranslateSettings
            else -> return@with false
        }.let { !it.isAppKeyConfigured || it.appId.isNullOrBlank() }
    }

    private fun checkUpdate() {
        val plugin = App.plugin
        val version = plugin.version
        val properties: PropertiesComponent = PropertiesComponent.getInstance()
        if (version != properties.getValue(VERSION_PROPERTY)) {
            val displayId = "${plugin.name} Plugin Update"
            val title = "${plugin.name} plugin updated to v$version"
            val content = "If you find this plugin helpful, please " +
                    "<b><a href=\"$GITHUB_URL\">give me a star on Github</a>.</b> " +
                    "If you run into any issue, feel free to <b><a href=\"$GITHUB_URL/issues\">raise a issue</a>.</b>" +
                    "<br/><br/>Change notes:<br/>${plugin.changeNotes}"
            NotificationGroup(displayId, NotificationDisplayType.STICKY_BALLOON, false)
                    .createNotification(title, content, NotificationType.INFORMATION, URL_OPENING_LISTENER)
                    .show(myProject)
            properties.setValue(VERSION_PROPERTY, version)
        }
    }

    override fun disposeComponent() {
        TranslationUIManager.disposeUI(myProject)
    }

    private companion object {
        private const val VERSION_PROPERTY = "${App.PLUGIN_ID}.version"
    }
}
