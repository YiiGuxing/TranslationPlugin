package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.update.UpdateManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction


class WhatsNewAction : DumbAwareAction() {
    init {
        templatePresentation.text = message("action.WhatsNewInTranslationAction.text")
    }

    override fun actionPerformed(e: AnActionEvent) {
        UpdateManager.browseWhatsNew(e.project)
    }
}