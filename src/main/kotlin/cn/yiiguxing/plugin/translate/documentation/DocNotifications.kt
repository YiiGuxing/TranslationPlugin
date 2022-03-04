package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.TranslationNotifications
import com.intellij.openapi.project.Project

object DocNotifications {

    fun showWarning(e: Throwable, project: Project?) {
        TranslationNotifications.showTranslationErrorNotification(
            project,
            message("translate.documentation.notification.title"),
            message("translate.documentation.error"),
            e
        )
    }

}