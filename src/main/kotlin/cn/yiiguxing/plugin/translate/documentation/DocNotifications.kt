package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.TranslationNotifications
import cn.yiiguxing.plugin.translate.util.Notifications
import com.intellij.openapi.project.Project

object DocNotifications {

    fun showTranslationTimeoutWarning(project: Project?) {
        Notifications.showWarningNotification("", message("doc.message.translation.timeout.please.try.again"), project)
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