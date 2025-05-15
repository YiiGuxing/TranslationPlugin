package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PopupAction
import com.intellij.openapi.actionSystem.Presentation
import icons.TranslationIcons

class TTSEngineActionGroup : DefaultActionGroup(
    { message("action.TTSEngineActionGroup.name") },
    Presentation.NULL_STRING,
    TranslationIcons.Speech
), PopupAction {

    init {
        isPopup = true

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