package cn.yiiguxing.plugin.translate.compat.notification

import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.util.SingletonNotificationManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer


object SingletonNotificationManagerCompat {

    fun createManager(groupId: String, type: NotificationType): SingletonNotificationManager {
        return if (IdeVersion > IdeVersion.IDE2022_1) {
            throw UnsupportedOperationException("Unsupported IDE version: ${IdeVersion.buildNumber}")
        } else {
            SingletonNotificationManagerCompat221(groupId, type)
        }
    }

}

private class SingletonNotificationManagerCompat221(groupId: String, private val type: NotificationType) :
    SingletonNotificationManager {
    private val group = NotificationGroupManager.getInstance().getNotificationGroup(groupId)
    private val notification = AtomicReference<Notification>()

    override fun notify(title: String, content: String, project: Project?) =
        notify(title, content, project) { }

    override fun notify(
        title: String,
        content: String,
        project: Project?,
        customizer: Consumer<Notification>
    ) {
        val oldNotification = notification.get()
        if (oldNotification != null) {
            if (isVisible(oldNotification, project)) {
                return
            }
            oldNotification.expire()
        }

        val newNotification = object : Notification(group.displayId, title, content, type) {
            override fun expire() {
                super.expire()
                notification.compareAndSet(this, null)
            }
        }
        customizer.accept(newNotification)

        if (notification.compareAndSet(oldNotification, newNotification)) {
            newNotification.notify(project)
        } else {
            newNotification.expire()
        }
    }

    private fun isVisible(notification: Notification, project: Project?): Boolean {
        val balloon = when {
            group.displayType != NotificationDisplayType.TOOL_WINDOW -> notification.balloon
            project != null -> ToolWindowManager.getInstance(project).getToolWindowBalloon(group.toolWindowId!!)
            else -> null
        }
        return balloon != null && !balloon.isDisposed
    }

    override fun clear() {
        notification.getAndSet(null)?.expire()
    }
}