package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.TranslationNotifications
import cn.yiiguxing.plugin.translate.util.Notifications
import com.intellij.notification.NotificationType
import com.intellij.notification.SingletonNotificationManager
import com.intellij.openapi.project.Project

object DocNotifications {

    private val notificationManager = SingletonNotificationManager(
        Notifications.DEFAULT_NOTIFICATION_GROUP_ID,
        NotificationType.WARNING
    )

    fun showWarning(project: Project?, message: String) {
        notificationManager.notify("", message, project) {}
    }

    fun showError(e: Throwable, project: Project?) {
        TranslationNotifications.showTranslationErrorNotification(
            project,
            message("translate.documentation.notification.title"),
            message("translate.documentation.error"),
            e
        )
    }

}