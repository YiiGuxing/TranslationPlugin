package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.BaiduTranslator
import cn.yiiguxing.plugin.translate.trans.YoudaoTranslator
import cn.yiiguxing.plugin.translate.util.Plugin
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.isNullOrBlank
import cn.yiiguxing.plugin.translate.util.show
import com.intellij.notification.*
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import javax.swing.event.HyperlinkEvent

class TranslationProjectComponent(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
        checkConfig()
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

    override fun disposeComponent() {
        TranslationUIManager.disposeUI(myProject)
    }

}
