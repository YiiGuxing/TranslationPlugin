package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.update.Version
import cn.yiiguxing.plugin.translate.update.WhatsNew
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction


class WhatsNewAction :
    DumbAwareAction({ message("action.WhatsNewAction.text", TranslationPlugin.name) }) {
    override fun actionPerformed(e: AnActionEvent) {
        WhatsNew.browse(e.project, Version.current())
    }
}