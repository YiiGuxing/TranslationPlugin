package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.UPDATE_NOTIFICATION_GROUP_ID
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.util.show
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

@Suppress("unused")
class IdeaVersionUpgradeNoticeActivity : BaseStartupActivity(true), DumbAware {

    override fun onBeforeRunActivity(project: Project): Boolean {
        return !(IdeVersion.isIde2019_3OrNewer || PropertiesComponent.getInstance()
            .getBoolean(DO_NOT_NOTIFY_AGAIN_PROPERTY, false))
    }

    override fun onRunActivity(project: Project) = showNotification(project)

    private class DoNotShowAgainAction : NotificationAction(message("notification.idea.version.do.not.show.again")) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            notification.expire()
            PropertiesComponent.getInstance().setValue(DO_NOT_NOTIFY_AGAIN_PROPERTY, true)
        }
    }

    companion object {
        private const val DISPLAY_ID = "Outdated IDE Version"

        private val DO_NOT_NOTIFY_AGAIN_PROPERTY =
            "yii.guxing.translate.IdeaVersionUpgradeNotice.${IdeVersion.buildNumber}.disable"

        private fun showNotification(project: Project) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(UPDATE_NOTIFICATION_GROUP_ID)
                .createNotification(
                    message("notification.idea.version.title"),
                    message("notification.idea.version"),
                    NotificationType.WARNING,
                    null
                )
                .addAction(DoNotShowAgainAction())
                .setImportant(true)
                .show(project)
        }
    }
}