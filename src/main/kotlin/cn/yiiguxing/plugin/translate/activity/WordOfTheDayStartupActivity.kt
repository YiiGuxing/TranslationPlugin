package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.ui.WordDialog
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.Settings
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.ToolWindowManager


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
        run(project, 3)
    }


    companion object {
        private fun run(project: Project, delayCount: Int) {
            if (project.isDisposed || !Settings.showWordsOnStartup) {
                return
            }

            if (delayCount > 0) {
                ToolWindowManager.getInstance(project).invokeLater { run(project, delayCount - 1) }
            } else {
                WordDialog(project).show()
            }
        }
    }
}