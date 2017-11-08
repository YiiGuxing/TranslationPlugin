package cn.yiiguxing.plugin.translate.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import javax.swing.JComponent

/**
 * 显示翻译对话框动作
 *
 * Created by Yii.Guxing on 2017/9/11
 */
class ShowTranslationDialogAction : AnAction(), DumbAware {

    init {
        isEnabledInModalContext = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment)
            return

        //TranslationManager.instance.showDialog(e.project)

        with(MyDialog(e.project)) {
            setSize(400, 500)
            show()
        }
    }
}

class MyDialog(p: Project?) : DialogWrapper(p) {


    init {
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
        }
    }

}