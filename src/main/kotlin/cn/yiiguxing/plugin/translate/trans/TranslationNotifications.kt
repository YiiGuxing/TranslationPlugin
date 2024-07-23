package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.action.TranslationEngineActionGroup
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.concurrent.asyncLatch
import cn.yiiguxing.plugin.translate.util.concurrent.finishOnUiThread
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import cn.yiiguxing.plugin.translate.util.e
import com.intellij.ide.DataManager
import com.intellij.notification.Notification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.PopupAction
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.runAsync
import java.util.*

object TranslationNotifications {

    private val logger = Logger.getInstance(TranslationNotifications::class.java)

    fun showTranslationErrorNotification(
        project: Project?,
        title: String,
        content: String?,
        throwable: Throwable,
        groupId: String = Notifications.DEFAULT_NOTIFICATION_GROUP_ID,
        vararg actions: AnAction,
        notificationCustomizer: (Notification) -> Unit = {}
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
        actionList.add(SwitchTranslationEngineAction())

        if (throwable !is TranslateException) {
            // 将异常写入IDE异常池，以便用户反馈
            logger.e("Translation error: ${throwable.message}", throwable)
        }
        Notifications.showErrorNotification(title, message, project, groupId) {
            it.addActions(actionList as Collection<AnAction>)
            notificationCustomizer(it)
        }
    }

    private class SwitchTranslationEngineAction : AnAction({ message("action.SwitchEngineAction.text") }),
        PopupAction {

        private var isActionPerforming = false

        override fun actionPerformed(e: AnActionEvent) {
            val component = PlatformDataKeys.CONTEXT_COMPONENT.getData(e.dataContext) ?: return
            if (isActionPerforming) {
                return
            }
            isActionPerforming = true
            asyncLatch { latch ->
                runAsync {
                    latch.await()
                    TranslationEngineActionGroup()
                }.successOnUiThread { group ->
                    if (component.isShowing) {
                        // Do not use `e.dataContext` directly because it is not an async data context.
                        val dataContext = DataManager.getInstance().getDataContext(component)
                        group.showActionPopup(dataContext)
                    }
                }.finishOnUiThread(ModalityState.any()) {
                    isActionPerforming = false
                }
            }
        }
    }
}