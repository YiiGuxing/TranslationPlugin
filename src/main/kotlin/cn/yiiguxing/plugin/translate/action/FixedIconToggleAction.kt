package cn.yiiguxing.plugin.translate.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.NlsActions.ActionText
import java.util.function.Supplier
import javax.swing.Icon

abstract class FixedIconToggleAction(
    private val icon: Icon,
    text: Supplier<@ActionText String?>,
    description: Supplier<@NlsActions.ActionDescription String?> = Presentation.NULL_STRING,
) : ToggleAction(text, description, null) {

    override fun update(e: AnActionEvent) {
        val selected = isSelected(e)
        val presentation = e.presentation
        Toggleable.setSelected(presentation, selected)
        presentation.icon = if (e.isFromContextMenu && selected) null else icon
    }

}