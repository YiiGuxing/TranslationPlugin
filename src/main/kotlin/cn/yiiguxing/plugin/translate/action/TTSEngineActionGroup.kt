package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PopupAction

class TTSEngineActionGroup : DefaultActionGroup(
    { message("action.TTSEngineActionGroup.name") },
    true
), PopupAction {

    init {
        val actions = TTSEngineAction.actions()
        actions.filter { it.isAvailable() }
            .forEach { add(it) }
        val unavailableActions = actions.filter { !it.isAvailable() }
        if (unavailableActions.isNotEmpty()) {
            addSeparator(message("action.separator.inactivated"))
            unavailableActions.forEach { add(it) }
        }
    }

    override fun isDumbAware(): Boolean = true

}