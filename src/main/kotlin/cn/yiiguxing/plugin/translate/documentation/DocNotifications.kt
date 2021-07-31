package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.Settings
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

object DocNotifications {

    fun showWarning(e: Throwable, project: Project?) {
        Notifications.showErrorNotification(
            project,
            message("translate.documentation.notification.title"),
            message("translate.documentation.error", e.message ?: ""),
            e,
            DisableAutoDocTranslationAction()
        )
    }

    private class DisableAutoDocTranslationAction : NotificationAction(message("translate.documentation.disable")) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            Settings.translateDocumentation = false
            notification.expire()
        }
    }
}