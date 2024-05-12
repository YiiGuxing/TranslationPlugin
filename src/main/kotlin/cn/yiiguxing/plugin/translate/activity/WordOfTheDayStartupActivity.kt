package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.util.executeOnPooledThread
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.wordbook.WordBookService
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project


/**
 * Word of the day startup activity.
 */
class WordOfTheDayStartupActivity : BaseStartupActivity(true), DumbAware {

    override fun onBeforeRunActivity(project: Project): Boolean = Settings.getInstance().showWordsOnStartup

    override fun onRunActivity(project: Project) {
        executeOnPooledThread {
            if (project.isDisposed) {
                return@executeOnPooledThread
            }
            WordBookService.getInstance()
                .takeIf { it.isInitialized }
                ?.getWords()
                ?.takeIf { it.isNotEmpty() }
                ?.shuffled()
                ?.let { words ->
                    invokeLater {
                        if (!project.isDisposed) {
                            TranslationUIManager.showWordOfTheDayDialog(project, words)
                        }
                    }
                }
        }
    }

}