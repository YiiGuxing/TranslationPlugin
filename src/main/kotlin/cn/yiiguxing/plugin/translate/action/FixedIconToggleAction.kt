package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.intellij.compat.action.UpdateInBackgroundCompatToggleAction
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.NlsActions.ActionText
import java.util.function.Supplier
import javax.swing.Icon

abstract class FixedIconToggleAction(
    protected val icon: Icon,
    text: Supplier<@ActionText String>,
    description: Supplier<@NlsActions.ActionDescription String> = Presentation.NULL_STRING,
) : UpdateInBackgroundCompatToggleAction(text, description, null) {

    override fun update(e: AnActionEvent) {
        val selected = isSelected(e)
        val presentation = e.presentation
        Toggleable.setSelected(presentation, selected)
        presentation.icon = getIcon(e.place, selected)
    }

    protected open fun getIcon(place: String, selected: Boolean): Icon? {
        return if (ActionPlaces.isPopupPlace(place) && selected) null else icon
    }

}