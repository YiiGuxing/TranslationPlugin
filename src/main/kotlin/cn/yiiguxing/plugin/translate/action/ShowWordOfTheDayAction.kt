@file:Suppress("InvalidBundleOrProperty")

package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.MessageType

/**
 * Show word of the day action.
 *
 * Created by Yii.Guxing on 2019/08/23.
 */
class ShowWordOfTheDayAction : AnAction(), DumbAware {

    private var isLoading: Boolean = false

    init {
        templatePresentation.description = message("word.of.the.day.title")
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (Application.isUnitTestMode || isLoading) {
            return
        }

        isLoading = true
        val project = e.project
        executeOnPooledThread {
            if (project?.isDisposed == true) {
                return@executeOnPooledThread
            }

            val words = WordBookService.getWords()
            invokeLater {
                if (project?.isDisposed != true) {
                    if (words.isNotEmpty()) {
                        TranslationUIManager.showWordDialog(project, words)
                    } else {
                        val message = message("word.of.the.day.no.words")
                        Popups.showBalloonForActiveFrame(message, MessageType.INFO)
                    }
                }
                isLoading = false
            }
        }
    }
}