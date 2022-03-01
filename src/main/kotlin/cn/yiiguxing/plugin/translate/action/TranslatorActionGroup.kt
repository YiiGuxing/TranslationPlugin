package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator

/**
 * TranslatorActionGroup
 */
class TranslatorActionGroup : DefaultActionGroup() {

    init {
        addAll(translatorGroupActions())
    }

    companion object {
        private fun translatorGroupActions(): List<AnAction> {
            val availableActions = TranslatorAction.availableActions()
            val unavailableActions = TranslatorAction.unavailableActions()

            val actions = ArrayList<AnAction>(availableActions.size + unavailableActions.size + 3)
            actions.addAll(availableActions)
            if (unavailableActions.isNotEmpty()) {
                actions.add(Separator.create(message("action.TranslatorActionGroup.separator.inactivated")))
                actions.addAll(unavailableActions)
                actions.add(Separator.create())
                actions.add(SettingsAction(message("action.TranslatorActionGroup.go.to.activate"), null))
            }

            return actions
        }
    }

}