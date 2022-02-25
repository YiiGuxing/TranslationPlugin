package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

object TranslationNotifications {

    fun showTranslationErrorNotification(
        project: Project?,
        title: String,
        content: String?,
        throwable: Throwable,
        vararg actions: AnAction
    ) {
        val errorInfo = (throwable as? TranslateException)?.errorInfo
        val errorMessage = errorInfo?.message ?: message("error.unknown")
        val message = content
            ?.takeIf { it.isNotBlank() }
            ?.let { "$it: $errorMessage" }
            ?: message("error.translate.failed", errorMessage)

        val actionList = actions.toMutableList()
        errorInfo?.continueActions?.let { actionList += it }
        // TODO: add switch translator action

        Notifications.showErrorNotification(project, title, message, throwable, *actionList.toTypedArray())
    }

}