package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.WordBookService
import cn.yiiguxing.plugin.translate.util.executeOnPooledThread
import cn.yiiguxing.plugin.translate.util.invokeLater
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project


/**
 * Word of the day startup activity.
 */
class WordOfTheDayStartupActivity : BaseStartupActivity(true), DumbAware {

    override fun onBeforeRunActivity(project: Project): Boolean = Settings.showWordsOnStartup

    override fun onRunActivity(project: Project) {
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