package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.BaiduTranslator
import cn.yiiguxing.plugin.translate.trans.YoudaoTranslator
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.util.Plugin
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.isNullOrBlank
import cn.yiiguxing.plugin.translate.util.show
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import icons.Icons
import javax.swing.event.HyperlinkEvent

class TranslationProjectComponent(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
        checkConfig()
        checkUpdate()
        TranslationUIManager.installStatusWidget(myProject)
    }

    private fun checkConfig() {
        if (!needShowNotification(Settings.translator)) {
            return
        }

        val displayId = "${Plugin.descriptor.name} App Key"
        val group = NotificationGroup(displayId, NotificationDisplayType.STICKY_BALLOON, true)
        val title = message("notification.title.settings.appKey")
        val content = message(
            "notification.content.settings.appKey", HTML_DESCRIPTION_DISABLE,
            HTML_DESCRIPTION_SETTINGS
        )
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
        val plugin = Plugin.descriptor
        val version = plugin.version
        val properties: PropertiesComponent = PropertiesComponent.getInstance()
        if (version == properties.getValue(VERSION_PROPERTY)) {
            return
        }

        val displayId = "${plugin.name} Plugin Update"
        val title = "${plugin.name} plugin updated to v$version"
        val content = """
            If you find my plugin helpful, please
            <b><a href="$HTML_DESCRIPTION_SUPPORT">support me</a>:</b>
            <b><a href="$HTML_DESCRIPTION_SUPPORT">Donate</a></b> with
            <b><a href="$HTML_DESCRIPTION_SUPPORT">AliPay/WeChatPay</a>.</b>
            <br/><br/>Change notes:<br/>${plugin.changeNotes}
        """.trimIndent()
        NotificationGroup(displayId, NotificationDisplayType.STICKY_BALLOON, false)
            .createNotification(
                title, content, NotificationType.INFORMATION,
                object : NotificationListener.Adapter() {
                    override fun hyperlinkActivated(notification: Notification, hyperlinkEvent: HyperlinkEvent) {
                        if (hyperlinkEvent.description == HTML_DESCRIPTION_SUPPORT) {
                            SupportDialog.show()
                        } else {
                            URL_OPENING_LISTENER.hyperlinkUpdate(notification, hyperlinkEvent)
                        }
                    }
                }
            )
            .addAction(object : DumbAwareAction("Support!", null, Icons.Support) {
                override fun actionPerformed(e: AnActionEvent) = SupportDialog.show()
            })
            .setImportant(true)
            .show(myProject)
        properties.setValue(VERSION_PROPERTY, version)
    }

    override fun disposeComponent() {
        TranslationUIManager.disposeUI(myProject)
    }

    private companion object {
        private const val VERSION_PROPERTY = "${Plugin.PLUGIN_ID}.version"
    }
}
