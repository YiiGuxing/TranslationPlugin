package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.TranslationNotifications
import cn.yiiguxing.plugin.translate.util.Notifications
import com.intellij.notification.Notification
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicReference

object DocNotifications {

    private const val GROUP_ID = "Documentation translation failed"

    private val notificationRef = AtomicReference<Notification>()

    fun showTranslationTimeoutWarning(project: Project?) {
        Notifications.showWarningNotification(
            title = "",
            message = message("doc.message.translation.timeout.please.try.again"),
            project = project,
            groupId = GROUP_ID,

            ) { register(it) }
    }

    fun showError(e: Throwable, project: Project?) {
        TranslationNotifications.showTranslationErrorNotification(
            project = project,
            title = message("translate.documentation.notification.title"),
            content = message("translate.documentation.error"),
            throwable = e,
            groupId = GROUP_ID,
        ) { register(it) }
    }

    @Synchronized
    private fun register(notification: Notification) {
        with(notificationRef) {
            get()?.expire()
            set(notification)
            notification.whenExpired { compareAndSet(notification, null) }
        }
    }

}