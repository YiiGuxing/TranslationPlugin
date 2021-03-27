@file:Suppress("MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.ui.settings.OptionsConfigurable
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.event.HyperlinkEvent

object Notifications {

    fun showErrorNotification(
        project: Project?,
        title: String,
        message: String,
        throwable: Throwable,
        vararg actions: AnAction
    ) {
        showErrorNotification(DEFAULT_NOTIFICATION_GROUP_ID, project, title, message, throwable, * actions)
    }

    fun showErrorNotification(
        groupId: String,
        project: Project?,
        title: String,
        message: String,
        throwable: Throwable,
        vararg actions: AnAction
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(groupId)
            .createNotification(
                title,
                message,
                NotificationType.ERROR,
                object : NotificationListener.Adapter() {
                    override fun hyperlinkActivated(notification: Notification, event: HyperlinkEvent) {
                        notification.expire()
                        when (event.description) {
                            HTML_DESCRIPTION_SETTINGS -> OptionsConfigurable.showSettingsDialog(project)
                            HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION -> TranslateService.translator.checkConfiguration()
                        }
                    }
                })
            .addAction(CopyToClipboardAction(message, throwable))
            .apply { for (action in actions) addAction(action) }
            .show(project)
    }


    private class CopyToClipboardAction(
        val message: String,
        val throwable: Throwable
    ) : NotificationAction(message("copy.to.clipboard.action.name")) {

        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            throwable.copyToClipboard(message)
            notification.expire()
        }

    }

    fun showNotification(
        title: String,
        message: String,
        type: NotificationType,
        project: Project? = null,
        groupId: String = DEFAULT_NOTIFICATION_GROUP_ID
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(groupId)
            .createNotification(title, message, type, null)
            .show(project)
    }

    fun showInfoNotification(
        title: String,
        message: String,
        project: Project? = null,
        groupId: String = DEFAULT_NOTIFICATION_GROUP_ID
    ) {
        showNotification(title, message, NotificationType.INFORMATION, project, groupId)
    }

    fun showWarningNotification(
        title: String,
        message: String,
        project: Project? = null,
        groupId: String = DEFAULT_NOTIFICATION_GROUP_ID
    ) {
        showNotification(title, message, NotificationType.WARNING, project, groupId)
    }

    fun showErrorNotification(
        title: String,
        message: String,
        project: Project? = null,
        groupId: String = DEFAULT_NOTIFICATION_GROUP_ID
    ) {
        showNotification(title, message, NotificationType.ERROR, project, groupId)
    }

    class UrlOpeningListener(expireNotification: Boolean = true) :
        NotificationListener.UrlOpeningListener(expireNotification) {
        override fun hyperlinkActivated(notification: Notification, hyperlinkEvent: HyperlinkEvent) {
            when (hyperlinkEvent.description) {
                HTML_DESCRIPTION_SETTINGS -> OptionsConfigurable.showSettingsDialog()
                HTML_DESCRIPTION_SUPPORT -> SupportDialog.show()
                HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION -> TranslateService.translator.checkConfiguration()
                else -> super.hyperlinkActivated(notification, hyperlinkEvent)
            }
        }
    }

}