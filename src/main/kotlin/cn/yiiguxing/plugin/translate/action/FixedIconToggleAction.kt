package cn.yiiguxing.plugin.translate.action

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.util.NlsActions.ActionDescription
import com.intellij.openapi.util.NlsActions.ActionText
import java.util.function.Supplier
import javax.swing.Icon

abstract class FixedIconToggleAction : ToggleAction {

    protected val icon: Icon

    protected constructor(
        icon: Icon,
        @ActionText text: String?,
        @ActionDescription description: String? = null
    ) : super(text, description, icon) {
        this.icon = icon
    }

    protected constructor(
        icon: Icon,
        text: Supplier<@ActionText String?>,
        description: Supplier<@ActionDescription String?> = Presentation.NULL_STRING
    ) : super(text, description, icon) {
        this.icon = icon
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        val presentation = e.presentation
        val selected = Toggleable.isSelected(presentation)
        presentation.icon = getIcon(e.place, selected)
    }

    protected open fun getIcon(place: String, selected: Boolean): Icon? {
        return if (ActionPlaces.isPopupPlace(place) && selected) null else icon
    }

}