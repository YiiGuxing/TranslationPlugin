package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity


/**
 * Word of the day startup activity.
 *
 * Created by Yii.Guxing on 2019/08/22.
 */
class WordOfTheDayStartupActivity : StartupActivity, DumbAware {

    private var veryFirstProjectOpening: Boolean = true

    override fun runActivity(project: Project) {
        if (Application.isUnitTestMode || !veryFirstProjectOpening || !Settings.showWordsOnStartup) {
            return
        }

        veryFirstProjectOpening = true
        Activity.runLater(project, 3) {
            executeOnPooledThread {
                if (project.isDisposed) {
                    return@executeOnPooledThread
                }
                WordBookService
                    .takeIf { it.isInitialized }
                    ?.getWords()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { words ->
                        val sortedWords = words.sortedBy { Math.random() }
                        invokeLater {
                            if (!project.isDisposed) {
                                TranslationUIManager.showWordOfTheDayDialog(project, sortedWords)
                            }
                        }
                    }
            }
        }
    }
}