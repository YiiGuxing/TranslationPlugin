package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.update.UpdateManager.Companion.UPDATE_NOTIFICATION_GROUP_ID
import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.util.Notifications
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import icons.TranslationIcons

class IdeaVersionUpgradeNoticeActivity : BaseStartupActivity(true, false) {

    override suspend fun onBeforeRunActivity(project: Project): Boolean {
        return IdeVersion < IdeVersion.IDE2024_1 && !Notifications.isDoNotShowAgain(DO_NOT_NOTIFY_AGAIN_KEY)
    }

    override suspend fun onRunActivity(project: Project) {
        showNotification(project)
    }
}

private val DO_NOT_NOTIFY_AGAIN_KEY = "IdeaVersionUpgradeNotice.${IdeVersion.buildNumber}"

private fun showNotification(project: Project) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup(UPDATE_NOTIFICATION_GROUP_ID)
        .createNotification(message("notification.idea.version"), NotificationType.WARNING)
        .setIcon(TranslationIcons.Logo)
        .setTitle(message("notification.idea.version.title"))
        .addAction(Notifications.DoNotShowAgainAction(DO_NOT_NOTIFY_AGAIN_KEY))
        .setImportant(true)
        .notify(project)
}