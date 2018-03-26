package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.TKK
import cn.yiiguxing.plugin.translate.trans.YoudaoTranslator
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.show
import com.intellij.notification.*
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import javax.swing.event.HyperlinkEvent

class TranslationProjectComponent(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
        TKK.update()
        checkYoudaoConfig()
        TranslationUIManager.installStatusWidget(myProject)
    }

    @Suppress("InvalidBundleOrProperty")
    private fun checkYoudaoConfig() {
        with(Settings) {
            if (isDisableAppKeyNotification || translator != YoudaoTranslator.TRANSLATOR_ID) {
                return
            }
            if (youdaoTranslateSettings.let { it.isAppKeyConfigured && it.appId.isNotBlank() }) {
                return
            }
        }

        val group = NotificationGroup(DISPLAY_ID_APP_KEY, NotificationDisplayType.STICKY_BALLOON, true)
        val title = message("notification.title.youdao.settings")
        val content = message("notification.content.youdao.settings", HTML_DESCRIPTION_DISABLE, HTML_DESCRIPTION_SETTINGS)
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

    override fun disposeComponent() {
        TranslationUIManager.disposeUI(myProject)
    }

    private companion object {
        private const val DISPLAY_ID_APP_KEY = "NOTIFICATION_APP_KEY"
    }
}
