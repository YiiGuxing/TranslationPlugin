package cn.yiiguxing.plugin.translate.diagnostic

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

internal object ErrorReportNotifications {

    fun showNotification(
        project: Project?,
        title: String,
        message: String,
        vararg actions: AnAction,
        notificationType: NotificationType = NotificationType.INFORMATION
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Error Report")
            .createNotification(title, message, notificationType)
            .addActions(actions.toList() as Collection<AnAction>)
            .setImportant(false)
            .notify(project)
    }

}