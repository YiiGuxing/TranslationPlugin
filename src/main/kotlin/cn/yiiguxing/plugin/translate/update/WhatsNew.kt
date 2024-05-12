package cn.yiiguxing.plugin.translate.update

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.WebPages
import cn.yiiguxing.plugin.translate.message
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project

internal object WhatsNew {

    fun browse(project: Project?, version: Version) {
        val v = version.getFeatureUpdateVersion()
        WebPages.browse(
            project,
            WebPages.releaseNote(v),
            message("help.whats.new", TranslationPlugin.name),
        )
    }

    class Action(private val version: Version) : DumbAwareAction(
        message("help.whats.new", version.getFeatureUpdateVersion()),
        null,
        AllIcons.General.Web
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            browse(e.project, version)
        }
    }
}