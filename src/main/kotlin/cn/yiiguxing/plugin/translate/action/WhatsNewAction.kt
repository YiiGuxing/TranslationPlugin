package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.update.Version
import cn.yiiguxing.plugin.translate.update.WhatsNew
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction


class WhatsNewAction : DumbAwareAction() {
    init {
        templatePresentation.text = adaptedMessage("action.WhatsNewInTranslationAction.text", "Translation")
    }

    override fun actionPerformed(e: AnActionEvent) {
        WhatsNew.browse(Version.current(), e.project)
    }
}