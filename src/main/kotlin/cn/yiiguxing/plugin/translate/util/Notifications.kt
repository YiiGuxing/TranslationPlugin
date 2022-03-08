@file:Suppress("MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.ErrorsDialog
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import icons.Icons
import javax.swing.event.HyperlinkEvent

object Notifications {

    const val DEFAULT_NOTIFICATION_GROUP_ID = "Translation Plugin"

    fun showErrorNotification(
        project: Project?,
        title: String,
        content: String,
        throwable: Throwable,
        vararg actions: AnAction
    ) {
        showErrorNotification(DEFAULT_NOTIFICATION_GROUP_ID, project, title, content, throwable, actions = actions)
    }

    fun showErrorNotification(
        groupId: String,
        project: Project?,
        title: String,
        content: String,
        throwable: Throwable,
        vararg actions: AnAction
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(groupId)
            .createNotification(content, NotificationType.ERROR)
            .setTitle(title)
            .setListener(UrlOpeningListener)
            // actions的折叠是从左往右折叠的
            .addActions(actions.toList() as Collection<AnAction>)
            .addAction(ErrorDetailsAction(content, throwable))
            .show(project)
    }


    private class ErrorDetailsAction(
        val message: String,
        val throwable: Throwable
    ) : DumbAwareAction(message("error.see.details.and.submit.report"), null, Icons.RecordErrorInfo) {

        override fun actionPerformed(e: AnActionEvent) {
            ErrorsDialog.show(e.project, message, throwable)
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
            .createNotification(message, type)
            .setTitle(title)
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