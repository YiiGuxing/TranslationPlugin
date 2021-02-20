package cn.yiiguxing.plugin.translate.action

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * TranslatorActionGroup
 */
class TranslatorActionGroup : ActionGroup() {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> =
        TranslatorAction.availableActions().toTypedArray()
}