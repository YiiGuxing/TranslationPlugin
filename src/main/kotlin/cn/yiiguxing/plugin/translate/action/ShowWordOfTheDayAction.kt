package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.WordBookService
import cn.yiiguxing.plugin.translate.util.executeOnPooledThread
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.wordbook.WordBookToolWindowFactory
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindowManager

/**
 * Show word of the day action.
 */
class ShowWordOfTheDayAction : AnAction(), DumbAware {

    init {
        templatePresentation.description = message("word.of.the.day.title")
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (Application.isUnitTestMode) {
            return
        }

        val project = e.project ?: return
        if (!WordBookService.isInitialized) {
            showNotificationByBalloon(project, message("wordbook.window.message.missing.driver"))
            return
        }

        executeOnPooledThread {
            if (project.isDisposed) {
                return@executeOnPooledThread
            }

            val words = WordBookService.getWords().sortedBy { Math.random() }
            invokeLater {
                if (!project.isDisposed) {
                    if (words.isNotEmpty()) {
                        TranslationUIManager.showWordOfTheDayDialog(project, words)
                    } else {
                        showNotificationByBalloon(project, message("word.of.the.day.no.words"))
                    }
                }
            }
        }
    }

    private fun showNotificationByBalloon(project: Project, message: String) {
        ToolWindowManager
            .getInstance(project)
            .notifyByBalloon(
                WordBookToolWindowFactory.TOOL_WINDOW_ID,
                MessageType.INFO,
                message,
                AllIcons.General.Information,
                null
            )
    }

}