package cn.yiiguxing.plugin.translate.diagnostic

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

internal object ErrorReportNotifications {

    fun showNotification(
        project: Project?,
        title: String,
        message: String,
        notificationType: NotificationType = NotificationType.INFORMATION
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Error Report")
            .createNotification(title, message, notificationType)
            .setListener(NotificationListener.URL_OPENING_LISTENER)
            .setImportant(false)
            .notify(project)
    }

}