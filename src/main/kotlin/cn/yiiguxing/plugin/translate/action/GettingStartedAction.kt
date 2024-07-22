package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.WebPages
import cn.yiiguxing.plugin.translate.message
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import javax.swing.Icon

open class GettingStartedAction(icon: Icon? = null) :
    DumbAwareAction({ message("action.GettingStartedAction.text") }, Presentation.NULL_STRING, icon) {
    override fun actionPerformed(e: AnActionEvent) {
        browse(e.project)
    }

    companion object {
        fun browse(project: Project? = null) {
            WebPages.browse(
                project,
                WebPages.docs(),
                message("help.getting.started", TranslationPlugin.name)
            )
        }
    }
}