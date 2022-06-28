package cn.yiiguxing.plugin.translate.action

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.NlsActions
import javax.swing.Icon

class BrowseUrlAction(
    @NlsActions.ActionText text: String,
    private val url: String,
    @NlsActions.ActionDescription description: String? = null,
    icon: Icon? = AllIcons.General.Web
) : AnAction(text, description, icon), DumbAware {

    override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(url)

}