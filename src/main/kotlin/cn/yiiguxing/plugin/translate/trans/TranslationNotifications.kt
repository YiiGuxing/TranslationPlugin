package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.action.TranslatorActionGroup
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import java.util.*

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

        // actions的折叠是从左往右折叠的
        val actionList = LinkedList<AnAction>()
        errorInfo?.continueActions?.let { actionList += it }
        actionList.addAll(actions)
        actionList.add(TranslatorActionGroup({ message("action.SwitchTranslatorAction.text") }))

        Notifications.showErrorNotification(project, title, message, throwable, *actionList.toTypedArray())
    }

}