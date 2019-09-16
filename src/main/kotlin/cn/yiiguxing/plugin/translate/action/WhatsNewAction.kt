package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.update.UpdateManager
import cn.yiiguxing.plugin.translate.util.Application
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class WhatsNewAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        if (Application.isUnitTestMode) {
            return
        }

        e.project?.let { UpdateManager.showUpdateToolWindow(it) }
    }
}