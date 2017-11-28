package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.TKK
import cn.yiiguxing.plugin.translate.trans.YoudaoTranslator
import com.intellij.notification.*
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project

import javax.swing.event.HyperlinkEvent
import kotlin.properties.Delegates

class TranslationComponent(project: Project) : AbstractProjectComponent(project) {

    private var settings: Settings by Delegates.notNull()

    override fun initComponent() {
        super.initComponent()
        TKK.update()
        settings = Settings.instance
    }

    override fun projectOpened() {
        if (settings.isDisableAppKeyNotification
                || (settings.translator == YoudaoTranslator.TRANSLATOR_ID
                && settings.youdaoTranslateSettings.appId.isNotBlank()
                && settings.youdaoTranslateSettings.isAppKeyConfigured)) {
            return
        }

        val group = NotificationGroup(DISPLAY_ID_APP_KEY, NotificationDisplayType.STICKY_BALLOON, true)
        val title = "设置有道App Key"
        val content = "当前有道App Key为空或者无效，请设置有道App Key.<br/><br/>" +
                "<a href=\"$HTML_DESCRIPTION_SETTINGS\">设置</a> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                "<a href=\"$HTML_DESCRIPTION_DISABLE\">不再提示</a>"
        val notification = group.createNotification(
                title,
                content,
                NotificationType.WARNING,
                object : NotificationListener.Adapter() {
                    override fun hyperlinkActivated(notification: Notification, hyperlinkEvent: HyperlinkEvent) {
                        notification.expire()
                        when (hyperlinkEvent.description) {
                            HTML_DESCRIPTION_SETTINGS -> OptionsConfigurable.showSettingsDialog(myProject)
                            HTML_DESCRIPTION_DISABLE -> settings.isDisableAppKeyNotification = true
                        }
                    }
                }
        )
        Notifications.Bus.notify(notification, myProject)
    }

    private companion object {
        private const val DISPLAY_ID_APP_KEY = "NOTIFICATION_APP_KEY"
    }
}
