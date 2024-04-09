package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.intellij.compat.action.CompatDumbAwareAction
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import com.intellij.openapi.actionSystem.AnActionEvent

class PinBalloonAction : CompatDumbAwareAction(), ImportantTranslationAction {
    init {
        templatePresentation.text = message("action.PinBalloonAction.text")
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = TranslationUIManager.instance(e.project).currentBalloon() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        TranslationUIManager.instance(e.project).currentBalloon()?.pin()
    }
}