package cn.yiiguxing.plugin.translate.action

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * TranslatorActionGroup
 */
class TranslatorActionGroup : ActionGroup() {

    private val translatorActions: Array<AnAction> = TranslatorAction.ACTIONS.toTypedArray()

    override fun getChildren(e: AnActionEvent?): Array<AnAction> = translatorActions

}