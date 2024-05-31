package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.wordbook.WordBookService
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Word of the day startup activity.
 */
class WordOfTheDayStartupActivity : BaseStartupActivity(true, false) {

    override suspend fun onBeforeRunActivity(project: Project): Boolean = Settings.getInstance().showWordsOnStartup

    override suspend fun onRunActivity(project: Project) {
        val words = withContext(Dispatchers.IO) {
            WordBookService.getInstance()
                .takeIf { it.isInitialized }
                ?.getWords()
                ?.takeIf { it.isNotEmpty() }
                ?.shuffled()
        } ?: return
        withContext(Dispatchers.EDT) {
            TranslationUIManager.showWordOfTheDayDialog(project, words)
        }
    }
}