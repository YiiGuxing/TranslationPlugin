@file:Suppress("MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.TranslateException
import cn.yiiguxing.plugin.translate.ui.DefaultHyperlinkHandler
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.event.HyperlinkEvent

typealias ErrorMessageProvider = (Throwable) -> String?

object Notifications {

    const val DEFAULT_NOTIFICATION_GROUP_ID = "Translation Plugin"

    val DEFAULT_ERROR_MESSAGE_PROVIDER: ErrorMessageProvider = { null }

    val TRANSLATION_ERROR_MESSAGE_PROVIDER: ErrorMessageProvider = { throwable ->
        (throwable as? TranslateException)
            ?.errorInfo
            ?.message
            ?.takeIf { it.isNotBlank() }
            ?: message("error.unknown")
    }

    fun showErrorNotification(
        project: Project?,
        title: String,
        content: String,
        throwable: Throwable,
        vararg actions: AnAction,
        errorMessage: ErrorMessageProvider = DEFAULT_ERROR_MESSAGE_PROVIDER
    ) {
        showErrorNotification(
            DEFAULT_NOTIFICATION_GROUP_ID,
            project,
            title,
            content,
            throwable,
            actions = actions,
            errorMessage = errorMessage
        )
    }

    fun showErrorNotification(
        groupId: String,
        project: Project?,
        title: String,
        content: String,
        throwable: Throwable,
        vararg actions: AnAction,
        errorMessage: ErrorMessageProvider = DEFAULT_ERROR_MESSAGE_PROVIDER
    ) {
        val errorInfo = (throwable as? TranslateException)?.errorInfo
        var message = content
        errorMessage(throwable)?.let { message += ": $it" }

        NotificationGroupManager.getInstance()
            .getNotificationGroup(groupId)
            .createNotification(message, NotificationType.ERROR)
            .setTitle(title)
            .setListener(UrlOpeningListener)
            .addActions(actions.toList() as Collection<AnAction>)
            .addActions((errorInfo?.continueActions ?: emptyList()) as Collection<AnAction>)
            .addAction(CopyToClipboardAction(message, throwable))
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
            if (!DefaultHyperlinkHandler.handleHyperlinkActivated(hyperlinkEvent)) {
                super.hyperlinkActivated(notification, hyperlinkEvent)
            }
        }

        companion object : UrlOpeningListener()
    }

}