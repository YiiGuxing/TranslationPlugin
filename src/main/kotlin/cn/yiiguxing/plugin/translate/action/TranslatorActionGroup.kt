package cn.yiiguxing.plugin.translate.action

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * TranslatorActionGroup
 *
 * Created by Yii.Guxing on 2018/01/19
 */
class TranslatorActionGroup : ActionGroup() {

    private val translatorActions: Array<AnAction> = TranslatorAction.ACTIONS.toTypedArray()

    override fun getChildren(e: AnActionEvent?): Array<AnAction> = translatorActions

}