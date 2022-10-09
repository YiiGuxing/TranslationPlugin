package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.settings.TranslationConfigurable
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.NlsActions.ActionText
import javax.swing.Icon

class SettingsAction(
    @ActionText text: String? = message("settings.title.translate"),
    @NlsActions.ActionDescription description: String? = message("settings.title.translate"),
    icon: Icon? = AllIcons.General.GearPlain
) : AnAction(text, description, icon), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        TranslationConfigurable.showSettingsDialog(e.project)
    }

}