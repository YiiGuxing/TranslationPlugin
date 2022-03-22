@file:Suppress("MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import javax.swing.event.HyperlinkEvent

object Notifications {

    const val DEFAULT_NOTIFICATION_GROUP_ID = "Translation Plugin"

    fun showErrorNotification(
        project: Project?,
        title: String,
        content: String,
        vararg actions: AnAction
    ) {
        showErrorNotification(DEFAULT_NOTIFICATION_GROUP_ID, project, title, content, actions = actions)
    }

    fun showErrorNotification(
        groupId: String,
        project: Project?,
        title: String,
        content: String,
        vararg actions: AnAction
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(groupId)
            .createNotification(content, NotificationType.ERROR)
            .setTitle(title)
            .setListener(UrlOpeningListener)
            // actions的折叠是从左往右折叠的
            .apply { addActions(actions.toList()) }
            .show(project)
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
            .createNotification(message, type)
            .setTitle(title)
            .setListener(UrlOpeningListener)
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

    open class UrlOpeningListener(expireNotification: Boolean = true) :
        NotificationListener.UrlOpeningListener(expireNotification) {

        override fun hyperlinkActivated(notification: Notification, hyperlinkEvent: HyperlinkEvent) {
            if (!Hyperlinks.handleDefaultHyperlinkActivated(hyperlinkEvent)) {
                super.hyperlinkActivated(notification, hyperlinkEvent)
            }
        }

        companion object : UrlOpeningListener()
    }

}