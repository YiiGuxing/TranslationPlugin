package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.ui.icon.Icons
import cn.yiiguxing.plugin.translate.util.TranslationUIManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware

/**
 * ShowInstantTranslationDialogAction
 *
 * Created by Yii.Guxing on 2018/07/08
 */
class ShowInstantTranslationDialogAction : AnAction(Icons.Translate2), DumbAware {

    init {
        isEnabledInModalContext = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (!ApplicationManager.getApplication().isHeadlessEnvironment) {
            TranslationUIManager.showInstantTranslationDialog(e.project)
        }
    }
}