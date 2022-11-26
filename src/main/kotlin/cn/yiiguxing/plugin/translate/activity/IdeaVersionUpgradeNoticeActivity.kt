package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.update.UpdateManager.Companion.UPDATE_NOTIFICATION_GROUP_ID
import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.show
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

@Suppress("unused")
class IdeaVersionUpgradeNoticeActivity : BaseStartupActivity(true), DumbAware {

    override fun onBeforeRunActivity(project: Project): Boolean {
        return IdeVersion < IdeVersion.IDE2020_3 && !Notifications.isDoNotShowAgain(DO_NOT_NOTIFY_AGAIN_KEY)
    }

    override fun onRunActivity(project: Project) = showNotification(project)


    companion object {

        private val DO_NOT_NOTIFY_AGAIN_KEY = "IdeaVersionUpgradeNotice.${IdeVersion.buildNumber}"

        private fun showNotification(project: Project) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(UPDATE_NOTIFICATION_GROUP_ID)
                .createNotification(
                    message("notification.idea.version"), NotificationType.WARNING
                )
                .setTitle(message("notification.idea.version.title"))
                .addAction(Notifications.DoNotShowAgainAction(DO_NOT_NOTIFY_AGAIN_KEY))
                .setImportant(true)
                .show(project)
        }
    }
}