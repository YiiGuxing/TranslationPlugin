package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.WebPages
import cn.yiiguxing.plugin.translate.message
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.Icon

class GettingStartedAction(icon: Icon? = null) :
    DumbAwareAction({ message("action.GettingStartedAction.text") }, Presentation.NULL_STRING, icon) {
    override fun actionPerformed(e: AnActionEvent) {
        WebPages.browse(
            e.project,
            WebPages.docs(),
            message("help.getting.started", TranslationPlugin.name)
        )
    }
}