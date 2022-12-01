@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.compat.notification.SingletonNotificationManagerCompat
import cn.yiiguxing.plugin.translate.message
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.notification.impl.NotificationFullContent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.util.function.Consumer
import javax.swing.event.HyperlinkEvent

interface SingletonNotificationManager {
    fun notify(title: String, content: String, project: Project?, customizer: Consumer<Notification>)

    fun notify(title: String, content: String, project: Project?) = notify(title, content, project) { }

    fun clear()
}


object Notifications {

    const val DEFAULT_NOTIFICATION_GROUP_ID = "Translation Plugin"

    private const val DO_NOT_SHOW_AGAIN_KEY_PREFIX = "translation.notification.do.not.show.again"

    private val DEFAULT_NOTIFICATION_CUSTOMIZER: (Notification) -> Unit = { }

    fun createSingletonNotificationManager(
        groupId: String = DEFAULT_NOTIFICATION_GROUP_ID,
        type: NotificationType
    ): SingletonNotificationManager {
        return SingletonNotificationManagerCompat.createManager(groupId, type)
    }

    fun showNotification(
        title: String,
        message: String,
        type: NotificationType,
        project: Project? = null,
        groupId: String = DEFAULT_NOTIFICATION_GROUP_ID,
        notificationCustomizer: (Notification) -> Unit = DEFAULT_NOTIFICATION_CUSTOMIZER
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(groupId)
            .createNotification(message, type)
            .setTitle(title)
            .apply { notificationCustomizer(this) }
            .show(project)
    }

    fun showFullContentNotification(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFORMATION,
        project: Project? = null,
        groupId: String = DEFAULT_NOTIFICATION_GROUP_ID,
        notificationCustomizer: (Notification) -> Unit = DEFAULT_NOTIFICATION_CUSTOMIZER
    ) {
        val group = NotificationGroupManager.getInstance().getNotificationGroup(groupId)
        val notification = FullContentNotification(group.displayId, title, message, type)
        notificationCustomizer(notification)
        notification.show(project)
    }

    fun showInfoNotification(
        title: String,
        message: String,
        project: Project? = null,
        groupId: String = DEFAULT_NOTIFICATION_GROUP_ID,
        notificationCustomizer: (Notification) -> Unit = DEFAULT_NOTIFICATION_CUSTOMIZER
    ) {
        showNotification(title, message, NotificationType.INFORMATION, project, groupId, notificationCustomizer)
    }

    fun showWarningNotification(
        title: String,
        message: String,
        project: Project? = null,
        groupId: String = DEFAULT_NOTIFICATION_GROUP_ID,
        notificationCustomizer: (Notification) -> Unit = DEFAULT_NOTIFICATION_CUSTOMIZER
    ) {
        showNotification(title, message, NotificationType.WARNING, project, groupId, notificationCustomizer)
    }

    fun showErrorNotification(
        title: String,
        message: String,
        project: Project? = null,
        groupId: String = DEFAULT_NOTIFICATION_GROUP_ID,
        notificationCustomizer: (Notification) -> Unit = DEFAULT_NOTIFICATION_CUSTOMIZER
    ) {
        showNotification(title, message, NotificationType.ERROR, project, groupId = groupId, notificationCustomizer)
    }

    fun isDoNotShowAgain(key: String): Boolean {
        return PropertiesComponent.getInstance().getBoolean("$DO_NOT_SHOW_AGAIN_KEY_PREFIX.$key", false)
    }

    fun setDoNotShowAgain(key: String, value: Boolean) {
        PropertiesComponent.getInstance().setValue("$DO_NOT_SHOW_AGAIN_KEY_PREFIX.$key", value)
    }


    private class FullContentNotification(displayId: String, title: String, content: String, type: NotificationType) :
        Notification(displayId, title, content, type), NotificationFullContent

    open class UrlOpeningListener(expireNotification: Boolean = true) :
        NotificationListener.UrlOpeningListener(expireNotification) {

        override fun hyperlinkActivated(notification: Notification, hyperlinkEvent: HyperlinkEvent) {
            if (!Hyperlinks.handleDefaultHyperlinkActivated(hyperlinkEvent)) {
                super.hyperlinkActivated(notification, hyperlinkEvent)
            }
        }
    }

    class BrowseUrlAction(
        text: String?,
        private val url: String,
        private val expireNotification: Boolean = true
    ) : NotificationAction(text) {

        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            if (expireNotification) {
                notification.expire()
            }
            BrowserUtil.browse(url)
        }
    }

    class DoNotShowAgainAction(private val key: String) :
        NotificationAction(message("notification.do.not.show.again")) {

        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            notification.expire()
            setDoNotShowAgain(key, true)
        }
    }
}