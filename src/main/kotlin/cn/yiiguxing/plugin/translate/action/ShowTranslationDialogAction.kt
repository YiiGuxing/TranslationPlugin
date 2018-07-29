package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.ui.icon.Icons
import cn.yiiguxing.plugin.translate.util.TranslationUIManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware

/**
 * 显示翻译对话框动作
 *
 * Created by Yii.Guxing on 2017/9/11
 */
class ShowTranslationDialogAction : AnAction(Icons.Translate), DumbAware {

    init {
        isEnabledInModalContext = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (!ApplicationManager.getApplication().isHeadlessEnvironment) {
            TranslationUIManager.showDialog(e.project)
        }
    }
}